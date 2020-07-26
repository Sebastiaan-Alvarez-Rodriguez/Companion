package com.python.companion.ui.measurement.adapter.item;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.python.companion.R;
import com.python.companion.db.entity.Measurement;
import com.python.companion.db.pojo.measurement.MeasurementWithParentNames;
import com.python.companion.util.MeasurementUtil;
import com.python.companion.util.ThreadUtil;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Executors;

public class MeasurementItem extends AbstractItem<FastAdapter.ViewHolder> {
    public static final @LayoutRes
    int layoutResource = R.layout.item_measurement;

    private Measurement measurement;
    private @NonNull String parentSingular;
    private @NonNull String parentPlural;

    public MeasurementItem(@NonNull MeasurementWithParentNames measurementWithParentNames) {
        this.measurement = measurementWithParentNames.measurement;
        if (measurementWithParentNames.parentSingular == null || measurementWithParentNames.parentPlural == null) {
            long id = measurementWithParentNames.measurement.getParentID();
            this.parentSingular = MeasurementUtil.IDtoName(id, 1);
            this.parentPlural = MeasurementUtil.IDtoName(id, 2);
        } else {
            this.parentSingular = measurementWithParentNames.parentSingular;
            this.parentPlural = measurementWithParentNames.parentPlural;
        }
    }
    public MeasurementItem(@NonNull Measurement measurement, @NonNull String parentSingular, @NonNull String parentPlural) {
        this.measurement = measurement;
        this.parentSingular = parentSingular;
        this.parentPlural = parentPlural;
    }

    @NotNull
    @Override
    public FastAdapter.ViewHolder getViewHolder(@NotNull View view) {
        return new MeasurementItem.MeasurementViewHolder(view);
    }

    @Override
    public int getLayoutRes() {
        return layoutResource;
    }

    @Override
    public int getType() {
        return R.id.item_measurement_layout;
    }

    @Override
    public long getIdentifier() {
        return measurement.getMeasurementID();
    }

    public Measurement getMeasurement() {
        return measurement;
    }

    public void setMeasurement(Measurement measurement) {
        this.measurement = measurement;
    }

    @NonNull
    public String getParentSingular() {
        return parentSingular;
    }

    public void setParentSingular(@NonNull String parentSingular) {
        this.parentSingular = parentSingular;
    }

    @NonNull
    public String getParentPlural() {
        return parentPlural;
    }

    public void setParentPlural(@NonNull String parentPlural) {
        this.parentPlural = parentPlural;
    }

    @Override
    public void bindView(@NotNull FastAdapter.ViewHolder holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);
        if (isSelected())
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        else
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorWindowBackground));
    }


    public static class MeasurementViewHolder extends FastAdapter.ViewHolder<MeasurementItem> {
        private TextView nameView, equalityView, definitionView;


        public MeasurementViewHolder(@NotNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.item_measurement_name);
            equalityView = itemView.findViewById(R.id.item_measurement_equality);
            definitionView = itemView.findViewById(R.id.item_measurement_definition);
        }

        @Override
        public void bindView(@NotNull MeasurementItem item, @NotNull List<Object> list) {
            nameView.setText(item.getMeasurement().getNamePlural());
            equalityView.setText("1 "+item.getMeasurement().getNameSingular()+" =");
            Executors.newSingleThreadExecutor().execute(() -> {
                String prelude = (item.getMeasurement().getCornerstoneType().isDurationEstimated()) ? "approx. " : "";
                long amount = item.getMeasurement().getAmount();

                String parentname = amount == 1 ? item.getParentSingular() : item.getParentPlural();
                ThreadUtil.runOnUIThread(() -> definitionView.setText(prelude+amount+" "+parentname));
            });
        }

        @Override
        public void unbindView(@NotNull MeasurementItem item) {

        }
    }
}
