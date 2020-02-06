package com.python.companion.ui.templates.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Template to create an Adapter, which works with architecture LiveData
 * @param <T> The type of items of the list to be displayed
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class Adapter<T> extends RecyclerView.Adapter<ViewHolder<T>> implements Observer<List<T>>, InternalClickListener {

    protected SortedList<T> list;
    protected Comperator<T> comperator;
    protected OnClickListener<T> onClickListener;

    /**
     * Constructor which sets given onClickListener to send callbacks to, if the listener is not null
     * @param onClickListener Listener to send callbacks in case of item clicks
     */
    public Adapter(OnClickListener<T> onClickListener) {
        this.onClickListener = onClickListener;
        this.comperator = getComperator();
        this.list = getSortedList(comperator);
    }

    /**
     * @see #Adapter(OnClickListener)
     * Same function, but sets no listener
     */
    public Adapter() {
        this(null);
    }

    /**
     * Simple function to return the list of items
     * @return the list of items
     */
    public List<T> getItems() {
        return toList(list);
    }

    /**
     * Adds a single item to the list, and displays it to the user
     * @param item The item to be added
     */
    public void add(T item) {
        list.add(item);
    }

    /**
     * @see #add(Object)
     * Function to add a collection of items to the list
     */
    public void add(Collection<T> items) {
        list.addAll(items);
    }

    /**
     * Removes a single item from the list, if it is in the list. Does nothing otherwise
     * @param item item to be removed
     */
    public void remove(T item) {
        list.remove(item);
    }

    /**
     * @see #remove(Object)
     * Function to remove a collection of items from the list
     */
    public void remove(Collection<T> items) {
        list.beginBatchedUpdates();
        for (T item : items)
            list.remove(item);
        list.endBatchedUpdates();
    }

    public void replaceAll(@NonNull Collection<T> items) {
        list.beginBatchedUpdates();
        for (int i = list.size() -1; i >= 0; i--) {
            final T item = list.get(i);
            if (!items.contains(item))
                list.remove(item);
        }
        list.addAll(items);
        list.endBatchedUpdates();
    }

    public SortBy getSortStrategy() {
        return comperator.getStrategy();
    }
    public void sort(SortBy strategy) {
        if (comperator.getStrategy() == strategy)
            return;

        comperator.setStrategy(strategy);
        if (list.size() > 1) {
            List<T> items = toList(list);
            list.beginBatchedUpdates();
            list.clear();
            list.addAll(items);
            list.endBatchedUpdates();
        }
    }

    public enum SortBy {
        DATE,
        NAME
    }

    /**
     * Assigns a viewholder an item from the list, depending on the position of the viewholder in the list
     * @param holder The viewholder to receive an item from the UI list
     * @param position The position of the viewholder in the UI list
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder<T> holder, int position) {
        T item = list.get(position);
        holder.set(item);
    }

    /**
     * @return the amount of items in the list
     */
    @Override
    public int getItemCount() {
        return list.size();
    }

    /**
     * Propagates click callback from viewholder to listener.
     * @param view The clicked view
     * @param pos the clicked position
     */
    @Override
    public void onClick(View view, int pos) {
        if (onClickListener != null)
            onClickListener.onClick(list.get(pos));
    }

    /**
     * Propagates long-click callback from viewholder to listener.
     * @param view The clicked view
     * @param pos the clicked position
     */
    @Override
    public boolean onLongClick(View view, int pos) {
        if (onClickListener != null)
            return onClickListener.onLongClick(list.get(pos));
        return true;
    }

    /**
     * Callback receiver for list changes.
     * @param newList the new List
     */
    @Override
    public void onChanged(@Nullable List<T> newList) {
        list.replaceAll(newList == null ? new ArrayList<>() : newList);
//        List<T> removed = ListUtil.getRemoved(list, newList);
//        List<T> added = ListUtil.getAdded(list, newList);
//        remove(removed);
//        add(added);
    }


    protected abstract @NonNull
    SortedList<T> getSortedList(Comperator<T> comperator);

    protected abstract @NonNull Comperator<T> getComperator();

    private List<T> toList(SortedList<T> sortedList) {
        List<T> l = new ArrayList<>();
        for (int i = 0; i < sortedList.size(); i++)
            l.add(sortedList.get(i));
        return l;
    }
}
