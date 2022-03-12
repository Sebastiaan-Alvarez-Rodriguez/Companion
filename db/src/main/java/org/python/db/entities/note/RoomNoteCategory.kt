package org.python.db.entities.note

import android.graphics.Color
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("name", unique = true)])
data class RoomNoteCategory(
    @PrimaryKey(autoGenerate = true) val categoryId: Long = 0,
    val name: String,
    val color: Color,
    val favorite: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (other !is RoomNoteCategory)
            return false
        return this.categoryId == other.categoryId
    }

    override fun hashCode(): Int {
        return this.categoryId.hashCode()
    }
}