package org.python.db.note.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RoomNote(
    @PrimaryKey val name: String,
    val content: String
)