package com.python.companion.ui.anniversary.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOAnniversary;
import com.python.companion.db.entity.Anniversary;

import java.util.List;

public class AnniversaryViewModel extends AndroidViewModel {
    private DAOAnniversary daoAnniversary;

    private LiveData<List<Anniversary>> data;

    public AnniversaryViewModel(@NonNull Application application) {
        super(application);
        daoAnniversary = Database.getDatabase(application).getDAOAnniversary();
        data = null;
    }

    public LiveData<List<Anniversary>> getAnniversaries() {
        if (data == null)
            data = daoAnniversary.getAllLive();
        return data;
    }
}
