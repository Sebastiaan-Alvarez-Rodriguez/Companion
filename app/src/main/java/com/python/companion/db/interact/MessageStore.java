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
     * Insert a given new message into the database when there is no name-conflict
     * @param message Notify to store
     * @param finishListener Receives a call if we inserted the message
     * @param errorListener Receives a call with a description of the error if we had an error while inserting (incurs no changes in database)
     */
    public static void insert(@NonNull Message message, @NonNull Context context, @NonNull FinishListener finishListener, @NonNull ErrorListener errorListener) {
        MessageQuery query = new MessageQuery(context);
        query.isUniqueInstanced(message, conflicting -> {
            if (conflicting == null) { // No conflicting countdown, just insert
                _insert(Database.getDatabase(context).getDAOMessage(), () -> {
                    AnniversaryQuery mq = new AnniversaryQuery(context);
                    mq.setHasNotifications(true, message.getAnniversaryID());
                    finishListener.onFinish();
                }, message);
            } else { // There is a conflict
                if (message.hasCountdown()) { // If we have a conflict because the user specifies a countdown-message and there already exists one, just update it
                    conflicting.setAmount(message.getAmount());
                    conflicting.setType(message.getType());
                    query.update(conflicting);
                    finishListener.onFinish();
                } else { // There is a conflict because user specifies the same notification date twice
                    errorListener.onError("There already is a notification scheduled on this date");
                }
            }
        });
    }

    /**
     * Insert or update (upsert) a notification. 2 notifications are considered equivalent if it is for the same anniversary, and on the same date.
     * @param message Notification to insert
     * @param finishListener Receives a call if we inserted or updated the message
     */
    public static void upsert(@NonNull Message message, @NonNull Context context, @NonNull FinishListener finishListener) {
        MessageQuery query = new MessageQuery(context);
        query.isUniqueInstanced(message, conflicting -> {
            if (conflicting == null) {
                _insert(Database.getDatabase(context).getDAOMessage(), () -> {
                    AnniversaryQuery mq = new AnniversaryQuery(context);
                    mq.setHasNotifications(true, message.getAnniversaryID());
                    finishListener.onFinish();
                }, message);
            } else {
                message.setMessageID(conflicting.getMessageID());
                query.update(message);
            }
        });
    }

    public static void update(@NonNull Message message, @NonNull Context context, @NonNull FinishListener finishListener, @NonNull ErrorListener errorListener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            MessageQuery query = new MessageQuery(context);
            query.isUniqueInstanced(message, conflicting -> {
                if (conflicting == null) {
                    query.update(message);
                    finishListener.onFinish();
                } else { // There is a conflict because user specifies the same notification date twice
                    if (message.hasCountdown()) { // If we have a conflict because the user specifies a countdown-message and there already exists one, just update it
                        conflicting.setAmount(message.getAmount());
                        conflicting.setType(message.getType());
                        query.update(conflicting);
                        finishListener.onFinish();
                    } else { // There is a conflict because user specifies the same notification date twice
                        errorListener.onError("There already is a notification scheduled on this date");
                    }
                }
            });
        });
    }

    /**
     * Deletes a notification. See also {@link #delete(List, Context, FinishListener)} to delete multiple notifications at once slightly more efficient
     * @param message Notification to delete
     * @param finishListener Called once the data is deleted
     */
    public static void delete(@NonNull Message message, @NonNull Context context, @NonNull FinishListener finishListener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            MessageQuery mq = new MessageQuery(context);
            _delete(Database.getDatabase(context).getDAOMessage(), message);
            mq.checkHasNotifications(message.getAnniversaryID(), hasNotifications -> {
                if (!hasNotifications)// We deleted the last notification
                     new AnniversaryQuery(context).setHasNotifications(false, message.getAnniversaryID());
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
            for (Message message : notifies)
                nq.checkHasNotifications(message.getAnniversaryID(), hasNotifications -> {
                    if (!hasNotifications) // We deleted the last notification
                        new AnniversaryQuery(context).setHasNotifications(false, message.getAnniversaryID());
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

    private static void _delete(DAOMessage daoMessage, List<Message> messageList) {
        daoMessage.delete(messageList.toArray(new Message[]{}));
    }

}
