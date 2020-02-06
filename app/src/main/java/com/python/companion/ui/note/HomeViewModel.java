package com.python.companion.ui.note;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.python.companion.db.entity.Note;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<List<Note>> notes;

    public HomeViewModel() {
        notes = new MutableLiveData<>();
        ArrayList<Note> n = new ArrayList<>();
        n.add(new Note("*testoof*", "*Some contents*"));
        notes.setValue(n);
    }

    public LiveData<List<Note>> getNotes() {
        return notes;
    }
}