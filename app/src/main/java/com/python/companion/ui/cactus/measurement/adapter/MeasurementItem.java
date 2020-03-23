package com.python.companion.ui.cactus.measurement.adapter;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;

import com.mikepenz.fastadapter.FastAdapter.ViewHolder;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.python.companion.R;
import com.python.companion.db.entity.Measurement;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

@SuppressWarnings("unused")
public class MeasurementItem extends AbstractItem<ViewHolder> implements Parcelable {
    public static final @LayoutRes int layoutResource = R.layout.item_measurement;

    private Measurement measurement;

    public MeasurementItem(Measurement measurement) {
        this.measurement = measurement;
    }

    protected MeasurementItem(Parcel in) {
        measurement = new Measurement(in.readString(), in.readString(), Duration.parse(in.readString()), ChronoUnit.valueOf(in.readString()));
    }

    public static final Creator<MeasurementItem> CREATOR = new Creator<MeasurementItem>() {
        @Override
        public MeasurementItem createFromParcel(Parcel in) {
            return new MeasurementItem(in);
        }

        @Override
        public MeasurementItem[] newArray(int size) {
            return new MeasurementItem[size];
        }
    };

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

    @Override
    public void bindView(@NotNull ViewHolder holder, @NotNull List<Object> payloads) {
        super.bindView(holder, payloads);
        if (isSelected())
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        else
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorWindowBackground));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(measurement.getNameSingular());
        dest.writeString(measurement.getNamePlural());
        dest.writeString(measurement.getDuration().toString());
        dest.writeString(measurement.getCornerstoneType().name());
    }


    public static class MeasurementViewHolder extends ViewHolder<MeasurementItem> {
        private TextView nameView;


        public MeasurementViewHolder(@NotNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.item_measurement_name);
        }

        @Override
        public void bindView(@NotNull MeasurementItem item, @NotNull List<Object> list) {
            nameView.setText(item.getMeasurement().getNamePlural());
        }

        @Override
        public void unbindView(@NotNull MeasurementItem item) {

        }
    }
}
