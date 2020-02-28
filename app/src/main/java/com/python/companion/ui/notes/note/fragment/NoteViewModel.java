package com.python.companion.ui.notes.note.fragment;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.python.companion.backend.note.NoteRepository;
import com.python.companion.db.entity.Note;

import java.util.List;

public class NoteViewModel extends AndroidViewModel {
    private NoteRepository noteRepository;

    private LiveData<List<Note>> notes = null;

    public NoteViewModel(@NonNull Application application) {
        super(application);
        noteRepository = new NoteRepository(application);
    }


    public LiveData<List<Note>> getNotes() {
        if (notes == null)
            notes = noteRepository.getNotes();
        return notes;
    }
}