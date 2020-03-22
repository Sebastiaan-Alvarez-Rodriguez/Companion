package com.python.companion.ui.cactus.measurement.adapter;

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
public class CactusItem extends AbstractItem<ViewHolder> {
    public static final @LayoutRes int layoutResource = R.layout.item_cactus;

    //TODO: Make inheritance structure for showing date or distance
    private Measurement measurement;
    private String displayValue, displayMeasurement;

    /** Boolean indicating whether we currently display a distance (aka long) or date in ISO-8601 (yyyy-MM-dd) **/
    private boolean valIsDistance;

    public CactusItem(Measurement measurement, String displayValue, String displayMeasurement, boolean valIsDistance) {
        this.measurement = measurement;
        this.displayValue = displayValue;
        this.displayMeasurement = displayMeasurement;
        this.valIsDistance = valIsDistance;
    }

    @NotNull
    @Override
    public ViewHolder getViewHolder(@NotNull View view) {
        return new MeasurementViewHolder(view);
    }

    @Override
    public int getLayoutRes() {
        return layoutResource;
    }

    @Override
    public int getType() {
        return R.id.item_cactus_layout;
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public void setMeasurement(Measurement measurement) {
        this.measurement = measurement;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayMeasurement() {
        return displayMeasurement;
    }

    public void setDisplayMeasurement(String displayMeasurement) {
        this.displayMeasurement = displayMeasurement;
    }

    public boolean isValDistance() {
        return valIsDistance;
    }

    public void setValIsDistance(boolean valIsDistance) {
        this.valIsDistance = valIsDistance;
    }

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
