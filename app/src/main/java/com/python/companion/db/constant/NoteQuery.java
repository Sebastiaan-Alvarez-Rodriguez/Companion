package com.python.companion.db.constant;

import android.content.Context;

import androidx.annotation.ColorInt;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAONote;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;
import com.python.companion.ui.notes.note.NoteType;

import java.util.Collection;
import java.util.List;
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

    public void insert(Note... notes) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.insert(notes);
        });
    }

    public void update(Note note, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.update(note);
            listener.onResult(null);
        });
    }

    public void replace(String prevName, Note note, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.replace(prevName, note);
            listener.onResult(null);
        });
    }

    public void updateFavorite(String name, boolean isFavorite, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.updateFavorite(name, isFavorite);
            listener.onResult(null);
        });
    }

    /**
     * Update category of 1 item, specified by given name
     * @param name Note name to receive update
     * @param category Updated category
     */
    public void updateCategory(String name, Category category, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.updateCategory(name, category.getCategoryName(), category.getCategoryColor());
            listener.onResult(null);
        });
    }

    /**
     * Set category of multiple notes to 1 specific category at once
     */
    public void updateCategories(Collection<String> names, Category category, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
                daoNote.updateCategories(names, category);
                listener.onResult(null);
        });
    }

    /**
     * Update category of *all* items. Use this function if you delete or merge or change entire categories.
     * In such cases, also don't forget to update the Category table too.
     * @param prevCategoryName 'old' name of category
     * @param categoryName new category name
     * @param color new color
     */
    public void updateEntireCategory(String prevCategoryName, String categoryName, @ColorInt int color, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.updateEntireCategory(prevCategoryName, categoryName, color);
            listener.onResult(null);
        });
    }

    public void updateType(String name, @NoteType.Type int type, ResultListener<Void> listener) {
        Executors.newSingleThreadExecutor().execute(() -> {
            daoNote.updateType(name, type);
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

    public void get(String name, ResultListener<Note> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoNote.get(name)));
    }

    public void getAll(boolean secureOnesToo, ResultListener<List<Note>> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoNote.getAll(secureOnesToo)));
    }

    public void isUnique(String name, ResultListener<Boolean> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoNote.get(name) == null));
    }

    public void isUniqueInstanced(String name, ResultListener<Note> listener) {
        Executors.newSingleThreadExecutor().execute(() -> listener.onResult(daoNote.get(name)));
    }
}
