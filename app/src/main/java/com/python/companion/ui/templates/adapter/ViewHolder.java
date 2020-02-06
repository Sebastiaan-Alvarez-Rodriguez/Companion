package com.python.companion.ui.templates.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Template to create viewholders, which control individual displayed items in the list
 * @param <T> The type of items in the list
 */
public abstract class ViewHolder<T> extends RecyclerView.ViewHolder {
    protected InternalClickListener clickListener;

    public ViewHolder(@NonNull View itemView) {
        this(itemView, null);
    }

    private ViewHolder(@NonNull View itemView, InternalClickListener clickListener) {
        super(itemView);
        this.clickListener = clickListener;
    }

    public abstract void set(T t);

}
