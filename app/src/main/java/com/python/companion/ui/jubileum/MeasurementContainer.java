package com.python.companion.ui.jubileum;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.python.companion.db.entity.Measurement;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class MeasurementContainer implements Parcelable {
    private @NonNull Measurement measurement;

    public MeasurementContainer(@NonNull Measurement measurement) {
        this.measurement = measurement;
    }

    protected MeasurementContainer(Parcel in) {
        measurement = new Measurement(in.readLong(), in.readString(), in.readString(), Duration.parse(in.readString()), in.readLong(), in.readLong(), in.readLong(), ChronoUnit.valueOf(in.readString()), in.readBoolean());
    }

    public @NonNull Measurement getMeasurement() {
        return measurement;
    }

    public static final Creator<MeasurementContainer> CREATOR = new Creator<MeasurementContainer>() {
        @Override
        public MeasurementContainer createFromParcel(Parcel in) {
            return new MeasurementContainer(in);
        }

        @Override
        public MeasurementContainer[] newArray(int size) {
            return new MeasurementContainer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(measurement.getMeasurementID());
        dest.writeString(measurement.getNameSingular());
        dest.writeString(measurement.getNamePlural());
        dest.writeString(measurement.getDuration().toString());
        dest.writeLong(measurement.getAmount());
        dest.writeLong(measurement.getPrecomputedamount());
        dest.writeLong(measurement.getParentID());
        dest.writeString(measurement.getCornerstoneType().name());
        dest.writeBoolean(measurement.getHasNotifications());
    }
}
