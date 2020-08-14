package com.python.companion.db.interact;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.python.companion.db.Database;
import com.python.companion.db.constant.MeasurementQuery;
import com.python.companion.db.dao.DAOMeasurement;
import com.python.companion.db.entity.Measurement;
import com.python.companion.db.pojo.measurement.MeasurementWithParentNames;
import com.python.companion.ui.jubileum.dialog.JubileumDeleteDialog;
import com.python.companion.util.NotificationUtil;
import com.python.companion.util.genericinterfaces.FinishListener;
import com.python.companion.util.genericinterfaces.ResultListener;

import java.util.List;
import java.util.concurrent.Executors;


public class MeasurementStore {
    /**
     * Insert a given new measurement into the database, either when there is no name-conflict, or the user tells us we may override
     * @param measurement Measurement to store. If its {@code secure} field is set, store the measurement securely
     * @param callback Callback receives a call in either {@link MeasurementStore.StoreCallback#onSuccess()) if we stored the new measurement, or {@link MeasurementStore.StoreCallback#onFailure()} if we did not
     */
    public static void insert(@NonNull Measurement measurement, @NonNull FragmentManager manager, @NonNull Context context, @NonNull MeasurementStore.StoreCallback callback) {
        final MeasurementQuery measurementQuery = new MeasurementQuery(context);
        DAOMeasurement daoMeasurement = Database.getDatabase(context).getDAOMeasurement();
        measurementQuery.isUniqueInstancedNamed(measurement.getNameSingular(), measurement.getNamePlural(), result -> {
            if (result == null) { // Unique-named measurement
                _insert(daoMeasurement, measurement);
                NotificationUtil.buildChannel(measurement, context);
                callback.onSuccess();
            } else { // Another measurement with same name exists
                showDeleteDialog(result, manager, new MeasurementStore.StoreCallback() {
                    @Override
                    public void onSuccess() {
                        _insert(daoMeasurement, measurement);
                        NotificationUtil.buildChannel(measurement, context);
                        callback.onSuccess();
                    }
                    @Override
                    public void onFailure() {
                        callback.onFailure();
                    }
                });
            }
        });
    }

    /**
     * Update given measurement, either when there is no change in names, there is no name conflict, or when the user tells us we may override
     * @param measurement Measurement to update. If its {@code secure} field is set, store the measurement securely
     * @param old Measurement before changes
     * @param callback Callback receives a call in either {@link MeasurementStore.StoreCallback#onSuccess()) if we updated the measurement, or {@link MeasurementStore.StoreCallback#onFailure()} if we did not
     */
    public static void update(@NonNull Measurement measurement, @NonNull Measurement old, @NonNull FragmentManager manager, @NonNull Context context, @NonNull MeasurementStore.StoreCallback callback) {
        final MeasurementQuery measurementQuery = new MeasurementQuery(context);
        if (old.getNameSingular().equals(measurement.getNameSingular()) && old.getNamePlural().equals(measurement.getNamePlural())) {
            updateInternal(Database.getDatabase(context).getDAOMeasurement(), measurement, old, callback);
        } else {
            updateCheckSingular(measurementQuery, Database.getDatabase(context).getDAOMeasurement(), measurement, old, manager, context, callback);
        }
    }

    public static void delete(@NonNull Measurement measurement, @NonNull Context context, @NonNull FinishListener listener) {
        NotificationUtil.deleteChannel(measurement, context);
        _delete(Database.getDatabase(context).getDAOMeasurement(), measurement, listener);
    }

    public static void delete(@NonNull List<Measurement> measurements, @NonNull Context context, @NonNull FinishListener listener) {
        NotificationUtil.deleteChannels2(measurements, context);
        _delete(Database.getDatabase(context).getDAOMeasurement(), measurements, listener);
    }

    private static void updateInternal(@NonNull DAOMeasurement daoMeasurement, @NonNull Measurement measurement, @NonNull Measurement old, @NonNull MeasurementStore.StoreCallback callback) {
        _update(daoMeasurement, measurement, old, success -> {
            if (success)
                callback.onSuccess();
            else
                callback.onFailure();
        });
    }

