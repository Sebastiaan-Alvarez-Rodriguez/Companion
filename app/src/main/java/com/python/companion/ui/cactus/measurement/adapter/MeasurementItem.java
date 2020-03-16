package com.python.companion.ui.cactus.measurement.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.mikepenz.fastadapter.FastAdapter.ViewHolder;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.python.companion.R;
import com.python.companion.db.entity.Measurement;
import com.python.companion.ui.cactus.requestor.ComputeInjectable;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;

public class MeasurementItem extends AbstractItem<ViewHolder> {
    public static final @LayoutRes int layoutResource = R.layout.item_measurement;

    private Measurement measurement;

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
        return R.id.item_measurement_layout;
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public void setMeasurement(Measurement measurement) {
        this.measurement = measurement;
    }

    public static class MeasurementViewHolder extends ViewHolder<MeasurementItem> implements ComputeInjectable {
        private TextView amountView, nameView;


        public MeasurementViewHolder(@NotNull View itemView) {
            super(itemView);
            amountView = itemView.findViewById(R.id.item_measurement_amount);
            nameView = itemView.findViewById(R.id.item_measurement_name);
        }

        @Override
        public void bindView(@NotNull MeasurementItem item, @NotNull List<Object> list) {

            nameView.setText(item.getMeasurement().getNamePlural());
        }

        @Override
        public void unbindView(@NotNull MeasurementItem item) {

        }

        @Override
        public void onShiftDate(@NonNull LocalDate newDate) {

        }
    }
}
