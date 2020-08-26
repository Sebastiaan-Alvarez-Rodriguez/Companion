package com.python.companion.db.interact;

import android.content.Context;

import androidx.annotation.NonNull;

import com.python.companion.db.Database;
import com.python.companion.db.constant.AnniversaryQuery;
import com.python.companion.db.constant.MessageQuery;
import com.python.companion.db.dao.DAOMessage;
import com.python.companion.db.entity.Message;
import com.python.companion.util.genericinterfaces.ErrorListener;
import com.python.companion.util.genericinterfaces.FinishListener;

import java.util.List;
import java.util.concurrent.Executors;

public class MessageStore {
    /**
     * Insert a given new notify into the database when there is no name-conflict
     * @param notify Notify to store
     * @param finishListener Receives a call if we inserted the notify
     * @param errorListener Receives a call with a description of the error if we had an error while inserting (incurs no changes in database)
     */
    public static void insert(@NonNull Message notify, @NonNull Context context, @NonNull FinishListener finishListener, @NonNull ErrorListener errorListener) {
        MessageQuery query = new MessageQuery(context);
        query.isUniqueInstanced(notify, conflicting -> {
            if (conflicting == null) {
                _insert(Database.getDatabase(context).getDAOMessage(), () -> {
                    AnniversaryQuery mq = new AnniversaryQuery(context);
                    mq.setHasNotifications(true, notify.getAnniversaryID());
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
    public static void upsert(@NonNull Message notify, @NonNull Context context, @NonNull FinishListener finishListener) {
        MessageQuery query = new MessageQuery(context);
        query.isUniqueInstanced(notify, conflicting -> {
            if (conflicting == null) {
                _insert(Database.getDatabase(context).getDAOMessage(), () -> {
                    AnniversaryQuery mq = new AnniversaryQuery(context);
                    mq.setHasNotifications(true, notify.getAnniversaryID());
                    finishListener.onFinish();
                }, notify);
            } else {
                notify.setMessageID(conflicting.getMessageID());
                query.update(notify);
            }
        });
    }

    /**
     * Deletes a notification. See also {@link #delete(List, Context, FinishListener)} to delete multiple notifications at once slightly more efficient
     * @param notify Notification to delete
     * @param finishListener Called once the data is deleted
     */
    public static void delete(@NonNull Message notify, @NonNull Context context, @NonNull FinishListener finishListener) {
        MessageQuery nq = new MessageQuery(context);
        Executors.newSingleThreadExecutor().execute(() -> {
            _delete(Database.getDatabase(context).getDAOMessage(), notify);
            nq.checkHasNotifications(notify.getAnniversaryID(), hasNotifications -> {
                if (!hasNotifications)// We deleted the last notification
                     new AnniversaryQuery(context).setHasNotifications(false, notify.getAnniversaryID());
                finishListener.onFinish();
            });
        });
    }

    /**
     * Deletes multiple notifications. See also {@link #delete(Message, Context, FinishListener)} to delete one notification
     * @param notifies Notifications to delete
     * @param finishListener Called once the data is deleted
     */
    public static void delete(@NonNull List<Message> notifies, @NonNull Context context, @NonNull FinishListener finishListener) {
        MessageQuery nq = new MessageQuery(context);
        Executors.newSingleThreadExecutor().execute(() -> {
            _delete(Database.getDatabase(context).getDAOMessage(), notifies);
            for (Message notify : notifies)
                nq.checkHasNotifications(notify.getAnniversaryID(), hasNotifications -> {
                    if (!hasNotifications) // We deleted the last notification
                        new AnniversaryQuery(context).setHasNotifications(false, notify.getAnniversaryID());
                });
            finishListener.onFinish();
        });
    }

    private static void _insert(DAOMessage daoMessage, FinishListener finishListener, Message... notifies) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoMessage.insert(notifies);
            finishListener.onFinish();
        });
    }

    private static void _delete(DAOMessage daoMessage, Message... notifies) {
        daoMessage.delete(notifies);
    }

    private static void _delete(DAOMessage daoMessage, List<Message> notifyList) {
        daoMessage.delete(notifyList.toArray(new Message[]{}));
    }

}
