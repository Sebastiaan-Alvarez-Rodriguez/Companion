package com.python.companion.ui.cactus.measurement.adapter.item;

import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;

import com.mikepenz.fastadapter.FastAdapter;
import com.python.companion.R;
import com.python.companion.db.entity.Measurement;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CactusItemDistance extends CactusItem {
    public static final @LayoutRes int layoutResource = R.layout.item_cactus;

    protected long distance;

    public CactusItemDistance(Measurement measurement, long distance) {
        super(measurement);
        this.distance = distance;
    }

    @NotNull
    @Override
    public FastAdapter.ViewHolder getViewHolder(@NotNull View view) {
        return new CactusItem.MeasurementViewHolder(view);
    }

    @Override
    public int getLayoutRes() {
        return layoutResource;
    }

    @Override
    public int getType() {
        return R.id.item_cactus_layout;
    }

    @Override
    public String getDisplayValue() {
        return String.valueOf(distance);
    }

    @Override
    public String getDisplayMeasurement() {
        return distance == 1 ? measurement.getNameSingular() : measurement.getNamePlural();
    }

    @Override
    public void bindView(@NotNull FastAdapter.ViewHolder holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);
        if (isSelected())
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        else
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorWindowBackground));
    }
}
