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

    @Query("select * from RoomNoteCategory where name = :name")
    suspend fun getByName(name: String): RoomNoteCategory?

    @Query("update RoomNote set favorite = :favorite where noteId == :noteId")
    suspend fun setFavorite(noteId: Long, favorite: Boolean)
    suspend fun setFavorite(note: RoomNote, favorite: Boolean) = setFavorite(note.noteId, favorite)


    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(item: RoomNoteCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RoomNoteCategory)

    @Update
    suspend fun update(item: RoomNoteCategory)

    @Delete
    suspend fun delete(item: RoomNoteCategory)
}