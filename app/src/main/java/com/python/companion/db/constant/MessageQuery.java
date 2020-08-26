package com.python.companion.db.constant;

import android.content.Context;

import androidx.annotation.NonNull;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOMessage;
import com.python.companion.db.entity.Message;
import com.python.companion.util.genericinterfaces.ResultListener;

import java.util.concurrent.Executors;

public class MessageQuery {
    private DAOMessage daoMessage;

    public MessageQuery(@NonNull Context context) {
        daoMessage = Database.getDatabase(context).getDAOMessage();
    }

    public void update(Message... messages) {
        Executors.newSingleThreadExecutor().execute(() -> daoMessage.update(messages));
    }

    public void isUniqueInstanced(@NonNull Message message, ResultListener<Message> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMessage.findConflicting(message.getAnniversaryID(), message.getMessageDate())));
    }

    public void checkHasNotifications(long anniversaryID, ResultListener<Boolean> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoMessage.checkHasMessages(anniversaryID)));
    }
}