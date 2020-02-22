package com.python.companion.ui.note.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;

import com.mikepenz.fastadapter.FastAdapter.ViewHolder;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.python.companion.R;
import com.python.companion.db.entity.Category;
import com.python.companion.db.entity.Note;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class NoteItem extends AbstractItem<ViewHolder> {
    public static final @LayoutRes int layoutResource = R.layout.item_note;

    private Note note;

    @NotNull
    @Override
    public ViewHolder getViewHolder(@NotNull View view) {
        return new NoteViewHolder(view);
    }

    @Override
    public int getLayoutRes() {
        return layoutResource;
    }

    @Override
    public int getType() {
        return R.id.item_note_layout;
    }

    public void setNote(Note note) {
        this.note = note;
    }

    public Note getNote() {
        return note;
    }

    @Override
    public void bindView(@NotNull ViewHolder holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);
        if (isSelected())
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        else
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorWindowBackground));
    }

    @SuppressWarnings("WeakerAccess")
    public class NoteViewHolder extends ViewHolder<NoteItem> {
        private TextView nameView, dateView, categoryView;

        public NoteViewHolder(@NotNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.item_note_name);
            dateView = itemView.findViewById(R.id.item_note_date);
            categoryView = itemView.findViewById(R.id.item_note_category);
        }

        @Override
        public void bindView(@NotNull NoteItem item, @NotNull List<Object> list) {
            nameView.setText(item.getNote().getName());
            dateView.setText(item.getNote().getModified().toString());
            Category category = item.getNote().getCategory();
            categoryView.setBackgroundColor((category.getCategoryName().length() != 0) ? category.getCategoryColor() : ContextCompat.getColor(nameView.getContext(), R.color.colorPrimary));
        }

        @Override
        public void unbindView(@NotNull NoteItem item) {

        }
    }
}
