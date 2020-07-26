package com.python.companion.db.constant;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOMeasurement;
import com.python.companion.db.entity.Measurement;
import com.python.companion.util.genericinterfaces.ResultListener;

import java.util.List;
import java.util.concurrent.Executors;

public class MeasurementQuery {
    private DAOMeasurement daoMeasurement;


    public MeasurementQuery(Context context) {
        daoMeasurement = Database.getDatabase(context).getDAOMeasurement();
    }

    /** Insert a new measurement */
    public void insert(@NonNull Measurement measurement) {
        Executors.newSingleThreadExecutor().execute(() -> daoMeasurement.insert(measurement));
    }

    public void delete(@NonNull List<Measurement> measurements, @NonNull ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            for (Measurement m : measurements)
                daoMeasurement.deleteInherit(m);
            listener.onResult(null);
        });
    }

    public void delete(@NonNull Measurement measurement, @NonNull ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoMeasurement.deleteInherit(measurement);
            listener.onResult(null);
        });
    }

    public void update(@NonNull Measurement measurement, @NonNull Measurement old, @NonNull ResultListener<Boolean> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMeasurement.updateInherit(measurement, old)));
    }

    public void isUnique(String nameSingular, String namePlural, ResultListener<Boolean> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMeasurement.getBySingularOrPlural(nameSingular, namePlural) == null));
    }

    public void isUniqueInstanced(String nameSingular, String namePlural, ResultListener<Measurement> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMeasurement.getBySingularOrPlural(nameSingular, namePlural)));
    }

    @WorkerThread
    public Measurement findByID(long id) {
        return daoMeasurement.findByID(id);
    }

    @WorkerThread
    public @Nullable Measurement isUniqueInstanced(String nameSingular, String namePlural) {
        return daoMeasurement.getBySingularOrPlural(nameSingular, namePlural);
    }
}
