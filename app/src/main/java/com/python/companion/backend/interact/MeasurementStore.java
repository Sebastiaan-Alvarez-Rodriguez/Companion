package com.python.companion.backend.interact;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.python.companion.db.constant.MeasurementQuery;
import com.python.companion.db.entity.Measurement;
import com.python.companion.ui.jubileum.dialog.MeasurementDeleteDialog;


public class MeasurementStore {
    /**
     * Insert a given new measurement into the database, either when there is no name-conflict, or the user tells us we may override
     * @param measurement Measurement to store. If its {@code secure} field is set, store the measurement securely
     * @param callback Callback receives a call in either {@link MeasurementStore.StoreCallback#onSuccess()) if we stored the new measurement, or {@link MeasurementStore.StoreCallback#onFailure()} if we did not
     */
    public static void insert(@NonNull Measurement measurement, @NonNull FragmentManager manager, @NonNull Context context, @NonNull MeasurementStore.StoreCallback callback) {
        final MeasurementQuery measurementQuery = new MeasurementQuery(context);
        measurementQuery.isUniqueInstanced(measurement.getNameSingular(), measurement.getNamePlural(), result -> {
            if (result == null) { // Unique-named measurement
                measurementQuery.insert(measurement);
                callback.onSuccess();
            } else { // Another measurement with same name exists
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
//        boolean changeID = (!measurement.getNameSingular().equals(old.getNameSingular()) || !measurement.getNamePlural().equals(old.getNamePlural()));// Force new ID
//        // TODO: Instead of forcing new ID, can also make viewmodel in MeasurementActivity work with a 'MeasurementWithParent' instead of on 'Measurement'

        measurementQuery.update(measurement, old, success -> {
            if (success)
                callback.onSuccess();
            else
                callback.onFailure();
        });
    }

    private static void updateCheckSingular(@NonNull MeasurementQuery measurementQuery, @NonNull Measurement measurement, @NonNull Measurement old, @NonNull FragmentManager manager, @NonNull MeasurementStore.StoreCallback callback) {
        if (!old.getNameSingular().equals(measurement.getNameSingular())) { // Singular name changed
            measurementQuery.isUniqueInstanced(measurement.getNameSingular(), old.getNamePlural(), result -> {
                if (result == null) { // New name is unique
                    updateCheckPlural(measurementQuery, measurement, old, manager, callback);
                } else { // Another measurement with same name exists
                    showDeleteDialog(result, manager, new StoreCallback() {
                        @Override
                        public void onSuccess() {
                            measurementQuery.delete(measurement, v -> updateCheckPlural(measurementQuery, measurement, old, manager, callback));
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
            measurementQuery.isUniqueInstanced(old.getNameSingular(), measurement.getNamePlural(), result -> {
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

    private static void showDeleteDialog(@NonNull Measurement conflicting, @NonNull FragmentManager manager, @NonNull MeasurementStore.StoreCallback callback) {
        MeasurementDeleteDialog measurementOverrideDialog = new MeasurementDeleteDialog.Builder()
                .setExistsText("Measurement name already exists!")
                .setQuestionText("Do you want to override existing measurement?")
                .setWarningText("Warning: Overridden measurements cannot be restored")
                .setMeasurement(conflicting)
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
