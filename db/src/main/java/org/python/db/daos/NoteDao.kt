package org.python.db.daos

import androidx.paging.PagingSource
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import org.python.datacomm.DataResult
import org.python.datacomm.ResultType
import org.python.db.entities.note.RoomNote
import org.python.db.entities.note.RoomNoteCategory
import org.python.db.entities.note.RoomNoteWithCategory

@Dao
interface NoteDao {

    ////////////////////////////////
    // Secure section;
    // All functions here have user verification checks built-in.
    ////////////////////////////////
    fun getAll(clearance: Int, sortColumn: RoomNoteWithCategory.Companion.SortableField, ascending: Boolean): PagingSource<Int, RoomNoteWithCategory> {
        val columnName = when(sortColumn) {
            RoomNoteWithCategory.Companion.SortableField.NAME -> "name"
            RoomNoteWithCategory.Companion.SortableField.SECURITYLEVEL -> "securityLevel"
            RoomNoteWithCategory.Companion.SortableField.CATEGORYNAME -> "categoryName"
        }
        return getAllSortedNotes(
            SimpleSQLiteQuery("select * from RoomNote " +
                    "join RoomNoteCategory on RoomNote.categoryKey = RoomNoteCategory.categoryId " +
                    "where securityLevel <= $clearance " +
                    "order by favorite desc, $columnName ${if (ascending) "asc" else "desc"}"
            )
        )
    }

    @Query("select * from RoomNote where noteId == :id and securityLevel <= :clearance")
    suspend fun get(id: Long, clearance: Int): RoomNote?

    @Transaction
    @Query("select * from RoomNote " +
            "join RoomNoteCategory on RoomNote.categoryKey = RoomNoteCategory.categoryId " +
            "where noteId == :id and securityLevel <= :clearance"
    )
    suspend fun getWithCategory(id: Long, clearance: Int): RoomNoteWithCategory?

    @Transaction
    @Query("select * from RoomNote " +
            "join RoomNoteCategory on RoomNote.categoryKey = RoomNoteCategory.categoryId " +
            "where noteId == :id and securityLevel <= :clearance"
    )
    fun getWithCategoryLive(id: Long, clearance: Int): Flow<RoomNoteWithCategory?>

    @Query("select * from RoomNote where name = :name and securityLevel <= :clearance")
    suspend fun getByName(name: String, clearance: Int): RoomNote?

    @Query("select noteId from RoomNote where name = :name and securityLevel <= :clearance")
    suspend fun getIdForName(name: String, clearance: Int): Long?

    @Query("update RoomNote set favorite = :favorite where noteId = :noteId and securityLevel <= :clearance")
    suspend fun setFavorite(noteId: Long, favorite: Boolean, clearance: Int)
    suspend fun setFavorite(note: RoomNote, favorite: Boolean, clearance: Int) = setFavorite(note.noteId, favorite, clearance)

    @Transaction
    suspend fun upsert(item: RoomNote, clearance: Int): org.python.datacomm.Result {
        if (!hasConflict(item.name))
            return DataResult.from(add(item))

        if (clearanceLevelForName(item.name)!! > clearance)
            return org.python.datacomm.Result(ResultType.FAILED,  message = "Could not update note: Clearance level insufficient.")

        val id = getIdForName(item.name, clearance) ?: return org.python.datacomm.Result(ResultType.FAILED,  message = "Could not update note: Could not find old note.")
        return when (update(item.copy(noteId = id), clearance)) {
            0 -> org.python.datacomm.Result(ResultType.FAILED,  message = "Could not update note: Could not find old note.")
            else -> DataResult.from(id)
        }
    }

    /** Updates existing note. Returns number of changed rows. Can be either 0 (no such note) or 1 (updated entry). */
    @Query("update RoomNote set name = :name, content = :content, favorite = :favorite, securityLevel = :securityLevel, iv = :iv, categoryKey = :categoryKey where noteId = :noteId and securityLevel <= :clearance")
    suspend fun update(noteId: Long, name: String, content: String, favorite: Boolean, securityLevel: Int, iv: ByteArray, categoryKey: Long, clearance: Int): Int
    suspend fun update(item: RoomNote, clearance: Int) = update(item.noteId, item.name, item.content, item.favorite, item.securityLevel, item.iv, item.categoryKey, clearance)

    /** Deletes note. Returns number of changed rows. Can be either 0 (no such note) or 1 (deleted entry). */
    @Query("delete from RoomNote where noteId = :noteId and securityLevel <= :clearance")
    suspend fun delete(noteId: Long, clearance: Int): Int
    suspend fun delete(item: RoomNote, clearance: Int) = delete(item.noteId, clearance)

    ////////////////////////////////
    // Insecure section;
    // Only crucial information may pass through here.
    ////////////////////////////////

    // TODO: Hide highly insecure database interface to module-level.
    @RawQuery(observedEntities = [RoomNote::class, RoomNoteCategory::class])
    fun getAllSortedNotes(query: SupportSQLiteQuery): PagingSource<Int, RoomNoteWithCategory>

    @Query("select exists(select 1 from RoomNote where securityLevel != 0)")
    fun hasSecureNotes(): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(item: RoomNote): Long

    @Query("select exists(select 1 from RoomNote where name == :name)")
    suspend fun hasConflict(name: String): Boolean

    @Query("select securityLevel from RoomNote where name = :name")
    suspend fun clearanceLevelForName(name: String): Int?


    @Query("delete from RoomNote where securityLevel > 0")
    suspend fun deleteAllSecure()

    @Transaction
    suspend fun deleteAllSecure(foreach: ((String) -> Unit)?) {
        getSecureNames().forEach { foreach?.invoke(it) }
        deleteAllSecure()
    }

    @Query("select name from RoomNote where securityLevel > 0")
    suspend fun getSecureNames(): List<String>

}