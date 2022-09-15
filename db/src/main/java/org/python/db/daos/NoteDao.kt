package org.python.db.daos

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.python.datacomm.DataResult
import org.python.datacomm.Result
import org.python.datacomm.ResultType
import org.python.db.entities.note.RoomNote
import org.python.db.entities.note.RoomNoteWithCategory
import timber.log.Timber

@Dao
interface NoteDao {
    @Transaction
    @Query("select * from RoomNote " +
            "join RoomNoteCategory on RoomNote.categoryKey = RoomNoteCategory.categoryId " +
            "where securityLevel <= :clearance " +
            "order by favorite desc, " +
                "(case when :ascending == 0 then name end) desc, " +
                "(case when :ascending != 0 then name end) asc")
    fun getAll_sortName(clearance: Int, ascending: Boolean): PagingSource<Int, RoomNoteWithCategory>

    @Transaction
    @Query("select * from RoomNote " +
            "join RoomNoteCategory on RoomNote.categoryKey = RoomNoteCategory.categoryId " +
            "where securityLevel <= :clearance " +
            "order by favorite desc, " +
            "(case when :ascending == 0 then date end) desc, " +
            "(case when :ascending != 0 then date end) asc")
    fun getAll_sortDate(clearance: Int, ascending: Boolean): PagingSource<Int, RoomNoteWithCategory>

    @Transaction
    @Query("select * from RoomNote " +
            "join RoomNoteCategory on RoomNote.categoryKey = RoomNoteCategory.categoryId " +
            "where securityLevel <= :clearance " +
            "order by favorite desc, " +
            "(case when :ascending == 0 then categoryName end) desc, " +
            "(case when :ascending != 0 then categoryName end) asc")
    fun getAll_sortCategoryName(clearance: Int, ascending: Boolean): PagingSource<Int, RoomNoteWithCategory>

    @Transaction
    @Query("select * from RoomNote " +
            "join RoomNoteCategory on RoomNote.categoryKey = RoomNoteCategory.categoryId " +
            "where securityLevel <= :clearance " +
            "order by favorite desc, " +
            "(case when :ascending == 0 then securityLevel end) desc, " +
            "(case when :ascending != 0 then securityLevel end) asc")
    fun getAll_sortSecurityLevel(clearance: Int, ascending: Boolean): PagingSource<Int, RoomNoteWithCategory>

    ////////////////////////////////
    // Secure section;
    // All functions here have user verification checks built-in.
    ////////////////////////////////
    fun getAll(clearance: Int, sortColumn: RoomNoteWithCategory.Companion.SortableField, ascending: Boolean): PagingSource<Int, RoomNoteWithCategory> =
        when(sortColumn) {
            RoomNoteWithCategory.Companion.SortableField.NAME -> getAll_sortName(clearance, ascending)
            RoomNoteWithCategory.Companion.SortableField.DATE -> getAll_sortDate(clearance, ascending)
            RoomNoteWithCategory.Companion.SortableField.CATEGORYNAME -> getAll_sortCategoryName(clearance, ascending)
            RoomNoteWithCategory.Companion.SortableField.SECURITYLEVEL -> getAll_sortSecurityLevel(clearance, ascending)
        }

    @Query("select * from RoomNote where noteId == :id and securityLevel <= :clearance")
    suspend fun get(id: Long, clearance: Int): RoomNote?

    @Query("select * from RoomNote where securityLevel <= :clearance")
    suspend fun getAll(clearance: Int): List<RoomNote>

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
    @Transaction
    suspend fun setFavorite(note: RoomNote, favorite: Boolean, clearance: Int) = setFavorite(note.noteId, favorite, clearance)

    @Query("update RoomNote set renderType = :renderType where noteId = :noteId and securityLevel <= :clearance")
    suspend fun setRenderType(noteId: Long, renderType: Int, clearance: Int)

    @Transaction
    suspend fun upsert(item: RoomNote, clearance: Int): Result {
        if (!hasConflict(item.name)) {
            Timber.e("upsert item - add: ${item.name}")
            return DataResult.from(add(item))
        }
        if (clearanceLevelForName(item.name)!! > clearance) {
            Timber.e("upsert item - failure (clearance insufficient): ${item.name} (have $clearance, need ${clearanceLevelForName(item.name)!!})")
            return Result(ResultType.FAILED,  message = "Could not update note: Clearance level insufficient.")
        }

        val id = getIdForName(item.name, clearance)
        if (id == null) {
            Timber.e("upsert item - failure (clearance insufficient): ${item.name}")
            return Result(ResultType.FAILED, message = "Could not update note: Could not find old note.")
        }

        return when (update(item.copy(noteId = id), clearance)) {
            0 -> {
                Timber.e("upsert item - failure (update): ${item.name}")
                Result(ResultType.FAILED,  message = "Could not update note: Could not find old note.")
            }
            else -> {
                Timber.e("upsert item - success: ${item.name}, ${id}, ${item.securityLevel}")
                DataResult.from(id)
            }
        }
    }
    @Transaction
    suspend fun upsertAll(items: List<RoomNote>, clearance: Int) {
        items.forEach { upsert(it, clearance) }
    }

    /** Updates existing note. Returns number of changed rows. Can be either 0 (no such note) or 1 (updated entry). */
    @Query("update RoomNote set name = :name, content = :content, favorite = :favorite, securityLevel = :securityLevel, iv = :iv, categoryKey = :categoryKey, renderType = :renderType where noteId = :noteId and securityLevel <= :clearance")
    suspend fun update(noteId: Long, name: String, content: String, favorite: Boolean, securityLevel: Int, iv: ByteArray, categoryKey: Long, renderType: Int, clearance: Int): Int
    @Transaction
    suspend fun update(item: RoomNote, clearance: Int) = update(item.noteId, item.name, item.content, item.favorite, item.securityLevel, item.iv, item.categoryKey, item.renderType, clearance)

    /** Deletes note. Returns number of changed rows. Can be either 0 (no such note) or 1 (deleted entry). */
    @Query("delete from RoomNote where noteId = :noteId and securityLevel <= :clearance")
    suspend fun delete(noteId: Long, clearance: Int): Int
    @Transaction
    suspend fun delete(item: RoomNote, clearance: Int) = delete(item.noteId, clearance)

    ////////////////////////////////
    // Insecure section;
    // Only crucial information may pass through here.
    ////////////////////////////////

    @Query("select exists(select 1 from RoomNote where securityLevel != 0)")
    fun hasSecureNotes(): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(item: RoomNote): Long

    @Insert
    suspend fun addAll(items: List<RoomNote>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addAllIgnoring(items: List<RoomNote>)

    @Query("select exists(select 1 from RoomNote where name == :name)")
    suspend fun hasConflict(name: String): Boolean

    @Query("select securityLevel from RoomNote where name = :name")
    suspend fun clearanceLevelForName(name: String): Int?

    @Query("delete from RoomNote")
    suspend fun deleteAll()

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