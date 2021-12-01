package org.python.backend.note.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RoomNote(
    @PrimaryKey val name: String,
    val content: String
)

private fun Note.toRoom() = RoomNote(
    name = name,
    content = content,
)

private fun RoomNote.toUI() = Note(
    name = name,
    content = content,
)