package org.python.db.typeconverters

import androidx.room.TypeConverter
import java.time.Instant

object InstantConverter {
    @TypeConverter
    fun dateFromTimestamp(value: Long?): Instant? {
        return if (value == null) null else Instant.ofEpochSecond(value)
    }

    @TypeConverter
    fun dateToTimestamp(moment: Instant?): Long? {
        return moment?.epochSecond
    }
}