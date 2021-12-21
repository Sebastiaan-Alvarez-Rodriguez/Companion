package org.python.backend.stores

import androidx.paging.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.python.backend.datatype.Note
import org.python.db.CompanionDatabase
import org.python.db.entities.RoomNote

class NoteStore(database: CompanionDatabase) {
    private val noteDao = database.noteDao

    fun getAllNotes(): Flow<PagingData<Note>> = pagingNote { noteDao.getAll() }

    suspend fun getByName(name: String): Note? = noteDao.getByName(name)?.toUI()

    suspend fun add(note: Note): Boolean {
        return try {
            noteDao.add(note.toRoom());
            true;
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            false;
        } catch (e: Exception) {
            false;
        }
    }

    suspend fun upsert(note: Note): Unit = noteDao.upsert(note.toRoom())

    suspend fun update(note: Note) = noteDao.update(note.toRoom())

    suspend fun delete(note: Note) = noteDao.delete(note.toRoom())
}

private fun pagingNote(block: () -> PagingSource<Int, RoomNote>): Flow<PagingData<Note>> =
    Pager(PagingConfig(pageSize = 20)) { block() }.flow.map { page -> page.map(RoomNote::toUI) }


private fun Note.toRoom() = RoomNote(
    name = name,
    content = content,
)

private fun RoomNote.toUI() = Note(
    name = name,
    content = content,
)