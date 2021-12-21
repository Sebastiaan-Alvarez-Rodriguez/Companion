package org.python.db.daos

import androidx.paging.PagingSource
import androidx.room.*
import org.python.db.entities.RoomAnniversary

@Dao
interface AnniversaryDao {
    @Query("SELECT * from RoomAnniversary")
    fun getAll(): PagingSource<Int, RoomAnniversary>

    @Query("SELECT * from RoomAnniversary where name = :name")
    suspend fun getByName(name: String): RoomAnniversary?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(item: RoomAnniversary)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: RoomAnniversary)

    @Update
    suspend fun update(item: RoomAnniversary)

    @Delete
    suspend fun delete(item: RoomAnniversary)
}