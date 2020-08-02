package com.python.companion.ui.jubileum.adapter;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mikepenz.fastadapter.utils.ComparableItemListImpl;
import com.python.companion.ui.jubileum.adapter.item.JubileumCalculatorItem;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;

public class JubileumCalculatorSortHandler {
    public static final int SORT_DURATION = 0;
    public static final int SORT_ALPHA = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SORT_ALPHA, SORT_DURATION})
    public @interface CactusSortStrategy {}

    protected @NonNull MutableLiveData<Integer> strategy;
    protected ComparableItemListImpl<JubileumCalculatorItem> itemList;

    public static class Builder {
        protected @CactusSortStrategy
        int strategy;
        protected @Nullable ComparableItemListImpl<JubileumCalculatorItem> itemList;

        public Builder() {
            strategy = SORT_DURATION;
        }

        public Builder setStrategy(int strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder setItemList(@NonNull ComparableItemListImpl<JubileumCalculatorItem> itemList) {
            this.itemList = itemList;
            return this;
        }

        public JubileumCalculatorSortHandler build() {
            if (itemList == null)
                throw new IllegalStateException("ItemList must be set");
            return new JubileumCalculatorSortHandler(strategy, itemList);
        }
    }


    protected JubileumCalculatorSortHandler(@CactusSortStrategy int strategy, ComparableItemListImpl<JubileumCalculatorItem> itemList) {
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
    public Comparator<JubileumCalculatorItem> getComparator() {
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

    protected static class MeasurementAlphaComperator implements Comparator<JubileumCalculatorItem> {
        @Override
        public int compare(JubileumCalculatorItem o1, JubileumCalculatorItem o2) {
            return o1.getMeasurementName().compareTo(o2.getMeasurementName());
        }
    }

    protected static class MeasurementDateComperator implements Comparator<JubileumCalculatorItem> {
        @Override
        public int compare(JubileumCalculatorItem o1, JubileumCalculatorItem o2) {
            return o1.getMeasurement().compareTo(o2.getMeasurement());
        }
    }
}
