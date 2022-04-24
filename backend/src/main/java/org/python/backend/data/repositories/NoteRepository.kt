package org.python.backend.data.repositories

import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteWithCategory
import org.python.backend.data.stores.NoteStore
import org.python.backend.security.Securer
import org.python.backend.security.SecurityActor
import org.python.db.CompanionDatabase

class NoteRepository(private val securityActor: SecurityActor, private val noteStore: NoteStore) {
    constructor(securityActor: SecurityActor, companionDatabase: CompanionDatabase) :
            this(securityActor, NoteStore(companionDatabase))

    /** @return All notes in the collection when authorized. All non-secure notes when unauthorized. */
    fun allNotes(): Flow<PagingData<NoteWithCategory>> = securityActor.authenticated.flatMapLatest { authed ->
        if (authed)
            noteStore.getAllNotesWithSecure().map { page -> page.map {
                NoteWithCategory(
                    (secureToUI(it.note) ?: throw IllegalStateException("Could not decrypt note")),
                    it.noteCategory
                )
            } }
        else
            noteStore.getAllNotes()
    }

    fun hasSecureNotes(): Flow<Boolean> = noteStore.hasSecureNotes()

    suspend fun get(id: Long): Note? = noteStore.get(id, securityActor.authenticated.value)?.let { secureToUI(it) }
    suspend fun getWithCategory(id: Long): NoteWithCategory? =
        noteStore.getWithCategory(id, securityActor.authenticated.value)?.let { data ->
                NoteWithCategory(
                    data.note.let { secureToUI(it) ?: throw IllegalStateException("Could not decrypt note") },
                    data.noteCategory
                )
        }
    fun getWithCategoryLive(id: Long): Flow<NoteWithCategory?> =
        noteStore.getWithCategoryLive(id, securityActor.authenticated.value).map { item ->
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
    suspend fun getByName(name: String): Note? = noteStore.getByName(name, securityActor.authenticated.value)?.let { secureToUI(it) }

    /** @return `true` if a conflicting note name was found, `false` otherwise */
    suspend fun hasConflict(name: String): Boolean = noteStore.hasConflict(name)

    /** @return `true` if a note may be overridden, `false` otherwise. If no conflict for given note name exists, returns `true.*/
    suspend fun mayOverride(name: String) = noteStore.mayOverride(name, securityActor.authenticated.value)

    /**
     * Sets note to be a regular or favored note.
     * @param note to set value for.
     * @param favorite new favored status.
     */
    suspend fun setFavorite(note: Note, favorite: Boolean): Unit = noteStore.setFavorite(note, favorite)

    /**
     * Adds a note. If a conflict exists, skips adding proposed item.
     * @return Inserted id on success, `null on conflict.
     */
    suspend fun add(note: Note): Long? = secureToStorage(note)?.let { noteStore.add(it) }

    /** Insert-or-update (upsert) inserts the item if no such item exists, updates otherwise. */
    suspend fun upsert(note: Note): Long? = secureToStorage(note)?.let { noteStore.upsert(it) }

    suspend fun update(oldNote: Note, updatedNote: Note): Boolean = secureUpdate(oldNote, updatedNote)?.let { noteStore.update(it); true } ?: false

    suspend fun delete(items: Collection<Note>): Unit = coroutineScope {
        for (item in items)
            launch {
                secureDelete(item)
                noteStore.delete(item)
            }
    }
    suspend fun delete(note: Note): Unit = noteStore.delete(secureDelete(note))

    suspend fun deleteAllSecure(): Unit = noteStore.deleteAllSecure { name -> secureDelete(name) }


    private fun secureToStorage(note: Note): Note? {
        if (note.secure) {
            val encrypted = Securer.encrypt(data = note.content, alias = note.name) ?: return null
            return note.copy(content = encrypted.dataString(), iv = encrypted.iv)
        }
        return note
    }
    private fun secureUpdate(oldNote: Note, updatedNote: Note): Note? {
        if (oldNote.secure) {
            if (oldNote.name != updatedNote.name) // alias change -> remove keystore alias
                secureDelete(oldNote)
            else if (!updatedNote.secure) // no longer secured -> remove keystore alias
                secureDelete(oldNote)
        }
        return secureToStorage(updatedNote)
    }

    private fun secureToUI(note: Note): Note? {
        if (note.secure) {
            val decrypted = Securer.decrypt(data = note.content, iv = note.iv, alias = note.name) ?: return null
            return note.copy(content = decrypted)
        }
        return note
    }

    private fun secureDelete(note: Note): Note {
        if (note.secure)
            secureDelete(note.name)
        return note
    }
    private fun secureDelete(name: String) {
        Securer.deleteAlias(name)
    }
}