package org.python.db.daos

import androidx.paging.PagingSource
import androidx.room.*
import org.python.db.entities.RoomNote

@Dao
interface NoteDao {
    @Query("SELECT * from RoomNote")
    fun getAll(): PagingSource<Int, RoomNote>

    @Query("SELECT * from RoomNote where name = :name")
    suspend fun getByName(name: String): RoomNote?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(item: RoomNote)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RoomNote)

    @Update
    suspend fun update(item: RoomNote)

    @Delete
    suspend fun delete(item: RoomNote)
}