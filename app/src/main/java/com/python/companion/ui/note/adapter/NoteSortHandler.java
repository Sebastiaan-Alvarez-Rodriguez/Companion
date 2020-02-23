package com.python.companion.ui.note.adapter;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mikepenz.fastadapter.utils.ComparableItemListImpl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;

@SuppressWarnings("WeakerAccess")
public class NoteSortHandler {
    public static final int SORT_DATE = 1;
    public static final int SORT_ALPHA = 2;
    public static final int SORT_CATEGORY = 3;
    public static final int SORT_LOCKED = 4;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SORT_ALPHA, SORT_DATE, SORT_CATEGORY, SORT_LOCKED})
    public @interface SortingStrategy {
    }

    protected @NonNull MutableLiveData<Integer> strategy;
    protected ComparableItemListImpl<NoteItem> itemList;
    protected SuperComparator comparator;

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
        this.strategy = new MutableLiveData<>(strategy);
        this.itemList = itemList;
        comparator = new SuperComparator(strategy);
    }



    public void setSortStrategy(@SortingStrategy int strategy) {
        if (this.strategy.getValue() != strategy) {
            this.strategy.setValue(strategy);
            comparator.setStrategy(strategy);
            resort();
        }
    }

    public void forceReSort() {
        resort();
    }
    public LiveData<Integer> getSortStrategy() {
        return strategy;
    }
    @Nullable
    public Comparator<NoteItem> getComparator() {
//        switch (strategy.getValue()) {
//            case SORT_ALPHA:
//                return new NoteAlphaComperator();
//            case SORT_DATE:
//            default:
//                return new NoteDateComperator();
//        }
        return comparator;
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

    protected static class SuperComparator implements  Comparator<NoteItem> {
        protected @SortingStrategy int strategy;

        public SuperComparator(@SortingStrategy int strategy) {
            this.strategy = strategy;
        }

        public void setStrategy(@SortingStrategy int strategy) {
            this.strategy = strategy;
        }

        @Override
        public int compare(NoteItem o1, NoteItem o2) {
            if (strategy == SORT_DATE)
                return o1.getNote().getModified().compareTo(o2.getNote().getModified());
            else
                return o1.getNote().getName().compareTo(o2.getNote().getName());
        }
    }
}
