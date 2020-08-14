package com.python.companion.db.constant;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOMeasurement;
import com.python.companion.db.entity.Measurement;
import com.python.companion.db.pojo.measurement.MeasurementWithParentNames;
import com.python.companion.util.genericinterfaces.ResultListener;

import java.util.List;
import java.util.concurrent.Executors;

public class MeasurementQuery {
    private DAOMeasurement daoMeasurement;

    public MeasurementQuery(@NonNull Context context) {
        daoMeasurement = Database.getDatabase(context).getDAOMeasurement();
    }

    public MeasurementQuery(@NonNull Database database) {
        daoMeasurement = database.getDAOMeasurement();
    }

    public void getAll(@NonNull ResultListener<List<Measurement>> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMeasurement.getAll()));
    }

    public void setHasNotifications( boolean hasNotifications, long measurementID) {
        Executors.newSingleThreadExecutor().execute(() -> daoMeasurement.setHasNotifications(measurementID, hasNotifications));
    }

    public void isUnique(String nameSingular, String namePlural, ResultListener<Boolean> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMeasurement.getBySingularOrPlural(nameSingular, namePlural) == null));
    }

    public void isUniqueInstanced(String nameSingular, String namePlural, ResultListener<Measurement> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMeasurement.getBySingularOrPlural(nameSingular, namePlural)));
    }

    public void isUniqueInstancedNamed(String nameSingular, String namePlural, ResultListener<MeasurementWithParentNames> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMeasurement.getBySingularOrPluralNamed(nameSingular, namePlural)));
    }

    public void findByID(long id, ResultListener<Measurement> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMeasurement.findByID(id)));
    }

    public void findByIDNamed(long id, ResultListener<MeasurementWithParentNames> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMeasurement.findByIDNamed(id)));
    }

    @WorkerThread
    public @Nullable Measurement isUniqueInstanced(String nameSingular, String namePlural) {
        return daoMeasurement.getBySingularOrPlural(nameSingular, namePlural);
    }
}
