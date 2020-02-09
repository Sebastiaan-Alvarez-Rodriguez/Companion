package com.python.companion.ui.category.list.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.python.companion.R;
import com.python.companion.db.entity.Category;
import com.python.companion.ui.templates.adapter.InternalClickListener;
import com.python.companion.ui.templates.adapter.ViewHolder;

public class CategoryViewHolder extends ViewHolder<Category> {
    public static @LayoutRes int layoutResource = R.layout.item_category;

    private TextView color, name;

    public CategoryViewHolder(@NonNull View itemView, InternalClickListener clickListener) {
        super(itemView);
        this.clickListener = clickListener;
        color = itemView.findViewById(R.id.item_category_color);
        name = itemView.findViewById(R.id.item_category_name);
        setupClicks();
    }

    private void setupClicks() {
        if (clickListener == null)
            return;
        itemView.setOnClickListener(v -> clickListener.onClick(v, getAdapterPosition()));
        itemView.setOnLongClickListener(v -> clickListener.onLongClick(v, getAdapterPosition()));
    }

    @Override
    public void set(Category category) {
        name.setText(category.getCategoryName());
        color.setBackgroundColor(category.getCategoryColor());
    }
}
