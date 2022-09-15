package org.python.db.daos

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.python.datacomm.DataResult
import org.python.datacomm.Result
import org.python.datacomm.ResultType
import org.python.db.entities.note.RoomNoteCategory
import timber.log.Timber

@Dao
interface NoteCategoryDao {
    @Transaction
    @Query("select * from RoomNoteCategory order by categoryFavorite desc, " +
            "(case when :ascending == 0 then categoryName end) desc, " +
            "(case when :ascending != 0 then categoryName end) asc")
    fun getAll_sortName(ascending: Boolean): PagingSource<Int, RoomNoteCategory>

    @Transaction
    @Query("select * from RoomNoteCategory order by categoryFavorite desc, " +
            "(case when :ascending == 0 then categoryDate end) desc, " +
            "(case when :ascending != 0 then categoryDate end) asc")
    fun getAll_sortDate(ascending: Boolean): PagingSource<Int, RoomNoteCategory>
    fun getAll(sortColumn: RoomNoteCategory.Companion.SortableField, ascending: Boolean): PagingSource<Int, RoomNoteCategory> =
        when (sortColumn) {
            RoomNoteCategory.Companion.SortableField.NAME -> getAll_sortName(ascending)
            RoomNoteCategory.Companion.SortableField.DATE -> getAll_sortDate(ascending)
        }

    @Query("select * from RoomNoteCategory")
    suspend fun getAll(): List<RoomNoteCategory>

    @Query("select * from RoomNoteCategory where categoryId == :id")
    suspend fun get(id: Long): RoomNoteCategory?

    @Query("select * from RoomNoteCategory where categoryName = :name")
    suspend fun getByName(name: String): RoomNoteCategory?

    @Query("select categoryId from RoomNoteCategory where categoryName = :name")
    suspend fun getIdForName(name: String): Long?

    @Query("update RoomNoteCategory set categoryFavorite = :favorite where categoryId == :categoryId")
    suspend fun setFavorite(categoryId: Long, favorite: Boolean)
    suspend fun setFavorite(category: RoomNoteCategory, favorite: Boolean) = setFavorite(category.categoryId, favorite)


    /** Returns live category for a note */
    @Query(
        "select RoomNoteCategory.* from RoomNoteCategory " +
            "join RoomNote on RoomNote.categoryKey = RoomNoteCategory.categoryId " +
            "where RoomNote.noteId == :noteId and RoomNote.securityLevel <= :clearance"
    )
    fun categoryForNoteLive(noteId: Long, clearance: Int): Flow<RoomNoteCategory>

    @Query("update RoomNote set categoryKey = :categoryId where noteId == :noteId")
    suspend fun updateCategoryForNote(noteId: Long, categoryId: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(item: RoomNoteCategory)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addAllIgnoring(items: List<RoomNoteCategory>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllOverriding(items: List<RoomNoteCategory>)

    @Transaction
    suspend fun upsert(item: RoomNoteCategory, useNewId: Boolean = false): Result {
        if (!hasConflict(item.categoryName))
            return DataResult.from(add(item))

        val oldId = getIdForName(item.categoryName)
        if (oldId == null) {
            Timber.e("Unexpected flow: Could not find id for known name")
            return Result(ResultType.FAILED, message = "Could not update note: Could not find old note.")
        }
        val newId = if (useNewId) item.categoryId else oldId
        if (useNewId)
            updateId(oldId = oldId, newId = newId)
        val updated = update(item.copy(categoryId = newId)) == 0

        return if (updated) {
            Timber.e("upsert item - failure (update): ${item.categoryName}")
            Result(ResultType.FAILED, message = "Could not update note: Could not find old note.")
        } else {
            Timber.e("upsert item - success: ${item.categoryName}, oldId=$oldId, newId=$newId")
            DataResult.from(newId)
        }
    }

    @Transaction
    suspend fun upsertAll(items: List<RoomNoteCategory>, useNewIds: Boolean = false) = items.forEach { item -> upsert(item, useNewIds) }
    @Update
    suspend fun update(item: RoomNoteCategory): Int

    @Transaction
    suspend fun updateId(oldId: Long, newId: Long) {
        __updateId(oldId = oldId, newId = newId)
        __updateKeys(oldKey= oldId, newValue = newId)
    }

    @Transaction
    suspend fun delete(item: RoomNoteCategory) {
        if (item.categoryId == RoomNoteCategory.DEFAULT.categoryId)
            return
        resetCategory(item.categoryId, RoomNoteCategory.DEFAULT.categoryId)
        __delete(item.categoryId)
    }

    @Transaction
    suspend fun deleteAll() {
        resetCategoryAll(RoomNoteCategory.DEFAULT.categoryId)
        __deleteAll(RoomNoteCategory.DEFAULT.categoryId)
    }

    @Query("select exists(select 1 from RoomNoteCategory where categoryName == :name)")
    suspend fun hasConflict(name: String): Boolean

    @Query("update RoomNote set categoryKey = :newCategoryId where categoryKey == :oldCategoryId")
    fun resetCategory(oldCategoryId: Long, newCategoryId: Long)

    @Query("update RoomNote set categoryKey = :defaultCategoryId")
    fun resetCategoryAll(defaultCategoryId: Long)

    @Query("update RoomNoteCategory set categoryId=:newId where categoryId=:oldId")
    suspend fun __updateId(oldId: Long, newId: Long)

    @Query("update RoomNote set categoryKey=:newValue where categoryKey=:oldKey")
    suspend fun __updateKeys(oldKey: Long, newValue: Long)

    @Query("delete from RoomNoteCategory where categoryId == :categoryId")
    fun __delete(categoryId: Long)

    @Query("delete from RoomNoteCategory where categoryId != :defaultCategoryId")
    fun __deleteAll(defaultCategoryId: Long)
}