package com.python.companion.backend.interact;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.python.companion.db.constant.MeasurementQuery;
import com.python.companion.db.entity.Measurement;
import com.python.companion.db.pojo.measurement.MeasurementWithParentNames;
import com.python.companion.ui.jubileum.dialog.MeasurementDeleteDialog;


public class MeasurementStore {
    /**
     * Insert a given new measurement into the database, either when there is no name-conflict, or the user tells us we may override
     * @param measurement Measurement to store. If its {@code secure} field is set, store the measurement securely
     * @param callback Callback receives a call in either {@link MeasurementStore.StoreCallback#onSuccess()) if we stored the new measurement, or {@link MeasurementStore.StoreCallback#onFailure()} if we did not
     */
    public static void insert(@NonNull Measurement measurement, @NonNull FragmentManager manager, @NonNull Context context, @NonNull MeasurementStore.StoreCallback callback) {
        Log.e("MeasurementStore", "Checking uniqueness for "+measurement.getNameSingular()+" (id "+measurement.getMeasurementID()+")");

        final MeasurementQuery measurementQuery = new MeasurementQuery(context);
        measurementQuery.isUniqueInstancedNamed(measurement.getNameSingular(), measurement.getNamePlural(), result -> {
            if (result == null) { // Unique-named measurement
                measurementQuery.insert(measurement);
                callback.onSuccess();
            } else { // Another measurement with same name exists
                Log.e("MeasurementStore", "Measurement is not unique. Existing: "+result.measurement.getNameSingular()+", id "+result.measurement.getMeasurementID());
                showDeleteDialog(result, manager, new MeasurementStore.StoreCallback() {
                    @Override
                    public void onSuccess() {
                        measurementQuery.insert(measurement);
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
            updateInternal(measurementQuery, measurement, old, callback);
        } else {
            updateCheckSingular(measurementQuery, measurement, old, manager, callback);
        }
    }

    private static void updateInternal(@NonNull MeasurementQuery measurementQuery, @NonNull Measurement measurement, @NonNull Measurement old, @NonNull MeasurementStore.StoreCallback callback) {
        measurementQuery.update(measurement, old, success -> {
            if (success)
                callback.onSuccess();
            else
                callback.onFailure();
        });
    }

    private static void updateCheckSingular(@NonNull MeasurementQuery measurementQuery, @NonNull Measurement measurement, @NonNull Measurement old, @NonNull FragmentManager manager, @NonNull MeasurementStore.StoreCallback callback) {
        if (!old.getNameSingular().equals(measurement.getNameSingular())) { // Singular name changed
            measurementQuery.isUniqueInstancedNamed(measurement.getNameSingular(), old.getNamePlural(), result -> {
                if (result == null) { // New name is unique
                    updateCheckPlural(measurementQuery, measurement, old, manager, callback);
                } else { // Another measurement with same name exists
                    showDeleteDialog(result, manager, new StoreCallback() {
                        @Override
                        public void onSuccess() {
                            measurementQuery.delete(measurement, () -> updateCheckPlural(measurementQuery, measurement, old, manager, callback));
                        }
                        @Override
                        public void onFailure() {
                            callback.onFailure();
                        }
                    });
                }
            });
        } else {
            updateCheckPlural(measurementQuery, measurement, old, manager, callback);
        }
    }

    private static void updateCheckPlural(@NonNull MeasurementQuery measurementQuery, @NonNull Measurement measurement, @NonNull Measurement old, @NonNull FragmentManager manager, @NonNull MeasurementStore.StoreCallback callback) {
        if (!old.getNamePlural().equals(measurement.getNamePlural())) {// Plural name changed
            measurementQuery.isUniqueInstancedNamed(old.getNameSingular(), measurement.getNamePlural(), result -> {
                if (result != null) { // Another measurement with same name exists
                    showDeleteDialog(result, manager, new StoreCallback() {
                        @Override
                        public void onSuccess() {
                            updateInternal(measurementQuery, measurement, old, callback);
                        }
                        @Override
                        public void onFailure() {
                            callback.onFailure();
                        }
                    });
                } else {
                    updateInternal(measurementQuery, measurement, old, callback);
                }
            });
        } else {
            updateInternal(measurementQuery, measurement, old, callback);
        }
    }

    private static void showDeleteDialog(@NonNull MeasurementWithParentNames conflicting, @NonNull FragmentManager manager, @NonNull MeasurementStore.StoreCallback callback) {
        MeasurementDeleteDialog measurementOverrideDialog = new MeasurementDeleteDialog.Builder()
                .setExistsText("Measurement name already exists!")
                .setQuestionText("Do you want to delete existing measurement?")
                .setWarningText("Warning: Deleted measurements cannot be restored")
                .setMeasurementWithParentName(conflicting)
                .setDeleteListener(callback::onSuccess)
                .setCancelListener(callback::onFailure)
                .build();
        measurementOverrideDialog.show(manager, null);
    }

    /** Callback to receive final state of store operations */
    public interface StoreCallback {
        void onSuccess();
        void onFailure();
    }
}
