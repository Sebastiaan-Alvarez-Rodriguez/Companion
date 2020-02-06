package com.python.companion.ui.templates.adapter;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.SortedList;

/**
 * Template to create comperators, which handle sorting in SortedLists
 * @param <T> the type of items in the SortedList
 */
@SuppressWarnings("WeakerAccess")
public abstract class Comperator<T> extends SortedList.Callback<T> {
    @NonNull
    protected Adapter<T> adapter;
    @NonNull
    protected Adapter.SortBy strategy;

    public Comperator(@NonNull Adapter<T> adapter, @NonNull Adapter.SortBy strategy) {
        this.adapter = adapter;
        this.strategy = strategy;
    }

    /**
     * Sorting happens here
     * @param o1 The first item
     * @param o2 The second item
     * @return a negative integer, zero, or a positive integer as the first argument is less than,
     * equal to, or greater than the second.
     */
    @Override
    public abstract int compare(T o1, T o2);

    /**
     * Called when one or more consecutive items are changed
     * @param position Starting position of changed items
     * @param count Amount of changed items
     */
    @Override
    public void onChanged(int position, int count) {
        adapter.notifyItemRangeChanged(position, count);
    }

    /**
     * Called when one or more items are inserted
     * @param position Starting position of inserted items
     * @param count Amount of inserted items
     */
    @Override
    public void onInserted(int position, int count) {
        adapter.notifyItemRangeInserted(position, count);
    }

    /**
     * Called when one or more items are removed
     * @param position Starting position of removed items
     * @param count Amount of removed items
     */
    @Override
    public void onRemoved(int position, int count) {
        adapter.notifyItemRangeRemoved(position, count);
    }

    /**
     * Called when one item is moved
     * @param fromPosition Old position of item
     * @param toPosition New position of item
     */
    @Override
    public void onMoved(int fromPosition, int toPosition) {
        adapter.notifyItemMoved(fromPosition, toPosition);
    }

    /**
     * Set a sorting strategy. Beware: list does not automatically re-sort all items.
     * To enforce this behavior, you can retrieve all items from the list, clear the list,
     * add again to the list.
     * @param strategy The new sorting strategy to use.
     */
    public void setStrategy(@NonNull Adapter.SortBy strategy) {
        this.strategy = strategy;
    }

    /**
     * @return the current sorting strategy
     */
    @NonNull
    public Adapter.SortBy getStrategy() {
        return strategy;
    }

    /**
     * Determines whether two items have the same contents
     * @param oldItem Existing item in the list
     * @param newItem Item to add
     * @return true if items have equal contents, false otherwise
     */
    @Override
    public abstract boolean areContentsTheSame(T oldItem, T newItem);

    /**
     * Determines whether two items are the same
     * @param item1 The first item
     * @param item2 The second item
     * @return true if the items are the same, false otherwise
     */
    @Override
    public abstract boolean areItemsTheSame(T item1, T item2);
}
