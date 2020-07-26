package com.python.companion.ui.measurement.adapter.item;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;

import com.mikepenz.fastadapter.FastAdapter.ViewHolder;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.python.companion.R;
import com.python.companion.db.entity.Measurement;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("unused")
public class MeasurementItemSimple extends AbstractItem<ViewHolder> {
    public static final @LayoutRes int layoutResource = R.layout.item_text;

    private Measurement measurement;

    public MeasurementItemSimple(Measurement measurement) {
        this.measurement = measurement;
    }

    @NotNull
    @Override
    public ViewHolder getViewHolder(@NotNull View view) {
        return new MeasurementSimpleViewHolder(view);
    }

    @Override
    public int getLayoutRes() {
        return layoutResource;
    }

    @Override
    public int getType() {
        return R.id.item_measurement_layout;
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public void setMeasurement(Measurement measurement) {
        this.measurement = measurement;
    }

    @Override
    public void bindView(@NotNull ViewHolder holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);
        if (isSelected())
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        else
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorWindowBackground));
    }

    @Override
    public long getIdentifier() {
        return measurement.getMeasurementID();
    }

    public static class MeasurementSimpleViewHolder extends ViewHolder<MeasurementItemSimple> {
        private TextView nameView;


        public MeasurementSimpleViewHolder(@NotNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.item_text_text);
        }

        @Override
        public void bindView(@NotNull MeasurementItemSimple item, @NotNull List<Object> list) {
            nameView.setText(item.getMeasurement().getNamePlural());
        }

        @Override
        public void unbindView(@NotNull MeasurementItemSimple item) {

        }
    }
}
