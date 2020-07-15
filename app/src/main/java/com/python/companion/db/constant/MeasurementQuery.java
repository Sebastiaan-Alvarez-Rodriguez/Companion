package com.python.companion.db.constant;

import android.content.Context;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOMeasurement;
import com.python.companion.db.entity.Measurement;
import com.python.companion.util.genericinterfaces.ResultListener;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;

public class MeasurementQuery {
    private DAOMeasurement daoMeasurement;

    public MeasurementQuery(Context context) {
        daoMeasurement = Database.getDatabase(context).getDAOMeasurement();
    }

    public void insert(String nameSingular, String namePlural, Duration duration, ChronoUnit cornerstone) {
        Executors.newSingleThreadExecutor().execute(() -> daoMeasurement.insert(new Measurement(nameSingular, namePlural, duration, cornerstone)));
    }
    public void insert(String nameSingular, String namePlural, Duration duration, ChronoUnit cornerstone, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            insert(nameSingular, namePlural, duration, cornerstone);
            listener.onResult(null);
        });
    }

    public void delete(List<Measurement> measurements, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoMeasurement.delete(measurements.toArray(new Measurement[]{}));
            listener.onResult(null);
        });
    }

    public void isUnique(String namePlural, ResultListener<Boolean> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMeasurement.get(namePlural) == null));
    }
}
