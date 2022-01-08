package org.python.db.daos

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.python.db.entities.RoomNote

@Dao
interface NoteDao {
    @Query("select * from RoomNote where secure == 0")
    fun getAll(): PagingSource<Int, RoomNote>

    @Query("select * from RoomNote")
    fun getAllWithSecure(): PagingSource<Int, RoomNote>

    // TODO: Allow searching secure notes when token is present and valid.
    @Query("select * from RoomNote where secure == 0 and name = :name")
    suspend fun getByName(name: String): RoomNote?

    @Query("select exists(select 1 from RoomNote where secure != 0)")
    fun hasSecureNotes(): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(item: RoomNote)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RoomNote)

    @Update
    suspend fun update(item: RoomNote)

    @Delete
    suspend fun delete(item: RoomNote)
}