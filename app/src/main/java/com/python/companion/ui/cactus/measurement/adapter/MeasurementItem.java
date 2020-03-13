package com.python.companion.ui.cactus.measurement.adapter;

import android.view.View;

import androidx.annotation.LayoutRes;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.python.companion.R;

import org.jetbrains.annotations.NotNull;

public class MeasurementItem extends AbstractItem<FastAdapter.ViewHolder> {
    public static final @LayoutRes int layoutResource = R.layout.item_measurement;

    @NotNull
    @Override
    public FastAdapter.ViewHolder getViewHolder(@NotNull View view) {
        return null;
    }

    @Override
    public int getLayoutRes() {
        return layoutResource;
    }

    @Override
    public int getType() {
        return 0;
    }
}
