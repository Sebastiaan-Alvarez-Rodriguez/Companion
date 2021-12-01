package org.python.backend.note.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import org.python.backend.note.entities.RoomNote

@Dao
interface NoteDao {
    @Query("SELECT * from RoomNote")
    fun getAll(): LiveData<List<RoomNote>>

    @Query("SELECT * from RoomNote where name = :name")
    fun getByName(name: String): RoomNote

    @Insert
    suspend fun insert(item: RoomNote)

    @Update
    suspend fun update(item: RoomNote)

    @Delete
    suspend fun delete(item: RoomNote)
}