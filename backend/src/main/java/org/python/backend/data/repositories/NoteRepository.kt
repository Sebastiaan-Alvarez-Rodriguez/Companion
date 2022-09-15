package org.python.backend.data.repositories

import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteWithCategory
import org.python.backend.data.datatype.RenderType
import org.python.backend.data.stores.NoteStore
import org.python.datacomm.DataResult
import org.python.datacomm.Result
import org.python.db.CompanionDatabase
import org.python.db.entities.note.RoomNoteWithCategory
import org.python.exim.EximUtil
import org.python.security.Securer
import org.python.security.SecurityActor

class NoteRepository(private val securityActor: SecurityActor, private val noteStore: NoteStore) {
    constructor(securityActor: SecurityActor, companionDatabase: CompanionDatabase) : this(securityActor, NoteStore(companionDatabase))

    ////////////////////////////////
    // Secure section;
    // All functions here have user verification checks built-in.
    ////////////////////////////////

    /** @return All notes in the collection (for which the user is authorized), sorted on given column. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun allNotes(sortColumn: RoomNoteWithCategory.Companion.SortableField, ascending: Boolean): Flow<PagingData<NoteWithCategory>> =
        securityActor.clearance.flatMapLatest { clearance ->
            noteStore.getAllNotes(clearance, sortColumn, ascending).map { page -> page.map {
                NoteWithCategory((secureToUI(it.note) ?: throw IllegalStateException("Could not decrypt note")), it.noteCategory)
            } }
        }

    suspend fun get(id: Long): Note? = noteStore.get(id, securityActor.clearance.value)?.let { secureToUI(it) }
    suspend fun getAll(): List<Note> = noteStore.getAll(securityActor.clearance.value).map { secureToUI(it) ?: throw IllegalStateException("Could not decrypt note") }

    suspend fun getWithCategory(id: Long): NoteWithCategory? =
        noteStore.getWithCategory(id, securityActor.clearance.value)?.let { data ->
            NoteWithCategory(
                data.note.let { secureToUI(it) ?: throw IllegalStateException("Could not decrypt note") },
                data.noteCategory
            )
        }
    fun getWithCategoryLive(id: Long): Flow<NoteWithCategory?> =
        noteStore.getWithCategoryLive(id, securityActor.clearance.value).map { item ->
            item?.let { data ->
                NoteWithCategory(
                    data.note.let { secureToUI(it) ?: throw IllegalStateException("Could not decrypt note") },
                    data.noteCategory
                )
            }
        }

    /**
     * Rerieves a note by name.
     * @return found note on success, `null` if no such name exists.
     */
    suspend fun getByName(name: String): Note? = noteStore.getByName(name, securityActor.clearance.value)?.let { secureToUI(it) }

    /**
     * Sets note to be a regular or favored note.
     * @param note to set value for.
     * @param favorite new favored status.
     */
    suspend fun setFavorite(note: Note, favorite: Boolean): Unit = noteStore.setFavorite(note, favorite, securityActor.clearance.value)

    suspend fun setRenderType(noteId: Long, renderType: RenderType): Unit = noteStore.setRenderType(noteId, renderType, securityActor.clearance.value)

    /** Inserts-or-updates [Note]. [Result] contains [Long], the updated id, on success. */
    suspend fun upsert(note: Note): Result =
        secureToStorage(note).pipeData<Note> { it.let { noteStore.upsert(it, securityActor.clearance.value) } }

    /** Updates [Note]. [Result] contains [Long], the updated id, on success. */
    suspend fun update(oldNote: Note, updatedNote: Note): Result =
        secureToStorage(updatedNote)
            .pipeData<Note> { it.let { note -> noteStore.update(note, securityActor.clearance.value) } }
            .pipe { secureUpdate(oldNote, updatedNote) }
            .pipe { DataResult.from(updatedNote.noteId) }

    suspend fun delete(items: Collection<Note>): Unit = coroutineScope {
        for (item in items)
            launch {
                noteStore.delete(item, securityActor.clearance.value).apply { secureDelete(item) }
            }
    }
    suspend fun delete(note: Note): Result = noteStore.delete(note, securityActor.clearance.value).apply { secureDelete(note) }
    suspend fun deleteAll(): Unit = noteStore.deleteAll()

    suspend fun deleteAllSecure(): Unit = noteStore.deleteAllSecure { name -> secureDelete(name) }


    ////////////////////////////////
    // Insecure section;
    // Only crucial information may pass through here.
    ////////////////////////////////

    fun hasSecureNotes(): Flow<Boolean> = noteStore.hasSecureNotes()

    /** @return `true` if a conflicting note name was found, `false` otherwise */
    suspend fun hasConflict(name: String): Boolean = noteStore.hasConflict(name)

    /** @return `true` if a note may be overridden, `false` otherwise. If no conflict for given note name exists, returns `true.*/
    suspend fun mayOverride(name: String) = noteStore.mayOverride(name, securityActor.clearance.value)


    /**
     * Adds a note. If a conflict exists, skips adding proposed item.
     * @return [DataResult]<[Long]> Inserted id on success, failure result otherwise.
     */
    suspend fun add(note: Note): Result = secureToStorage(note).pipeData<Note> { noteStore.add(it) }

    /**
     * Adds all notes, using given merge strategy.
     * @return [Result] success.
     */
    suspend fun addAll(items: Collection<Note>, mergeStrategy: EximUtil.MergeStrategy): Result =
        noteStore.addAll(items.map { secureToStorage(it).toDataResult<Note>().data }, mergeStrategy, securityActor.clearance.value)

    ////////////////////////////////
    // Utility section;
    ////////////////////////////////

    /** Returns a [DataResult]<[Note]> on success, failure result otherwise */
    private fun secureToStorage(note: Note): Result = Result.fromObject(failMessage = "Could not encrypt note '${note.name}'") {
        when {
            note.securityLevel > 0 -> Securer.encrypt(data = note.content, alias = note.name)?.let { note.copy(content = it.dataString(), iv = it.iv) }
            else -> note
        }
    }

    private fun secureUpdate(oldNote: Note, updatedNote: Note): Result {
        when {
            oldNote.securityLevel > 0 && oldNote.name != updatedNote.name -> secureDelete(oldNote) // alias change -> remove keystore alias
            oldNote.securityLevel > 0 && updatedNote.securityLevel == 0 -> secureDelete(oldNote) // no longer secured -> remove keystore alias
        }
        return Result.DEFAULT_SUCCESS
    }

    private fun secureToUI(note: Note): Note? {
        if (note.securityLevel > 0) {
            val decrypted = Securer.decrypt(data = note.content, iv = note.iv, alias = note.name) ?: return null
            return note.copy(content = decrypted)
        }
        return note
    }

    private fun secureDelete(note: Note): Note {
        if (note.securityLevel > 0)
            secureDelete(note.name)
        return note
    }
    private fun secureDelete(name: String) = Securer.deleteAlias(name)
}