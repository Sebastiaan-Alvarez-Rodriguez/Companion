package com.python.companion.db.constant;

import android.content.Context;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAONote;
import com.python.companion.db.entity.Note;

import java.util.concurrent.Executors;

public class NoteQuery {
    private DAONote daoNote;

    public NoteQuery(Context context) {
        daoNote = Database.getDatabase(context).getDAONote();
    }

    public void insert(String name, String content, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.insert(new Note(name,content));
            listener.onResult(null);
        });
    }

    public void update(String name, String content, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.update(name, content);
            listener.onResult(null);
        });
    }

    public void update(String prevName, String name, String content, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.update(prevName, name, content);
            listener.onResult(null);
        });
    }

    public void delete(String name, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.delete(new Note(name, ""));
            listener.onResult(null);
        });
    }

    public void getContent(String name, ResultListener<String> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Note note = daoNote.get(name);
            listener.onResult(note == null ? null : note.getContent());
        });
    }

    public void isUnique(String name, ResultListener<Boolean> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoNote.get(name) == null));
    }
}
