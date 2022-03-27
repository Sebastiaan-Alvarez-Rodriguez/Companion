package org.python.backend.data.stores

import androidx.paging.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.python.backend.data.datatype.NoteCategory
import org.python.db.CompanionDatabase
import org.python.db.entities.note.RoomNoteCategory

class NoteCategoryStore(database: CompanionDatabase) {
    private val noteCategoryDao = database.noteCategoryDao

    fun getAll(): Flow<PagingData<NoteCategory>> = pagingNoteCategory { noteCategoryDao.getAll() }

    /**
     * Searches note by id.
     * @param id Id to search for.
     * @return Found category if present, null otherwise
     */
    suspend fun get(id: Long): NoteCategory? = noteCategoryDao.get(id)?.toUI()

    /**
     * Searches note by name.
     * @param name Exact name of category.
     * @return Found category if present, null otherwise
     */
    suspend fun getByName(name: String): NoteCategory? = noteCategoryDao.getByName(name)?.toUI()

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
}

private fun pagingNoteCategory(block: () -> PagingSource<Int, RoomNoteCategory>): Flow<PagingData<NoteCategory>> =
    Pager(PagingConfig(pageSize = 20)) { block() }.flow.map { page -> page.map { it.toUI() } }

private fun NoteCategory.toRoom() = RoomNoteCategory(categoryId = categoryId, categoryName = name, color = color, favorite = favorite)

private fun RoomNoteCategory.toUI() = NoteCategory(categoryId = categoryId, name = categoryName, color = color, favorite = favorite)