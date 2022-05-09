package org.python.backend.data.stores

import androidx.paging.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteCategory
import org.python.backend.data.datatype.NoteWithCategory
import org.python.backend.data.datatype.RenderType
import org.python.datacomm.DataResult
import org.python.datacomm.Result
import org.python.datacomm.ResultType
import org.python.db.CompanionDatabase
import org.python.db.entities.note.RoomNote
import org.python.db.entities.note.RoomNoteCategory
import org.python.db.entities.note.RoomNoteWithCategory

class NoteStore(database: CompanionDatabase) {
    private val noteDao = database.noteDao

    ////////////////////////////////
    // Secure section;
    // All functions here have user verification checks built-in.
    ////////////////////////////////

    fun getAllNotes(
        clearance: Int,
        sortColumn: RoomNoteWithCategory.Companion.SortableField,
        ascending: Boolean
    ): Flow<PagingData<NoteWithCategory>> =
        pagingNote { noteDao.getAll(clearance, sortColumn, ascending) }

    /**
     * Searches note by id.
     * @param id Id to search for.
     * @param clearance Current clearance level of the user.
     * @return Found note if present, null otherwise
     */
    suspend fun get(id: Long, clearance: Int): Note? = noteDao.get(id, clearance)?.toUI()
    suspend fun getWithCategory(id: Long, clearance: Int): NoteWithCategory? = noteDao.getWithCategory(id, clearance)?.toUI()

    fun getWithCategoryLive(id: Long, clearance: Int): Flow<NoteWithCategory?> =
        noteDao.getWithCategoryLive(id, clearance).mapLatest { it?.toUI() }

    /**
     * Searches note by name.
     * @param name Exact name of note.
     * @param clearance Current clearance level of the user.
     * @return Found note if present, null otherwise
     */
    suspend fun getByName(name: String, clearance: Int): Note? = noteDao.getByName(name, clearance)?.toUI()

    suspend fun setFavorite(note: Note, favorite: Boolean, clearance: Int) = noteDao.setFavorite(note.noteId, favorite, clearance)

    suspend fun setRenderType(noteId: Long, renderType: RenderType, clearance: Int): Unit = noteDao.setRenderType(noteId, renderType.ordinal, clearance)

    suspend fun upsert(note: Note, clearance: Int): Result = noteDao.upsert(note.toRoom(), clearance)

    suspend fun update(note: Note, clearance: Int) = when (noteDao.update(note.toRoom(), clearance)) {
        0 -> Result(message = "Could not update note '${note.name}' (does not exist)", type = ResultType.FAILED)
        else -> Result.DEFAULT_SUCCESS
    }

    suspend fun delete(note: Note, clearance: Int): Result = when (noteDao.delete(note.toRoom(), clearance)) {
        0 -> Result(message = "Could not delete note '${note.name}' (does not exist)", type = ResultType.FAILED)
        else -> Result.DEFAULT_SUCCESS
    }

    suspend fun deleteAllSecure(foreach: ((String) -> Unit)? = null) = noteDao.deleteAllSecure(foreach)

    ////////////////////////////////
    // Insecure section;
    // Only crucial information may pass through here.
    ////////////////////////////////

    fun hasSecureNotes(): Flow<Boolean> = noteDao.hasSecureNotes()

    suspend fun hasConflict(name: String): Boolean = noteDao.hasConflict(name)

    suspend fun mayOverride(name: String, clearance: Int): Boolean =
        noteDao.clearanceLevelForName(name)?.let { it <= clearance } ?: true

    suspend fun add(note: Note): Result {
        return try {
            DataResult.from(noteDao.add(note.toRoom()))
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            Result(message = "Could not insert note due to conflict", type = ResultType.FAILED)
        }
    }
}

private fun pagingNote(block: () -> PagingSource<Int, RoomNoteWithCategory>): Flow<PagingData<NoteWithCategory>> =
    Pager(PagingConfig(pageSize = 20)) { block() }.flow.map { page -> page.map { it.toUI() } }


private fun Note.toRoom() = RoomNote(
    noteId = noteId,
    name = name,
    content = content,
    favorite = favorite,
    securityLevel = securityLevel,
    iv = iv,
    date = date,
    renderType = renderType.ordinal,
    categoryKey = categoryKey
)

private fun RoomNote.toUI() = Note(
    noteId = noteId,
    name = name,
    content = content,
    favorite = favorite,
    securityLevel = securityLevel,
    iv = iv,
    date = date,
    renderType = RenderType.values()[renderType],
    categoryKey = categoryKey
)

private fun NoteCategory.toRoom() = RoomNoteCategory(
    categoryId = categoryId,
    categoryName = name,
    color = color,
    favorite = favorite,
    categoryDate = categoryDate
)

private fun RoomNoteCategory.toUI() = NoteCategory(
    categoryId = categoryId,
    name = categoryName,
    color = color,
    favorite = favorite,
    categoryDate = categoryDate
)

private fun NoteWithCategory.toRoom() = RoomNoteWithCategory(note = note.toRoom(), noteCategory = noteCategory.toRoom())

private fun RoomNoteWithCategory.toUI() = NoteWithCategory(note = note.toUI(), noteCategory = noteCategory.toUI())