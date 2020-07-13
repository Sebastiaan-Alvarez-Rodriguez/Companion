package com.python.companion.ui.cactus.measurement.adapter.item;

import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;

import com.mikepenz.fastadapter.FastAdapter;
import com.python.companion.R;
import com.python.companion.db.entity.Measurement;
import com.python.companion.ui.cactus.type.Type;
import com.python.companion.ui.cactus.type.TypeChangeListener;
import com.python.companion.util.measurement.MeasurementUtil;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;

public class CactusItemRegular extends CactusItem implements TypeChangeListener  {
    public static final @LayoutRes int layoutResource = R.layout.item_cactus;

    protected Measurement measurement;

    private Type currentType;
    private LocalDate date;
    private long distance;

    public CactusItemRegular(Measurement measurement, LocalDate date) {
        super(measurement);
        this.measurement = measurement;
        this.currentType = Type.DATE;
        this.date = date;
        this.distance = MeasurementUtil.computeDistance(date);
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

    public Measurement getMeasurement() {
        return measurement;
    }

    public String getDisplayValue() {
        return currentType == Type.DATE ? date.toString() : String.valueOf(distance);
    }

    @Override
    public String getDisplayMeasurement() {
        return currentType == Type.DISTANCE && distance == 1 ? measurement.getNameSingular() : measurement.getNamePlural();
    }

    @Override
    public void bindView(@NotNull FastAdapter.ViewHolder holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);
        if (isSelected())
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        else
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorWindowBackground));
    }

    @Override
    public void onTypeChange(Type type) {
        if (type != currentType) {
            currentType = type;
        }
    }
}
