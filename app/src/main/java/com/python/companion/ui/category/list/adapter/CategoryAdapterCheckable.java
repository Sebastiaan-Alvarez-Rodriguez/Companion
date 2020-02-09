package com.python.companion.ui.category.list.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.SortedList;

import com.python.companion.db.entity.Category;
import com.python.companion.ui.templates.adapter.Comperator;
import com.python.companion.ui.templates.adapter.ViewHolder;
import com.python.companion.ui.templates.adapter.checkable.CheckableAdapter;

public class CategoryAdapterCheckable extends CheckableAdapter<Category> {
    @NonNull
    @Override
    protected SortedList<Category> getSortedList(Comperator<Category> comperator) {
        return new SortedList<>(Category.class, comperator);
    }

    @NonNull
    @Override
    protected Comperator<Category> getComperator() {
        return new Comperator<Category>(this, SortBy.NAME) {
            @Override
            public int compare(Category o1, Category o2) {
                return o1.getCategoryName().compareTo(o2.getCategoryName());
            }

            @Override
            public boolean areContentsTheSame(Category oldItem, Category newItem) {
                return oldItem.getCategoryName().equals(newItem.getCategoryName()) && oldItem.getCategoryColor() == newItem.getCategoryColor();
            }

            @Override
            public boolean areItemsTheSame(Category item1, Category item2) {
                return item1.getCategoryName().equals(item2.getCategoryName());
            }
        };
    }

    @NonNull
    @Override
    public ViewHolder<Category> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CategoryViewHolder(LayoutInflater.from(parent.getContext()).inflate(CategoryViewHolder.layoutResource, parent, false), this);
    }
}
