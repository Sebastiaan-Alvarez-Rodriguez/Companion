package com.python.companion.ui.note.list;

import androidx.annotation.NonNull;

import com.python.companion.db.entity.Note;
import com.python.companion.ui.templates.adapter.Adapter;
import com.python.companion.ui.templates.adapter.Comperator;

public class NoteComperator extends Comperator<Note> {
    public NoteComperator(@NonNull Adapter<Note> adapter, @NonNull Adapter.SortBy strategy) {
        super(adapter, strategy);
    }

    @Override
    public int compare(Note o1, Note o2) {
        if (strategy == Adapter.SortBy.DATE) {
            return o1.getModified().compareTo(o2.getModified());
        } else {
            return o1.getName().compareTo(o2.getName());
        }
    }

    @Override
    public boolean areContentsTheSame(Note oldItem, Note newItem) {
        return oldItem.getName().equals(newItem.getName());
    }

    @Override
    public boolean areItemsTheSame(Note item1, Note item2) {
        return areContentsTheSame(item1,item2);
    }
}
