package com.python.companion.ui.templates.adapter.checkable;


import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.R;
import com.python.companion.ui.templates.adapter.Adapter;
import com.python.companion.ui.templates.adapter.OnClickListener;
import com.python.companion.ui.templates.adapter.ViewHolder;
import com.python.companion.util.ListUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @see Adapter
 * Template specialization to allow items to be checked
 * @param <T> the type of items in the list
 */
@SuppressWarnings({"WeakerAccess", "BooleanMethodIsAlwaysInverted"})
public abstract class CheckableAdapter<T> extends Adapter<T> {
    protected Set<T> selected_items;

    /**
     * Constructor to set initially selected items and provide a listener to send click callbacks to
     * @param initialSelected List of initially selected items
     * @param onClickListener Listener to send callbacks in case of item clicks
     */
    public CheckableAdapter(List<T> initialSelected, OnClickListener<T> onClickListener) {
        super(onClickListener);
        selected_items = initialSelected == null ? new HashSet<>() : new HashSet<>(initialSelected);
    }

    /**
     * @see #CheckableAdapter(List, OnClickListener)
     * Same function, only takes away the need to call with second null argument
     */
    public CheckableAdapter(List<T> initialSelected) { this(initialSelected, null);}

    /**
     * @see #CheckableAdapter(List, OnClickListener)
     * Same function, only takes away the need to call with 2 null arguments
     */
    public CheckableAdapter() { this(null, null); }

    /**
     * Sets selected items
     * @param items The list of items to be set as selected
     */
    public void setSelected(Collection<T> items) {
        List<T> removed = ListUtil.getRemoved(selected_items, items);
        List<T> added = ListUtil.getAdded(selected_items, items);
        for (T t : removed)
            notifyItemChanged(list.indexOf(t));
        for (T t : added)
            notifyItemChanged(list.indexOf(t));
        selected_items.clear();
        selected_items.addAll(items);
    }

    /**
     * @see Adapter#onClick(View, int)
     * Here, selecting and background changing is handled
     */
    @Override
    public void onClick(View view, int pos) {
        T item = list.get(pos);
        if (selected_items.add(item)) {
            view.setBackgroundResource(R.color.colorAccent);
        } else {
            selected_items.remove(item);
            view.setBackgroundResource(android.R.color.transparent);
        }
        super.onClick(view, pos);
    }

    /**
     * @see Adapter#onLongClick(View, int)
     * Here, selecting and background changing is handled
     */
    @Override
    public boolean onLongClick(View view, int pos) {
        T item = list.get(pos);
        if (selected_items.add(item)) {
            view.setBackgroundResource(R.color.colorAccent);
        } else {
            selected_items.remove(item);
            view.setBackgroundResource(android.R.color.transparent);
        }
        return super.onLongClick(view, pos);
    }

    /**
     * @see Adapter#onBindViewHolder(ViewHolder, int)
     * Same as linked function, but colors background if selected
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder<T> holder, int position) {
        if (selected_items.contains(list.get(position)))
            holder.itemView.setBackgroundResource(R.color.colorAccent);
        super.onBindViewHolder(holder, position);
    }

    /**
     * When a view is sent to recycle view pool, it may have a colored background.
     * Such colors are removed to ensure the view does not appear always selected when rebound
     * @param holder The holder the view belonged to
     */
    @Override
    public void onViewRecycled(@NonNull ViewHolder<T> holder) {
        holder.itemView.setBackgroundResource(android.R.color.transparent);
        super.onViewRecycled(holder);
    }

    /**
     * @return the amount of selected items
     */
    public int getSelectedCount() {
        return selected_items.size();
    }

    /**
     * @return whether any items are selected at all
     */
    public boolean hasSelected() {
        return !selected_items.isEmpty();
    }

    /**
     * @return all selected items, by value
     */
    public Set<T> getSelected() {
        return new HashSet<>(selected_items);
    }

    /**
     * @see Adapter#onChanged(List)
     * Clears selected items before a change happens
     */
    @Override
    public void onChanged(@Nullable List<T> newList) {
        selected_items.clear();
        super.onChanged(newList);
    }
}
