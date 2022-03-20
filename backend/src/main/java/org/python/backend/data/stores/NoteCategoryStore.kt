package org.python.backend.data.stores

import androidx.paging.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.python.backend.data.datatype.NoteCategory
import org.python.backend.data.datatype.NoteCategoryCategory
import org.python.backend.data.datatype.NoteCategory
import org.python.db.CompanionDatabase
import org.python.db.entities.category.RoomNoteCategory
import org.python.db.entities.category.RoomNoteCategoryCategory
import org.python.db.entities.category.RoomNoteCategory
import org.python.db.entities.note.RoomNoteCategory

class NoteCategoryStore(database: CompanionDatabase) {
    private val noteCategoryDao = database.noteCategoryDao

    fun getAllNoteCategories(): Flow<PagingData<NoteCategory>> = pagingNoteCategory { noteCategoryDao.getAll() }

    /**
     * Searches note by id.
     * @param id Id to search for.
     * @param secure If set, searches secure notes and insecure notes. Otherwise, only searches insecure notes.
     * @return Found note if present, null otherwise
     */
    suspend fun get(id: Long, secure: Boolean = false): NoteCategory? = noteCategoryDao.get(id, secure)?.toUI()

    /**
     * Searches note by name.
     * @param name Exact name of category.
     * @param secure If set, searches secure notes and insecure notes. Otherwise, only searches insecure notes.
     * @return Found note if present, null otherwise
     */
    suspend fun getByName(name: String, secure: Boolean = false): NoteCategory? = noteCategoryDao.getByName(name, secure)?.toUI()

    suspend fun setFavorite(category: NoteCategory, favorite: Boolean) = noteCategoryDao.setFavorite(category.categoryId, favorite)
    suspend fun add(category: NoteCategory): Boolean {
        return try {
            noteCategoryDao.add(category.toRoom())
            true
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun upsert(category: NoteCategory): Unit = noteCategoryDao.upsert(category.toRoom())

    suspend fun update(category: NoteCategory) = noteCategoryDao.update(category.toRoom())

    suspend fun delete(category: NoteCategory) = noteCategoryDao.delete(category.toRoom())

    suspend fun deleteAllSecure(foreach: ((String) -> Unit)? = null) = noteCategoryDao.deleteAllSecure(foreach)
}

private fun pagingNoteCategory(block: () -> PagingSource<Int, RoomNoteCategory>): Flow<PagingData<NoteCategory>> =
    Pager(PagingConfig(pageSize = 20)) { block() }.flow.map { page -> page.map { it.toUI() } }

private fun NoteCategory.toRoom() = RoomNoteCategory(categoryId = categoryId, name = name, color = color, favorite = favorite)

private fun RoomNoteCategory.toUI() = NoteCategory(categoryId = categoryId, name = name, color = color, favorite = favorite)