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

    @Query("select name from RoomNote where secure > 0")
    fun getSecureNames(): List<String>

    @Query("select * from RoomNote where secure <= :secure and name = :name")
    suspend fun getByName(name: String, secure: Boolean = false): RoomNote?

    @Query("update RoomNote set favorite = :favorite where id == :noteId")
    suspend fun setFavorite(noteId: Long, favorite: Boolean)
    suspend fun setFavorite(note: RoomNote, favorite: Boolean) = setFavorite(note.id, favorite)

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

    @Query("delete from RoomNote where secure > 0") // TODO: Do delete
    suspend fun deleteAllSecure()

    @Transaction
    suspend fun deleteAllSecure(foreach: ((String) -> Unit)?) {
        getSecureNames().forEach { foreach?.invoke(it) }
        deleteAllSecure()
    }
}