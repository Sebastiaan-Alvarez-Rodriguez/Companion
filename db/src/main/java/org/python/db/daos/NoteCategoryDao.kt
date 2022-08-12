package org.python.db.daos

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.python.db.entities.note.RoomNoteCategory

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RoomNoteCategory)

    @Update
    suspend fun update(item: RoomNoteCategory)

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

    @Query("update RoomNote set categoryKey = :newCategoryId where categoryKey == :oldCategoryId")
    fun resetCategory(oldCategoryId: Long, newCategoryId: Long)

    @Query("update RoomNote set categoryKey = :defaultCategoryId")
    fun resetCategoryAll(defaultCategoryId: Long)

//    @Query("insert into RoomNoteCategory ")
    @Query("delete from RoomNoteCategory where categoryId == :categoryId")
    fun __delete(categoryId: Long)

    @Query("delete from RoomNoteCategory where categoryId != :defaultCategoryId")
    fun __deleteAll(defaultCategoryId: Long)
}