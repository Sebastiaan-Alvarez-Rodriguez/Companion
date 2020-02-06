package com.python.companion.ui.note.list.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.SortedList;

import com.python.companion.db.entity.Note;
import com.python.companion.ui.note.list.NoteComperator;
import com.python.companion.ui.templates.adapter.Comperator;
import com.python.companion.ui.templates.adapter.ViewHolder;
import com.python.companion.ui.templates.adapter.action.ActionAdapter;
import com.python.companion.ui.templates.adapter.action.ActionListener;

public class NoteAdapterAction extends ActionAdapter<Note> {

    public NoteAdapterAction(ActionListener<Note> actionListener) {
        super(actionListener);
    }
    @NonNull
    @Override
    protected SortedList<Note> getSortedList(Comperator<Note> comperator) {
        return new SortedList<>(Note.class, comperator);
    }

    @NonNull
    @Override
    protected Comperator<Note> getComperator() {
        return new NoteComperator(this, SortBy.NAME);
    }

    @NonNull
    @Override
    public ViewHolder<Note> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(LayoutInflater.from(parent.getContext()).inflate(NoteViewHolder.layoutResource, parent, false), this);
    }
}
