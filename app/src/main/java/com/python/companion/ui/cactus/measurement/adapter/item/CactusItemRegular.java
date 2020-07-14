package com.python.companion.ui.cactus.measurement.adapter.item;

import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.mikepenz.fastadapter.FastAdapter;
import com.python.companion.R;
import com.python.companion.db.entity.Measurement;
import com.python.companion.ui.cactus.Type;
import com.python.companion.util.MeasurementUtil;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;

public class CactusItemRegular extends CactusItem  {
    public static final @LayoutRes int layoutResource = R.layout.item_cactus;

    protected Measurement measurement;

    private Type currentType;
    private LocalDate date;
    private long distance;
    private boolean hasError;
    private String error;

    protected CactusItemRegular(Measurement measurement) {
        super(measurement);
        this.measurement = measurement;
        this.currentType = Type.DATE;
    }

    public CactusItemRegular(Measurement measurement, LocalDate date) {
        this(measurement);
        this.date = date;
        this.distance = MeasurementUtil.computeDistance(date);
        this.hasError = false;
    }

    public CactusItemRegular(Measurement measurement, String error) {
        this(measurement);
        this.hasError = true;
        this.error = error;
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
        return hasError ? error : (currentType == Type.DATE ? date.toString() : String.valueOf(distance));
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

    /** Called when user changes type. Does not require recomputing date/distance. Manually invalidate view after update */
    public void onTypeChange(Type type) {
        if (type != currentType) {
            currentType = type;
        }
    }

    public void onDateChange(LocalDate date) {
        this.date = date;
        this.distance = MeasurementUtil.computeDistance(date);
        this.hasError = false;
    }

    public void onDateError(@Nullable String msg) {
        hasError = true;
        error = msg == null ? "!" : msg;
    }
}
