package com.python.companion.db.constant;

import android.content.Context;

import androidx.annotation.NonNull;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAONotify;
import com.python.companion.db.entity.Notify;
import com.python.companion.util.genericinterfaces.ResultListener;

import java.util.concurrent.Executors;

public class NotifyQuery {
    private DAONotify daoNotify;

    public NotifyQuery(@NonNull Context context) {
        daoNotify = Database.getDatabase(context).getDAONotify();
    }

    public void update(Notify... notifies) {
        Executors.newSingleThreadExecutor().execute(() -> daoNotify.update(notifies));
    }

    public void isUniqueInstanced(@NonNull Notify notify, ResultListener<Notify> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoNotify.findConflicting(notify.getJubileumID(), notify.getNotifyDate())));
    }

    public void checkHasNotifications(long measurementID, ResultListener<Boolean> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoNotify.checkHasNotifications(measurementID)));
    }
}