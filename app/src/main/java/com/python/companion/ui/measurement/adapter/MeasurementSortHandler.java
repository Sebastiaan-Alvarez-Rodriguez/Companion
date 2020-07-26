package com.python.companion.ui.measurement.adapter;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mikepenz.fastadapter.utils.ComparableItemListImpl;
import com.python.companion.ui.cactus.adapter.CactusSortHandler;
import com.python.companion.ui.measurement.adapter.item.MeasurementItem;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;

public class MeasurementSortHandler {
    public static final int SORT_DURATION = 0;
    public static final int SORT_ALPHA = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SORT_ALPHA, SORT_DURATION})
    public @interface MeasurementSortStrategy {}

    protected @NonNull
    MutableLiveData<Integer> strategy;
    protected ComparableItemListImpl<MeasurementItem> itemList;

    public static class Builder {
        protected @CactusSortHandler.CactusSortStrategy
        int strategy;
        protected @Nullable ComparableItemListImpl<MeasurementItem> itemList;

        public Builder() {
            strategy = SORT_DURATION;
        }

        public Builder setStrategy(int strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder setItemList(@NonNull ComparableItemListImpl<MeasurementItem> itemList) {
            this.itemList = itemList;
            return this;
        }

        public MeasurementSortHandler build() {
            if (itemList == null)
                throw new IllegalStateException("ItemList must be set");
            return new MeasurementSortHandler(strategy, itemList);
        }
    }


    protected MeasurementSortHandler(@CactusSortHandler.CactusSortStrategy int strategy, ComparableItemListImpl<MeasurementItem> itemList) {
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
    public Comparator<MeasurementItem> getComparator() {
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

    protected static class MeasurementAlphaComperator implements Comparator<MeasurementItem> {
        @Override
        public int compare(MeasurementItem o1, MeasurementItem o2) {
            return o1.getMeasurement().getNamePlural().compareTo(o2.getMeasurement().getNamePlural());
        }
    }

    protected static class MeasurementDateComperator implements Comparator<MeasurementItem> {
        @Override
        public int compare(MeasurementItem o1, MeasurementItem o2) {
            return o1.getMeasurement().compareTo(o2.getMeasurement());
        }
    }
}
