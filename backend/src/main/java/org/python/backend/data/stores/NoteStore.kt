package org.python.backend.data.stores

import androidx.paging.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteCategory
import org.python.backend.data.datatype.NoteWithCategory
import org.python.db.CompanionDatabase
import org.python.db.entities.note.RoomNote
import org.python.db.entities.note.RoomNoteCategory
import org.python.db.entities.note.RoomNoteWithCategory

class NoteStore(database: CompanionDatabase) {
    private val noteDao = database.noteDao

    fun getAllNotes(): Flow<PagingData<NoteWithCategory>> = pagingNote { noteDao.getAll() }

    fun getAllNotesWithSecure(): Flow<PagingData<NoteWithCategory>> = pagingNote { noteDao.getAllWithSecure() }

    fun hasSecureNotes(): Flow<Boolean> = noteDao.hasSecureNotes()

    /**
     * Searches note by id.
     * @param id Id to search for.
     * @param secure If set, searches secure notes and insecure notes. Otherwise, only searches insecure notes.
     * @return Found note if present, null otherwise
     */
    suspend fun get(id: Long, secure: Boolean = false): Note? = noteDao.get(id, secure)?.toUI()
    suspend fun getWithCategory(id: Long, secure: Boolean = false): NoteWithCategory? =
        noteDao.getWithCategory(id, secure)?.toUI()
    /**
     * Searches note by name.
     * @param name Exact name of note.
     * @param secure If set, searches secure notes and insecure notes. Otherwise, only searches insecure notes.
     * @return Found note if present, null otherwise
     */
    suspend fun getByName(name: String, secure: Boolean = false): Note? = noteDao.getByName(name, secure)?.toUI()

    suspend fun setFavorite(note: Note, favorite: Boolean) = noteDao.setFavorite(note.noteId, favorite)

    suspend fun updateCategoryForNote(noteId: Long, categoryId: Long) = noteDao.updateCategoryForNote(noteId, categoryId)


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

private fun pagingNote(block: () -> PagingSource<Int, RoomNoteWithCategory>): Flow<PagingData<NoteWithCategory>> =
    Pager(PagingConfig(pageSize = 20)) { block() }.flow.map { page -> page.map { it.toUI() } }


private fun Note.toRoom() = RoomNote(
    noteId = noteId,
    name = name,
    content = content,
    favorite = favorite,
    secure = secure,
    iv = iv,
    categoryKey = categoryKey
)

private fun RoomNote.toUI() = Note(
    noteId = noteId,
    name = name,
    content = content,
    favorite = favorite,
    secure = secure,
    iv = iv,
    categoryKey = categoryKey
)

private fun NoteCategory.toRoom() = RoomNoteCategory(categoryId = categoryId, categoryName = name, color = color, favorite = favorite)

private fun RoomNoteCategory.toUI() = NoteCategory(categoryId = categoryId, name = categoryName, color = color, favorite = favorite)

private fun NoteWithCategory.toRoom() = RoomNoteWithCategory(note = note.toRoom(), noteCategory = noteCategory.toRoom())

private fun RoomNoteWithCategory.toUI() = NoteWithCategory(note = note.toUI(), noteCategory = noteCategory.toUI())