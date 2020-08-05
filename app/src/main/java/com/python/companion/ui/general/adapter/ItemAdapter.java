package com.python.companion.ui.general.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.python.companion.ui.jubileum.adapter.item.JubileumSpinnerItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic adapter, not optimized much. Sample implementation given at {@link JubileumSpinnerItem}
 */
public class ItemAdapter<T extends ItemAdapter.Item<ItemAdapter.ViewHolder<T>>> extends BaseAdapter {
    protected @NonNull LayoutInflater inflater;
    protected @Nullable List<T> items;

    public static <T extends ItemAdapter.Item<ItemAdapter.ViewHolder<T>>> ItemAdapter<T> from(@NonNull Context context) {
        return new ItemAdapter<>(context);
    }

    protected ItemAdapter(@NonNull Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    /** Sets data. Makes copy of provided items. See also {@link #setData(List)} */
    public ItemAdapter<T> with(@NonNull List<T> items) {
        this.items = new ArrayList<>(items);
        return this;
    }

    /** Sets data. Makes copy of provided items. See also {@link #with(List)}*/
    public void setData(@NonNull List<T> items) {
        this.items = new ArrayList<>(items);
    }

    /** Returns all items referred to by the adapter */
    public @Nullable List<T> getData() {
        return items == null ? null : new ArrayList<>(items);
    }

    @Override
    public int getCount() {
        return items == null ? 0 : items.size();
    }

    @Override
    public T getItem(int position) {
        if (items == null)
            throw new IllegalStateException("Cannot fetch position for a data-unininitialized adapter");
        else if (position >= items.size())
            throw new IndexOutOfBoundsException("Cannot fetch position "+position+" in an adapter with "+items.size()+" items");
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (items == null)
            throw new IllegalStateException("Cannot get view for uninitialized layout");
        else if (position >= items.size())
            throw new IndexOutOfBoundsException("Cannot get item at position "+position+" in a data set of "+position+" items");

        Item<ViewHolder<T>> item = items.get(position);
        View v = (convertView == null) ? inflater.inflate(item.getLayoutRes(), null) : convertView;
        ViewHolder<T> viewHolder = item.getViewHolder(v);

        viewHolder.bindView(items.get(position));
        return v;
    }


    public abstract static class IITem {
        public abstract @LayoutRes int getLayoutRes();
    }

    public abstract static class Item<VH> extends IITem {
        public abstract @NonNull VH getViewHolder(@NonNull View view);
    }

    public abstract static class ViewHolder<T extends IITem> {
        protected @NonNull View view;
        
        public ViewHolder(@NonNull View view) {
            this.view = view;
        }

        public abstract void bindView(@NonNull T item);
    }
}
