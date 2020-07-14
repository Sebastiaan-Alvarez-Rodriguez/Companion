package com.python.companion.ui.cactus.measurement.adapter.item;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.mikepenz.fastadapter.FastAdapter.ViewHolder;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.python.companion.R;
import com.python.companion.db.entity.Measurement;

import org.jetbrains.annotations.NotNull;

import java.util.List;


public abstract class CactusItem extends AbstractItem<ViewHolder> {
    protected Measurement measurement;

    public CactusItem(Measurement measurement) {
        this.measurement = measurement;
    }

    @NotNull
    @Override
    public ViewHolder getViewHolder(@NotNull View view) {
        return new MeasurementViewHolder(view);
    }

    @Override
    public int getType() {
        return R.id.item_cactus_layout;
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public abstract String getDisplayValue();

    public abstract String getDisplayMeasurement();


    @Override
    public void bindView(@NotNull ViewHolder holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);
        if (isSelected())
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        else
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorWindowBackground));
    }

    public static class MeasurementViewHolder extends ViewHolder<CactusItem> {
        private TextView amountView, nameView;


        public MeasurementViewHolder(@NotNull View itemView) {
            super(itemView);
            amountView = itemView.findViewById(R.id.item_cactus_amount);
            nameView = itemView.findViewById(R.id.item_cactus_name);
        }

        @Override
        public void bindView(@NotNull CactusItem item, @NotNull List<Object> list) {
            amountView.setText(item.getDisplayValue());
            nameView.setText(item.getDisplayMeasurement());
        }

        @Override
        public void unbindView(@NotNull CactusItem item) {
        }
    }
}
