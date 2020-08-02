package com.python.companion.db.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAONote;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;

import java.util.List;

public class NoteRepository {
    private DAONote daoNote;

    public NoteRepository(Context context) {
        daoNote = Database.getDatabase(context).getDAONote();
    }

    public LiveData<List<Note>> getNotes() {
        return daoNote.getAllLive();
    }

    public LiveData<Category> getCategoryForNote(String noteName) {
        return daoNote.getCategoryLive(noteName);
    }
}
