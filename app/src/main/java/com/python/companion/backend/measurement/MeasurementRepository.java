package com.python.companion.backend.measurement;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOMeasurement;
import com.python.companion.db.entity.Measurement;
import com.python.companion.db.pojo.measurement.MeasurementWithParentNames;

import java.util.List;

public class MeasurementRepository {
    private DAOMeasurement daoMeasurement;

    public MeasurementRepository(Context context) {
        daoMeasurement = Database.getDatabase(context).getDAOMeasurement();
    }

    public LiveData<List<Measurement>> getMeasurements() {
        return daoMeasurement.getAllLive();
    }

    public LiveData<List<MeasurementWithParentNames>> getMeasurementsNamed() {
        return daoMeasurement.getAllNamedLive();
    }
}
