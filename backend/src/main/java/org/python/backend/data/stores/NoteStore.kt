package org.python.backend.data.stores

import androidx.paging.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteCategory
import org.python.db.CompanionDatabase
import org.python.db.entities.note.RoomNote
import org.python.db.entities.note.RoomNoteCategory
import org.python.db.entities.note.RoomNoteWithCategory

class NoteStore(database: CompanionDatabase) {
    private val noteDao = database.noteDao

    fun getAllNotes(): Flow<PagingData<Pair<Note, NoteCategory?>>> = pagingNote { noteDao.getAll() }

    fun getAllNotesWithSecure(): Flow<PagingData<Pair<Note, NoteCategory?>>> = pagingNote { noteDao.getAllWithSecure() }

    fun hasSecureNotes(): Flow<Boolean> = noteDao.hasSecureNotes()

    /**
     * Searches note by name.
     * @param name Exact name of note.
     * @param secure If set, searches secure notes and insecure notes. Otherwise, only searches insecure notes.
     * @return Found note if present, null otherwise
     */
    suspend fun getByName(name: String, secure: Boolean = false): Note? = noteDao.getByName(name, secure)?.toUI()

    suspend fun setFavorite(note: Note, favorite: Boolean) = noteDao.setFavorite(note.id, favorite)
    suspend fun add(note: Note): Boolean {
        return try {
            noteDao.add(note.toRoom())
            true
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun upsert(note: Note): Unit = noteDao.upsert(note.toRoom())

    suspend fun update(note: Note) = noteDao.update(note.toRoom())

    suspend fun delete(note: Note) = noteDao.delete(note.toRoom())

    suspend fun deleteAllSecure(foreach: ((String) -> Unit)? = null) = noteDao.deleteAllSecure(foreach)
}

private fun pagingNote(block: () -> PagingSource<Int, RoomNoteWithCategory>): Flow<PagingData<Pair<Note, NoteCategory?>>> =
    Pager(PagingConfig(pageSize = 20)) { block() }.flow.map { page -> page.map {
        it.note.toUI() to it.roomNoteCategory?.toUI()
    } }


private fun Note.toRoom() = RoomNote(
    id = id,
    name = name,
    content = content,
    favorite = favorite,
    secure = secure,
    iv = iv,
    categoryId = categoryId
)

private fun RoomNote.toUI() = Note(
    id = id,
    name = name,
    content = content,
    favorite = favorite,
    secure = secure,
    iv = iv,
    categoryId = categoryId
)

private fun NoteCategory.toRoom() = RoomNoteCategory(id = id, name = name, color = color)

private fun RoomNoteCategory.toUI() = NoteCategory(id = id, name = name, color = color)