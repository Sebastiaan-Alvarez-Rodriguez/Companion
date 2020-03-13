package com.python.companion.db.typeconverters;

import androidx.room.TypeConverter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class DurationConverter {
    @TypeConverter
    public static Duration durationFromSeconds(Long value) {
        return value == null ? null : Duration.of(value, ChronoUnit.SECONDS);
    }

    @TypeConverter
    public static Long durationToSeconds(Duration duration) {
        return duration.getSeconds();
    }
}
