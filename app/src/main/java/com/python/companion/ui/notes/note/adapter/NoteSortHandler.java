package com.python.companion.ui.notes.note.adapter;

import android.util.Log;

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
    public static final int SORT_DATE = 0;
    public static final int SORT_ALPHA = 1;
    public static final int SORT_CATEGORY = 2;
    public static final int SORT_LOCK = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SORT_ALPHA, SORT_DATE, SORT_CATEGORY, SORT_LOCK})
    public @interface NoteSortStrategy {
    }

    protected @NonNull MutableLiveData<Integer> strategy;
    protected ComparableItemListImpl<NoteItem> itemList;

    public static class Builder {
        protected @NoteSortStrategy
        int strategy;
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


    protected NoteSortHandler(@NoteSortStrategy int strategy, ComparableItemListImpl<NoteItem> itemList) {
        Log.e("NotesortHandler", "Created with strategy: "+strategy);
        this.strategy = new MutableLiveData<>(strategy);
        this.itemList = itemList;
        resort();
    }



    public void setSortStrategy(@NoteSortStrategy int strategy) {
        Log.e("NotesortHandler", "Someone set strategy to: "+strategy);
        if (this.strategy.getValue() != strategy) {
            this.strategy.setValue(strategy);
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
        switch (strategy.getValue()) {
            case SORT_ALPHA:
                return new NoteAlphaComperator();
            case SORT_CATEGORY:
                return new NoteCategoryComperator();
            case SORT_LOCK:
                return new NoteLockComperator();
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
    protected static class NoteCategoryComperator implements Comparator<NoteItem> {
        @Override
        public int compare(NoteItem o1, NoteItem o2) {
            return o1.getNote().getCategory().getCategoryName().compareTo(o2.getNote().getCategory().getCategoryName());
        }
    }

    protected static class NoteLockComperator implements Comparator<NoteItem> {
        @Override
        public int compare(NoteItem o1, NoteItem o2) {
            return (o1.getNote().isSecure() == o2.getNote().isSecure()) ? 0 : (o1.getNote().isSecure() ? -1 : 1);
        }
    }
}
