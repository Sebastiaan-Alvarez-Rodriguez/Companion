package org.python.backend.data.stores

import androidx.paging.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.python.backend.data.datatype.NoteCategory
import org.python.datacomm.Result
import org.python.db.CompanionDatabase
import org.python.db.entities.note.RoomNoteCategory
import org.python.exim.EximUtil

class NoteCategoryStore(database: CompanionDatabase) {
    private val noteCategoryDao = database.noteCategoryDao

    fun getAll(sortColumn: RoomNoteCategory.Companion.SortableField, ascending: Boolean): Flow<PagingData<NoteCategory>> =
        pagingNoteCategory { noteCategoryDao.getAll(sortColumn, ascending) }

    /**
     * Searches note by id.
     * @param id Id to search for.
     * @return Found category if present, null otherwise
     */
    suspend fun get(id: Long): NoteCategory? = noteCategoryDao.get(id)?.toUI()
    suspend fun getAll(): List<NoteCategory> = noteCategoryDao.getAll().map { it.toUI() }

    /**
     * Searches note by name.
     * @param name Exact name of category.
     * @return Found category if present, null otherwise
     */
    suspend fun getByName(name: String): NoteCategory? = noteCategoryDao.getByName(name)?.toUI()

    suspend fun setFavorite(category: NoteCategory, favorite: Boolean) = noteCategoryDao.setFavorite(category.categoryId, favorite)

    fun categoryForNoteLive(noteId: Long, clearance: Int): Flow<NoteCategory> = noteCategoryDao.categoryForNoteLive(noteId, clearance).map { it.toUI() }

    suspend fun updateCategoryForNote(noteId: Long, categoryId: Long) = noteCategoryDao.updateCategoryForNote(noteId, categoryId)

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

    suspend fun addAll(items: Collection<NoteCategory>, mergeStrategy: EximUtil.MergeStrategy): Result = items.map { it.toRoom() }.let {
        when (mergeStrategy) {
            EximUtil.MergeStrategy.OVERRIDE_ON_CONFLICT -> noteCategoryDao.addAllOverriding(it)
            else -> noteCategoryDao.addAllIgnoring(it)
        }
        return Result.DEFAULT_SUCCESS
    }

    suspend fun upsert(category: NoteCategory): Unit = noteCategoryDao.upsert(category.toRoom())

    suspend fun update(category: NoteCategory) = noteCategoryDao.update(category.toRoom())

    suspend fun delete(category: NoteCategory) = noteCategoryDao.delete(category.toRoom())

    suspend fun deleteAll(): Unit = noteCategoryDao.deleteAll()

}

private fun pagingNoteCategory(block: () -> PagingSource<Int, RoomNoteCategory>): Flow<PagingData<NoteCategory>> =
    Pager(PagingConfig(pageSize = 20)) { block() }.flow.map { page -> page.map { it.toUI() } }

private fun NoteCategory.toRoom() = RoomNoteCategory(
    categoryId = categoryId,
    categoryName = name,
    categoryColor = color,
    categoryFavorite = favorite,
    categoryDate = date
)

private fun RoomNoteCategory.toUI() = NoteCategory(
    categoryId = categoryId,
    name = categoryName,
    color = categoryColor,
    favorite = categoryFavorite,
    date = categoryDate
)