package org.python.db.daos

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.python.db.entities.note.RoomNoteCategory

@Dao
interface NoteCategoryDao {
    @Query("select * from RoomNoteCategory")
    fun getAll(): PagingSource<Int, RoomNoteCategory>

    @Query("select * from RoomNoteCategory where categoryId == :id")
    suspend fun get(id: Long): RoomNoteCategory?

    @Query("select * from RoomNoteCategory where categoryName = :name")
    suspend fun getByName(name: String): RoomNoteCategory?

    @Query("update RoomNoteCategory set favorite = :favorite where categoryId == :categoryId")
    suspend fun setFavorite(categoryId: Long, favorite: Boolean)
    suspend fun setFavorite(category: RoomNoteCategory, favorite: Boolean) = setFavorite(category.categoryId, favorite)


    /** Returns the live category for a note */
    @Query("select * from RoomNoteCategory " +
            "join RoomNote on RoomNote.categoryKey = RoomNoteCategory.categoryId " +
            "where noteId == :noteId and secure <= :secure")
    fun categoryForNoteLive(noteId: Long, secure: Boolean = false): Flow<RoomNoteCategory>

    @Query("update RoomNote set categoryKey = :categoryId where noteId == :noteId")
    suspend fun updateCategoryForNote(noteId: Long, categoryId: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(item: RoomNoteCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RoomNoteCategory)

    @Update
    suspend fun update(item: RoomNoteCategory)

    @Transaction
    suspend fun delete(item: RoomNoteCategory) {
        resetCategory(item.categoryId, RoomNoteCategory.DEFAULT.categoryId)
        __delete(item.categoryId)
    }

    @Query("delete from RoomNoteCategory where categoryId == :categoryId")
    fun __delete(categoryId: Long)

    @Query("update RoomNote set categoryKey = :newCategoryId where categoryKey == :oldCategoryId")
    fun resetCategory(oldCategoryId: Long, newCategoryId: Long)
}