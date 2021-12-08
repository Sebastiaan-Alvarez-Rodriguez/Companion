package org.python.db.note.daos

import androidx.paging.PagingSource
import androidx.room.*
import org.python.db.note.entities.RoomNote

@Dao
interface NoteDao {
    @Query("SELECT * from RoomNote")
    fun getAll(): PagingSource<Int, RoomNote>

    @Query("SELECT * from RoomNote where name = :name")
    suspend fun getByName(name: String): RoomNote?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(item: RoomNote)

    @Update
    suspend fun update(item: RoomNote)

    @Delete
    suspend fun delete(item: RoomNote)
}