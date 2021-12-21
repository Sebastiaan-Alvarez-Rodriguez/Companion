package org.python.db.typeconverters

import androidx.room.TypeConverter
import java.time.Duration
import java.time.temporal.ChronoUnit


object DurationConverter {
    @TypeConverter
    fun durationFromDays(value: Long?): Duration? {
        return if (value == null) null else Duration.of(value, ChronoUnit.DAYS)
    }

    @TypeConverter
    fun durationToDays(duration: Duration): Long {
        return duration.toDays()
    }
}