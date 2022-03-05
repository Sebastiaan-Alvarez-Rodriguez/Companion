package org.python.db.daos

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.python.db.entities.note.RoomNote
import org.python.db.entities.note.RoomNoteWithCategory

@Dao
interface NoteDao {
    @Query(
        "select * from RoomNote " +
            "left join RoomNoteCategory on RoomNote.categoryId = RoomNoteCategory.id " +
            "where secure == 0"
    )
    fun getAll(): PagingSource<Int, RoomNoteWithCategory>

    @Query(
        "select * from RoomNote " +
        "left join RoomNoteCategory on RoomNote.categoryId = RoomNoteCategory.id "
    )
    fun getAllWithSecure(): PagingSource<Int, RoomNoteWithCategory>

    @Query("select name from RoomNote where secure > 0")
    suspend fun getSecureNames(): List<String>

    @Query("select * from RoomNote where secure <= :secure and id == :id")
    suspend fun get(id: Long, secure: Boolean = false): RoomNote?

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

    @Query("delete from RoomNote where secure > 0")
    suspend fun deleteAllSecure()

    @Transaction
    suspend fun deleteAllSecure(foreach: ((String) -> Unit)?) {
        getSecureNames().forEach { foreach?.invoke(it) }
        deleteAllSecure()
    }
}