package com.python.companion.db.interact;

import android.content.Context;

import androidx.annotation.NonNull;

import com.python.companion.db.Database;
import com.python.companion.db.constant.MeasurementQuery;
import com.python.companion.db.constant.NotifyQuery;
import com.python.companion.db.dao.DAONotify;
import com.python.companion.db.entity.Notify;
import com.python.companion.util.genericinterfaces.ErrorListener;
import com.python.companion.util.genericinterfaces.FinishListener;

import java.util.List;
import java.util.concurrent.Executors;

public class NotifyStore {
    /**
     * Insert a given new notify into the database when there is no name-conflict
     * @param notify Notify to store
     * @param finishListener Receives a call if we inserted the notify
     * @param errorListener Receives a call with a description of the error if we had an error while inserting (incurs no changes in database)
     */
    public static void insert(@NonNull Notify notify, @NonNull Context context, @NonNull FinishListener finishListener, @NonNull ErrorListener errorListener) {
        NotifyQuery query = new NotifyQuery(context);
        query.isUniqueInstanced(notify, conflicting -> {
            if (conflicting == null) {
                _insert(Database.getDatabase(context).getDAONotify(), () -> {
                    MeasurementQuery mq = new MeasurementQuery(context);
                    mq.setHasNotifications(true, notify.getJubileumID());
                    finishListener.onFinish();
                }, notify);
            } else {
                errorListener.onError("There already is a notification scheduled on this date");
            }
        });
    }

    /**
     * Insert or update (upsert) a notification. 2 notifications is considered equivalent if it is for the same anniversary, and on the same date.
     * @param notify Notification to insert
     * @param finishListener Receives a call if we inserted or updated the notify
     */
    public static void upsert(@NonNull Notify notify, @NonNull Context context, @NonNull FinishListener finishListener) {
        NotifyQuery query = new NotifyQuery(context);
        query.isUniqueInstanced(notify, conflicting -> {
            if (conflicting == null) {
                _insert(Database.getDatabase(context).getDAONotify(), () -> {
                    MeasurementQuery mq = new MeasurementQuery(context);
                    mq.setHasNotifications(true, notify.getJubileumID());
                    finishListener.onFinish();
                }, notify);
            } else {
                notify.setNotifyID(conflicting.getNotifyID());
                query.update(notify);
            }
        });
    }

    /**
     * Deletes a notification. See also {@link #delete(List, Context, FinishListener)} to delete multiple notifications at once slightly more efficient
     * @param notify Notification to delete
     * @param finishListener Called once the data is deleted
     */
    public static void delete(@NonNull Notify notify, @NonNull Context context, @NonNull FinishListener finishListener) {
        NotifyQuery nq = new NotifyQuery(context);
        Executors.newSingleThreadExecutor().execute(() -> {
            _delete(Database.getDatabase(context).getDAONotify(), notify);
            nq.checkHasNotifications(notify.getJubileumID(), hasNotifications -> {
                if (!hasNotifications)// We deleted the last notification
                     new MeasurementQuery(context).setHasNotifications(false, notify.getJubileumID());
                finishListener.onFinish();
            });
        });
    }

    /**
     * Deletes multiple notifications. See also {@link #delete(Notify, Context, FinishListener)} to delete one notification
     * @param notifies Notifications to delete
     * @param finishListener Called once the data is deleted
     */
    public static void delete(@NonNull List<Notify> notifies, @NonNull Context context, @NonNull FinishListener finishListener) {
        NotifyQuery nq = new NotifyQuery(context);
        Executors.newSingleThreadExecutor().execute(() -> {
            _delete(Database.getDatabase(context).getDAONotify(), notifies);
            for (Notify notify : notifies)
                nq.checkHasNotifications(notify.getJubileumID(), hasNotifications -> {
                    if (!hasNotifications) // We deleted the last notification
                        new MeasurementQuery(context).setHasNotifications(false, notify.getJubileumID());
                });
            finishListener.onFinish();
        });
    }

    private static void _insert(DAONotify daoNotify, FinishListener finishListener, Notify... notifies) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNotify.insert(notifies);
            finishListener.onFinish();
        });
    }

    private static void _delete(DAONotify daoNotify, Notify... notifies) {
        daoNotify.delete(notifies);
    }

    private static void _delete(DAONotify daoNotify, List<Notify> notifyList) {
        daoNotify.delete(notifyList.toArray(new Notify[]{}));
    }

}
