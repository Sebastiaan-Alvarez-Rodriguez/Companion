package org.python.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("name", unique = true)])
data class RoomNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val content: String
)