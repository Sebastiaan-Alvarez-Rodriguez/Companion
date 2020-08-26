package com.python.companion.db.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOAnniversary;
import com.python.companion.db.entity.Anniversary;
import com.python.companion.db.pojo.anniversary.AnniversaryWithParentNames;

import java.util.List;

public class AnniversaryRepository {
    private DAOAnniversary daoAnniversary;

    public AnniversaryRepository(Context context) {
        daoAnniversary = Database.getDatabase(context).getDAOAnniversary();
    }

    public LiveData<List<Anniversary>> getAnniversarys() {
        return daoAnniversary.getAllLive();
    }

    public LiveData<List<AnniversaryWithParentNames>> getAnniversarysNamed() {
        return daoAnniversary.getAllNamedLive();
    }
}
