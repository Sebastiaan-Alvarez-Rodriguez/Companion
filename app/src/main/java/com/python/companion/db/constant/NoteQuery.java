package com.python.companion.db.constant;

import android.content.Context;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAONote;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;

import java.util.Collection;
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

    public void updateContent(String name, String content, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.updateContent(name, content);
            listener.onResult(null);
        });
    }

    public void updateContent(String prevName, String name, String content, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.updateContent(prevName, name, content);
            listener.onResult(null);
        });
    }

    public void updateCategory(String name, Category category, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.updateCategory(name, category.getCategoryName(), category.getCategoryColor());
            listener.onResult(null);
        });
    }

    public void delete(String name, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.delete(new Note(name, ""));
            listener.onResult(null);
        });
    }

    public void delete(Collection<Note> notes, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.delete(notes.toArray(new Note[]{}));
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

    public void isUniqueInstanced(String name, ResultListener<Note> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoNote.get(name)));
    }
}
