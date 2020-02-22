package com.python.companion.ui.note.activity.view;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.python.companion.db.Database;
import com.python.companion.db.dao.DAONote;
import com.python.companion.db.entity.Note;

public class NoteViewViewModel extends AndroidViewModel {
    private DAONote daoNote;
    private LiveData<Note> data;

    public NoteViewViewModel(@NonNull Application application) {
        super(application);
        daoNote = Database.getDatabase(application).getDAONote();
    }

    public LiveData<Note> getNote(@NonNull String name) {
        if (data == null)
            data = daoNote.getLive(name);
        return data;
    }
}
