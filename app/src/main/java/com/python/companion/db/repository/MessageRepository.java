package com.python.companion.db.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAOMessage;
import com.python.companion.db.entity.Message;

import java.util.List;

public class MessageRepository {
    private DAOMessage daoMessage;

    public MessageRepository(Context context) {
        daoMessage = Database.getDatabase(context).getDAOMessage();
    }

    public LiveData<List<Message>> getMessagesForAnniversary(long anniversaryID) {
        return daoMessage.getMessagesForAnniversaryLive(anniversaryID);
    }
}