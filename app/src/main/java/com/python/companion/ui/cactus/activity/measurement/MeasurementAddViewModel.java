package com.python.companion.ui.cactus.activity.measurement;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOMeasurement;
import com.python.companion.db.entity.Measurement;

import java.util.List;

public class MeasurementAddViewModel extends AndroidViewModel {
    private DAOMeasurement daoMeasurement;

    private LiveData<List<Measurement>> data;

    public MeasurementAddViewModel(@NonNull Application application) {
        super(application);
        daoMeasurement = Database.getDatabase(application).getDAOMeasurement();
        data = null;
    }

    public LiveData<List<Measurement>> getMeasurements() {
        if (data == null)
            data = daoMeasurement.getAllLive();
        return data;
    }
}
