package org.python.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("name", unique = true)])
data class RoomNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val content: String,
    val favorite: Boolean,
    val secure: Boolean,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (other !is RoomNote)
            return false
        return this.id == other.id
    }

    override fun hashCode(): Int {
        return this.id.toInt()
    }
}