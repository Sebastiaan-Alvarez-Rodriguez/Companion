package com.python.companion.ui.cactus.measurement.adapter;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mikepenz.fastadapter.utils.ComparableItemListImpl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.util.Comparator;

public class CactusSortHandler {
    public static final int SORT_DURATION = 0;
    public static final int SORT_ALPHA = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SORT_ALPHA, SORT_DURATION})
    public @interface MeasurementSortStrategy {}

    protected @NonNull MutableLiveData<Integer> strategy;
    protected ComparableItemListImpl<CactusItem> itemList;

    public static class Builder {
        protected @MeasurementSortStrategy int strategy;
        protected @Nullable ComparableItemListImpl<CactusItem> itemList;

        public Builder() {
            strategy = SORT_DURATION;
        }

        public Builder setStrategy(int strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder setItemList(@NonNull ComparableItemListImpl<CactusItem> itemList) {
            this.itemList = itemList;
            return this;
        }

        public CactusSortHandler build() {
            if (itemList == null)
                throw new IllegalStateException("ItemList must be set");
            return new CactusSortHandler(strategy, itemList);
        }
    }


    protected CactusSortHandler(@MeasurementSortStrategy int strategy, ComparableItemListImpl<CactusItem> itemList) {
        this.strategy = new MutableLiveData<>(strategy);
        this.itemList = itemList;
        resort();
    }



    public void setSortStrategy(@MeasurementSortStrategy int strategy) {
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
    public Comparator<CactusItem> getComparator() {
        switch (strategy.getValue()) {
            case SORT_ALPHA:
                return new MeasurementAlphaComperator();
            case SORT_DURATION:
            default:
                return new MeasurementDateComperator();
        }
    }

    /**
     * Re-sorts the list
     */
    protected void resort() {
        itemList.withComparator(getComparator());
    }

    protected static class MeasurementAlphaComperator implements Comparator<CactusItem> {
        @Override
        public int compare(CactusItem o1, CactusItem o2) {
            return o1.getDisplayMeasurement().compareTo(o2.getDisplayMeasurement());
        }
    }

    protected static class MeasurementDateComperator implements Comparator<CactusItem> {
        @Override
        public int compare(CactusItem o1, CactusItem o2) {
            if (o1.isValDistance())
                return Long.compare(Long.parseLong(o1.getDisplayValue()), Long.parseLong(o2.getDisplayValue()));
            return LocalDate.parse(o1.getDisplayValue()).compareTo(LocalDate.parse(o2.getDisplayValue()));
        }
    }
}
