package com.python.companion.ui.anniversary;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.python.companion.db.entity.Anniversary;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class AnniversaryContainer implements Parcelable {
    private @NonNull
    Anniversary anniversary;

    public AnniversaryContainer(@NonNull Anniversary anniversary) {
        this.anniversary = anniversary;
    }

    protected AnniversaryContainer(Parcel in) {
        anniversary = new Anniversary(in.readLong(), in.readString(), in.readString(), Duration.parse(in.readString()), in.readLong(), in.readLong(), in.readLong(), ChronoUnit.valueOf(in.readString()), in.readBoolean(), in.readBoolean());
    }

    public @NonNull
    Anniversary getAnniversary() {
        return anniversary;
    }

    public static final Creator<AnniversaryContainer> CREATOR = new Creator<AnniversaryContainer>() {
        @Override
        public AnniversaryContainer createFromParcel(Parcel in) {
            return new AnniversaryContainer(in);
        }

        @Override
        public AnniversaryContainer[] newArray(int size) {
            return new AnniversaryContainer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(anniversary.getAnniversaryID());
        dest.writeString(anniversary.getNameSingular());
        dest.writeString(anniversary.getNamePlural());
        dest.writeString(anniversary.getDuration().toString());
        dest.writeLong(anniversary.getAmount());
        dest.writeLong(anniversary.getPrecomputedamount());
        dest.writeLong(anniversary.getParentID());
        dest.writeString(anniversary.getCornerstoneType().name());
        dest.writeBoolean(anniversary.getHasNotifications());
        dest.writeBoolean(anniversary.getCanModify());
    }
}
