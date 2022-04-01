package org.python.db.daos

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.python.db.entities.note.RoomNote
import org.python.db.entities.note.RoomNoteWithCategory

@Dao
interface NoteDao {
    @Transaction
    @Query(
        "select * from RoomNote " +
            "join RoomNoteCategory on RoomNote.categoryKey = RoomNoteCategory.categoryId " +
            "where secure == 0"
    )
    fun getAll(): PagingSource<Int, RoomNoteWithCategory>

    @Transaction
    @Query(
        "select * from RoomNote " +
        "join RoomNoteCategory on RoomNote.categoryKey = RoomNoteCategory.categoryId"
    )
    fun getAllWithSecure(): PagingSource<Int, RoomNoteWithCategory>

    @Query("select name from RoomNote where secure > 0")
    suspend fun getSecureNames(): List<String>

    @Query("select * from RoomNote where noteId == :id and secure <= :secure")
    suspend fun get(id: Long, secure: Boolean = false): RoomNote?

    @Transaction
    @Query("select * from RoomNote " +
            "join RoomNoteCategory on RoomNote.categoryKey = RoomNoteCategory.categoryId " +
            "where noteId == :id and secure <= :secure")
    suspend fun getWithCategory(id: Long, secure: Boolean = false): RoomNoteWithCategory?

    @Transaction
    @Query("select * from RoomNote " +
            "join RoomNoteCategory on RoomNote.categoryKey = RoomNoteCategory.categoryId " +
            "where noteId == :id and secure <= :secure")
    fun getWithCategoryLive(id: Long, secure: Boolean = false): Flow<RoomNoteWithCategory?>

    @Query("select * from RoomNote where name = :name and secure <= :secure")
    suspend fun getByName(name: String, secure: Boolean = false): RoomNote?

    @Query("select exists(select 1 from RoomNote where name = :name)")
    suspend fun hasConflict(name: String): Boolean

    @Query("update RoomNote set favorite = :favorite where noteId == :noteId")
    suspend fun setFavorite(noteId: Long, favorite: Boolean)
    suspend fun setFavorite(note: RoomNote, favorite: Boolean) = setFavorite(note.noteId, favorite)

    @Query("update RoomNote set categoryKey = :categoryId where noteId == :noteId")
    suspend fun updateCategoryForNote(noteId: Long, categoryId: Long)

    @Query("select exists(select 1 from RoomNote where secure != 0)")
    fun hasSecureNotes(): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(item: RoomNote): Long?

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