    private static void updateCheckSingular(@NonNull MeasurementQuery measurementQuery, @NonNull DAOMeasurement daoMeasurement, @NonNull Measurement measurement, @NonNull Measurement old, @NonNull FragmentManager manager, @NonNull Context context, @NonNull MeasurementStore.StoreCallback callback) {
        if (!old.getNameSingular().equals(measurement.getNameSingular())) { // Singular name changed
            measurementQuery.isUniqueInstancedNamed(measurement.getNameSingular(), old.getNamePlural(), result -> {
                if (result == null || result.measurement.getMeasurementID() == measurement.getMeasurementID()) { // New name is unique
                    updateCheckPlural(measurementQuery, daoMeasurement, measurement, old, manager, callback);
                } else { // Another measurement with same name exists
                    showDeleteDialog(result, manager, new StoreCallback() {
                        @Override
                        public void onSuccess() {
                            delete(measurement, context, () -> updateCheckPlural(measurementQuery, daoMeasurement, measurement, old, manager, callback));
                        }
                        @Override
                        public void onFailure() {
                            callback.onFailure();
                        }
                    });
                }
            });
        } else {
            updateCheckPlural(measurementQuery, daoMeasurement, measurement, old, manager, callback);
        }
    }

    private static void updateCheckPlural(@NonNull MeasurementQuery measurementQuery, @NonNull DAOMeasurement daoMeasurement, @NonNull Measurement measurement, @NonNull Measurement old, @NonNull FragmentManager manager, @NonNull MeasurementStore.StoreCallback callback) {
        if (!old.getNamePlural().equals(measurement.getNamePlural())) {// Plural name changed
            measurementQuery.isUniqueInstancedNamed(old.getNameSingular(), measurement.getNamePlural(), result -> {
                if (result == null || result.measurement.getMeasurementID() == measurement.getMeasurementID()) { // New name is unique
                    updateInternal(daoMeasurement, measurement, old, callback);
                } else { // Another measurement with same name exists
                    showDeleteDialog(result, manager, new StoreCallback() {
                        @Override
                        public void onSuccess() {
                            updateInternal(daoMeasurement, measurement, old, callback);
                        }
                        @Override
                        public void onFailure() {
                            callback.onFailure();
                        }
                    });
                }
            });
        } else {
            updateInternal(daoMeasurement, measurement, old, callback);
        }
    }

    private static void showDeleteDialog(@NonNull MeasurementWithParentNames conflicting, @NonNull FragmentManager manager, @NonNull MeasurementStore.StoreCallback callback) {
        JubileumDeleteDialog deleteDialog = new JubileumDeleteDialog.Builder()
                .setExistsText("Measurement name already exists!")
                .setQuestionText("Do you want to delete existing measurement?")
                .setWarningText("Warning: Deleted measurements cannot be restored")
                .setMeasurementWithParentName(conflicting)
                .setDeleteListener(callback::onSuccess)
                .setCancelListener(callback::onFailure)
                .build();
        deleteDialog.show(manager, null);
    }

    /** Callback to receive final state of store operations */
    public interface StoreCallback {
        void onSuccess();
        void onFailure();
    }


    /** Insert a new measurement. Do not call this directly. Instead, use {@link #insert(Measurement, FragmentManager, Context, MeasurementStore.StoreCallback)}*/
    private static void _insert(@NonNull DAOMeasurement daoMeasurement, @NonNull Measurement measurement) {
        Executors.newSingleThreadExecutor().execute(() -> daoMeasurement.insert(measurement));
    }

    /** Deletes a measurement. Do not call this directly. Instead, use {@link #delete(List, Context, FinishListener)} */
    private static void _delete(@NonNull DAOMeasurement daoMeasurement, @NonNull List<Measurement> measurements, @NonNull FinishListener listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            for (Measurement m : measurements)
                daoMeasurement.deleteInherit(m);
            listener.onFinish();
        });
    }

    /** Deletes a measurement. Do not call this directly. Instead, use {@link #delete(Measurement, Context, FinishListener)} */
    private static void _delete(@NonNull DAOMeasurement daoMeasurement, @NonNull Measurement measurement, @NonNull FinishListener listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoMeasurement.deleteInherit(measurement);
            listener.onFinish();
        });
    }

    /** Updates a measurement, handling inheritance. Do not call this directly. Instead, use {@link #update(Measurement, Measurement, FragmentManager, Context, MeasurementStore.StoreCallback)} */
    private static void _update(@NonNull DAOMeasurement daoMeasurement, @NonNull Measurement measurement, @NonNull Measurement old, @NonNull ResultListener<Boolean> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMeasurement.updateInherit(measurement, old)));
    }

}
