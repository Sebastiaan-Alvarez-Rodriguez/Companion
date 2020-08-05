package com.python.companion.ui.notes.note.adapter;

import android.view.View;
import android.widget.ImageView;
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
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.transparent));
    }

    @SuppressWarnings("WeakerAccess")
    public static class NoteViewHolder extends ViewHolder<NoteItem> {
        private TextView nameView, dateView, categoryView;

        private ImageView lockedView, favoriteView;

        public NoteViewHolder(@NotNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.item_note_name);
            dateView = itemView.findViewById(R.id.item_note_date);
            categoryView = itemView.findViewById(R.id.item_note_category);
            lockedView = itemView.findViewById(R.id.item_note_lock);
            favoriteView = itemView.findViewById(R.id.item_note_favorite);
        }

        @Override
        public void bindView(@NotNull NoteItem item, @NotNull List<Object> list) {
            nameView.setText(item.getNote().getName());
            dateView.setText(item.getNote().getModified().toString());
            Category category = item.getNote().getCategory();
            categoryView.setBackgroundColor((category.getCategoryName().length() != 0) ? category.getCategoryColor() : ContextCompat.getColor(nameView.getContext(), R.color.colorPrimary));
            lockedView.setVisibility(item.getNote().isSecure() ? View.VISIBLE : View.INVISIBLE);
            favoriteView.setImageResource(item.getNote().isFavorite() ? R.drawable.ic_cactus_filled : R.drawable.ic_cactus_outline);
        }

        @Override
        public void unbindView(@NotNull NoteItem item) {}
    }
}
