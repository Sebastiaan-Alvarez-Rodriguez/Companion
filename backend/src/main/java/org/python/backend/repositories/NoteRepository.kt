package org.python.backend.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import org.python.backend.datatype.Note
import org.python.db.note.daos.NoteDao
import org.python.db.note.entities.RoomNote

class NoteRoomRepository(private val noteDao: NoteDao) {
    // TODO: Test: https://stackoverflow.com/questions/61533937/
    val readAllData : LiveData<List<Note>> = Transformations.map(noteDao.getAll()) { list: List<RoomNote> ->
        list.map { item -> item.toUI() }
    }

    suspend fun add(note: Note) {
        noteDao.insert(note.toRoom())
    }
    suspend fun update(note: Note) {
        noteDao.update(note.toRoom())
    }
    suspend fun delete(note: Note) {
        noteDao.delete(note.toRoom())
    }
}

private fun Note.toRoom() = RoomNote(
    name = name,
    content = content,
)

private fun RoomNote.toUI() = Note(
    name = name,
    content = content,
)