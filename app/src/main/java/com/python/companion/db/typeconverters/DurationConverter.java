package com.python.companion.db.typeconverters;

import androidx.room.TypeConverter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class DurationConverter {
    @TypeConverter
    public static Duration durationFromDays(Long value) {
        return value == null ? null : Duration.of(value, ChronoUnit.DAYS);
    }

    @TypeConverter
    public static Long durationToDays(Duration duration) {
        return duration.toDays();
    }
}
