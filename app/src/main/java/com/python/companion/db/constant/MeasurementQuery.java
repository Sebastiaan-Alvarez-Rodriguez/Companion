package com.python.companion.db.constant;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOMeasurement;
import com.python.companion.db.entity.Measurement;
import com.python.companion.db.pojo.measurement.MeasurementWithParentNames;
import com.python.companion.util.NotificationUtil;
import com.python.companion.util.genericinterfaces.FinishListener;
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

    /** Insert a new measurement */
    public void insert(@NonNull Measurement measurement) {
        Executors.newSingleThreadExecutor().execute(() -> daoMeasurement.insert(measurement));
    }

    public void delete(@NonNull Context context, @NonNull List<Measurement> measurements, @NonNull FinishListener listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            NotificationUtil.deleteChannels2(context, measurements);
            for (Measurement m : measurements)
                daoMeasurement.deleteInherit(m);
            listener.onFinish();
        });
    }

    public void delete(@NonNull Context context, @NonNull Measurement measurement, @NonNull FinishListener listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            NotificationUtil.deleteChannel(context, measurement);
            daoMeasurement.deleteInherit(measurement);
            listener.onFinish();
        });
    }

    public void update(@NonNull Measurement measurement, @NonNull Measurement old, @NonNull ResultListener<Boolean> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMeasurement.updateInherit(measurement, old)));
    }

    public void getAll(@NonNull ResultListener<List<Measurement>> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMeasurement.getAll()));
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

    public void findByIDNamed(long id, ResultListener<MeasurementWithParentNames> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMeasurement.findByIDNamed(id)));
    }

    @WorkerThread
    public @Nullable Measurement isUniqueInstanced(String nameSingular, String namePlural) {
        return daoMeasurement.getBySingularOrPlural(nameSingular, namePlural);
    }
}
