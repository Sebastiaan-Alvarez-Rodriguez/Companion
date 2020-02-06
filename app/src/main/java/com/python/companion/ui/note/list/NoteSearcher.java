package com.python.companion.ui.note.list;

import androidx.annotation.NonNull;

import com.python.companion.db.entity.Note;
import com.python.companion.ui.templates.search.Searcher;

import java.util.List;
import java.util.stream.Collectors;

public class NoteSearcher extends Searcher<Note> {
    public NoteSearcher(EventListener<Note> listener) {
        super(listener);
    }

    @NonNull
    @Override
    protected List<Note> filter(List<Note> list, @NonNull String query) {
        return list.stream().filter(note -> note.getName().toLowerCase().contains(query)).collect(Collectors.toList());
    }
}
