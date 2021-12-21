package org.python.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RoomNote(
    @PrimaryKey val name: String,
    val content: String
)