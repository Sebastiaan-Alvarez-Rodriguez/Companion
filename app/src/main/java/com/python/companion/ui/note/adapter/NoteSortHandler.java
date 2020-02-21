package com.python.companion.ui.note.adapter;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mikepenz.fastadapter.utils.ComparableItemListImpl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;

@SuppressWarnings("WeakerAccess")
public class NoteSortHandler {
    public static final int SORT_DATE = 1;
    public static final int SORT_ALPHA = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SORT_ALPHA, SORT_DATE})
    public @interface SortingStrategy {
    }

    protected @SortingStrategy int strategy;
    protected ComparableItemListImpl<NoteItem> itemList;

    public static class Builder {
        protected @SortingStrategy int strategy;
        protected @Nullable ComparableItemListImpl<NoteItem> itemList;

        public Builder() {
            strategy = SORT_DATE;
        }

        public Builder setStrategy(int strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder setItemList(@NonNull ComparableItemListImpl<NoteItem> itemList) {
            this.itemList = itemList;
            return this;
        }

        public NoteSortHandler build() {
            if (itemList == null)
                throw new IllegalStateException("ItemList must be set");
            return new NoteSortHandler(strategy, itemList);
        }
    }


    protected NoteSortHandler(@SortingStrategy int strategy, ComparableItemListImpl<NoteItem> itemList) {
        this.strategy = strategy;
        this.itemList = itemList;
    }

    public void setSortStrategy(@SortingStrategy int strategy) {
        if (this.strategy != strategy) {
            this.strategy = strategy;
            resort();
        }
    }

    public @SortingStrategy int getSortStrategy() {
        return strategy;
    }
    @Nullable
    public Comparator<NoteItem> getComparator() {
        switch (strategy) {

            case SORT_ALPHA:
                return new NoteAlphaComperator();
            case SORT_DATE:
            default:
                return new NoteDateComperator();
        }
    }

    /**
     * Re-sorts the list
     */
    protected void resort() {
        itemList.withComparator(getComparator());
    }

    protected static class NoteAlphaComperator implements Comparator<NoteItem> {

        @Override
        public int compare(NoteItem o1, NoteItem o2) {
            return o1.getNote().getName().compareTo(o2.getNote().getName());
        }
    }

    protected static class NoteDateComperator implements Comparator<NoteItem> {

        @Override
        public int compare(NoteItem o1, NoteItem o2) {
            return o1.getNote().getModified().compareTo(o2.getNote().getModified());
        }
    }
}
