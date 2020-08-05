package com.python.companion.ui.jubileum.adapter.item;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.mikepenz.fastadapter.FastAdapter.ViewHolder;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.python.companion.R;
import com.python.companion.db.entity.Measurement;

import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("unused")
public class JubileumItemSimple extends AbstractItem<ViewHolder> {
    public static final @LayoutRes int layoutResource = R.layout.item_text;

    private Measurement measurement;

    private boolean displayPlural;

    public JubileumItemSimple(@NonNull Measurement measurement) {
        this(measurement, true);
    }

    public JubileumItemSimple(@NonNull Measurement measurement, boolean displayPlural) {
        this.measurement = measurement;
        this.displayPlural = displayPlural;
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
        return R.id.item_jubileum_layout;
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public void setMeasurement(Measurement measurement) {
        this.measurement = measurement;
    }

    public boolean getDisplayPlural() {
        return displayPlural;
    }

    public void setDisplayPlural(boolean displayPlural) {
        this.displayPlural = displayPlural;
    }

    @Override
    public void bindView(@NotNull ViewHolder holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);
        if (isSelected())
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        else
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.transparent));
    }

    @Override
    public long getIdentifier() {
        return measurement.getMeasurementID();
    }

    public static class MeasurementSimpleViewHolder extends ViewHolder<JubileumItemSimple> {
        private TextView nameView;


        public MeasurementSimpleViewHolder(@NotNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.item_text_text);
        }

        @Override
        public void bindView(@NotNull JubileumItemSimple item, @NotNull List<Object> list) {
            nameView.setText(item.getDisplayPlural() ? item.getMeasurement().getNamePlural() : item.getMeasurement().getNameSingular());
        }

        @Override
        public void unbindView(@NotNull JubileumItemSimple item) {}
    }
}
