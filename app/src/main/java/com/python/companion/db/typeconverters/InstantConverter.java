package com.python.companion.db.typeconverters;

import androidx.room.TypeConverter;
import java.time.Instant;
public class InstantConverter {
    @TypeConverter
    public static Instant dateFromTimestamp(Long value) {
        return value == null ? null : Instant.ofEpochSecond(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Instant moment) {
        return moment == null ? null : moment.getEpochSecond();
    }
}