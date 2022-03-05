
package org.python.db.typeconverters

import android.graphics.Color
import androidx.room.TypeConverter


object ColorConverter {
    @TypeConverter
    fun longToColor(value: Long): Color = Color.valueOf(value)

    @TypeConverter
    fun colorToLong(color: Color): Long = color.pack()
}