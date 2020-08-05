package com.python.companion.db.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAONotify;
import com.python.companion.db.pojo.notify.NotifyWithMeasurementNames;

import java.util.List;

public class NotifyRepository {
    private DAONotify daoNotify;

    public NotifyRepository(Context context) {
        daoNotify = Database.getDatabase(context).getDAONotify();
    }

    public LiveData<List<NotifyWithMeasurementNames>> getNotificationsForJubileumNamed(long jubileumID) {
        return daoNotify.getForJubileumNamed(jubileumID);
    }
}