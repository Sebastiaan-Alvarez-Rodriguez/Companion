package org.python.db.daos

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.python.db.entities.note.RoomNote
import org.python.db.entities.note.RoomNoteCategory
import org.python.db.entities.note.RoomNoteWithCategory

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


    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(item: RoomNoteCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RoomNoteCategory)

    @Update
    suspend fun update(item: RoomNoteCategory)

    @Delete
    suspend fun delete(item: RoomNoteCategory)
}