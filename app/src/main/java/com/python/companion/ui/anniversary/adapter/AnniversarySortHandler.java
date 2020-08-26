package com.python.companion.ui.anniversary.adapter;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mikepenz.fastadapter.utils.ComparableItemListImpl;
import com.python.companion.ui.anniversary.adapter.item.AnniversaryItem;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;

public class AnniversarySortHandler {
    public static final int SORT_DURATION = 0;
    public static final int SORT_ALPHA = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SORT_ALPHA, SORT_DURATION})
    public @interface AnniversarySortStrategy {}

    protected @NonNull
    MutableLiveData<Integer> strategy;
    protected ComparableItemListImpl<AnniversaryItem> itemList;

    public static class Builder {
        protected @AnniversaryCalculatorSortHandler.CactusSortStrategy
        int strategy;
        protected @Nullable ComparableItemListImpl<AnniversaryItem> itemList;

        public Builder() {
            strategy = SORT_DURATION;
        }

        public Builder setStrategy(int strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder setItemList(@NonNull ComparableItemListImpl<AnniversaryItem> itemList) {
            this.itemList = itemList;
            return this;
        }

        public AnniversarySortHandler build() {
            if (itemList == null)
                throw new IllegalStateException("ItemList must be set");
            return new AnniversarySortHandler(strategy, itemList);
        }
    }


    protected AnniversarySortHandler(@AnniversaryCalculatorSortHandler.CactusSortStrategy int strategy, ComparableItemListImpl<AnniversaryItem> itemList) {
        this.strategy = new MutableLiveData<>(strategy);
        this.itemList = itemList;
        resort();
    }



    public void setSortStrategy(@AnniversarySortStrategy int strategy) {
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
    public Comparator<AnniversaryItem> getComparator() {
        switch (strategy.getValue()) {
            case SORT_ALPHA:
                return new AnniversaryAlphaComperator();
            case SORT_DURATION:
            default:
                return new AnniversaryDateComperator();
        }
    }

    /**
     * Re-sorts the list
     */
    protected void resort() {
        itemList.withComparator(getComparator());
    }

    protected static class AnniversaryAlphaComperator implements Comparator<AnniversaryItem> {
        @Override
        public int compare(AnniversaryItem o1, AnniversaryItem o2) {
            return o1.getAnniversary().getNamePlural().compareTo(o2.getAnniversary().getNamePlural());
        }
    }

    protected static class AnniversaryDateComperator implements Comparator<AnniversaryItem> {
        @Override
        public int compare(AnniversaryItem o1, AnniversaryItem o2) {
            return o1.getAnniversary().compareTo(o2.getAnniversary());
        }
    }
}
