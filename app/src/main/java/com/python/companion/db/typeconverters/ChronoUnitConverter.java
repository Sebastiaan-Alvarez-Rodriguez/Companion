package com.python.companion.db.typeconverters;

import androidx.room.TypeConverter;

import java.time.temporal.ChronoUnit;

public class ChronoUnitConverter {
    @TypeConverter
    public static ChronoUnit durationFromDays(String value) {
        return value == null ? null : ChronoUnit.valueOf(value);
    }

    @TypeConverter
    public static String durationToDays(ChronoUnit unit) {
        return unit.name();
    }
}