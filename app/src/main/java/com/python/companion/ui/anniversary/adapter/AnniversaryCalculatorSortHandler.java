package com.python.companion.ui.anniversary.adapter;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mikepenz.fastadapter.utils.ComparableItemListImpl;
import com.python.companion.ui.anniversary.adapter.item.AnniversaryCalculatorItem;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;

public class AnniversaryCalculatorSortHandler {
    public static final int SORT_DURATION = 0;
    public static final int SORT_ALPHA = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SORT_ALPHA, SORT_DURATION})
    public @interface CactusSortStrategy {}

    protected @NonNull MutableLiveData<Integer> strategy;
    protected ComparableItemListImpl<AnniversaryCalculatorItem> itemList;

    public static class Builder {
        protected @CactusSortStrategy
        int strategy;
        protected @Nullable ComparableItemListImpl<AnniversaryCalculatorItem> itemList;

        public Builder() {
            strategy = SORT_DURATION;
        }

        public Builder setStrategy(int strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder setItemList(@NonNull ComparableItemListImpl<AnniversaryCalculatorItem> itemList) {
            this.itemList = itemList;
            return this;
        }

        public AnniversaryCalculatorSortHandler build() {
            if (itemList == null)
                throw new IllegalStateException("ItemList must be set");
            return new AnniversaryCalculatorSortHandler(strategy, itemList);
        }
    }


    protected AnniversaryCalculatorSortHandler(@CactusSortStrategy int strategy, ComparableItemListImpl<AnniversaryCalculatorItem> itemList) {
        this.strategy = new MutableLiveData<>(strategy);
        this.itemList = itemList;
        resort();
    }



    public void setSortStrategy(@CactusSortStrategy int strategy) {
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
    public Comparator<AnniversaryCalculatorItem> getComparator() {
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

    protected static class AnniversaryAlphaComperator implements Comparator<AnniversaryCalculatorItem> {
        @Override
        public int compare(AnniversaryCalculatorItem o1, AnniversaryCalculatorItem o2) {
            return o1.getAnniversaryName().compareTo(o2.getAnniversaryName());
        }
    }

    protected static class AnniversaryDateComperator implements Comparator<AnniversaryCalculatorItem> {
        @Override
        public int compare(AnniversaryCalculatorItem o1, AnniversaryCalculatorItem o2) {
            return o1.getAnniversary().compareTo(o2.getAnniversary());
        }
    }
}
