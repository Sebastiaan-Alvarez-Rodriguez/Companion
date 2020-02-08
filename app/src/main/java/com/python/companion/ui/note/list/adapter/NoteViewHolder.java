package com.python.companion.ui.note.list.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.R;
import com.python.companion.db.entity.Note;
import com.python.companion.ui.templates.adapter.InternalClickListener;
import com.python.companion.ui.templates.adapter.ViewHolder;

public class NoteViewHolder extends ViewHolder<Note> {
    public static final @LayoutRes int layoutResource = R.layout.item_note;

    private TextView Name, Date;

    public NoteViewHolder(@NonNull View itemView, @Nullable InternalClickListener clickListener) {
        super(itemView);
        this.clickListener = clickListener;
        Name = itemView.findViewById(R.id.item_note_name);
        Date = itemView.findViewById(R.id.item_note_date);
        setupClicks();
    }

    private void setupClicks() {
        if (clickListener == null)
            return;
        itemView.setOnClickListener(v -> clickListener.onClick(v, getAdapterPosition()));
        itemView.setOnLongClickListener(v -> clickListener.onLongClick(v, getAdapterPosition()));
    }

    @Override
    public void set(Note note) {
        Name.setText(note.getName());
//        Date.setText(note.getDate());
    }
}
