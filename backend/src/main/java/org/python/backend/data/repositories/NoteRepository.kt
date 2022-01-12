package org.python.backend.data.repositories

import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.python.backend.data.datatype.Note
import org.python.backend.data.stores.NoteStore
import org.python.backend.security.Securer
import org.python.backend.security.VerificationToken
import org.python.db.CompanionDatabase

class NoteRepository(private val noteStore: NoteStore) {
    constructor(companionDatabase: CompanionDatabase) : this(NoteStore(companionDatabase))

    /**
     * @param token If set, also fetches secure notes from the collection.
     * @return All notes to be found in the collection.
     */
    fun allNotes(token: VerificationToken?) : Flow<PagingData<Note>> {
        return when (token) {
            null -> noteStore.getAllNotes()
            else -> noteStore.getAllNotesWithSecure().map { page -> page.map { secureToUI(it)?: throw IllegalStateException("Could not decrypt notes") } }
        }
    }

    fun hasSecureNotes(): Flow<Boolean> = noteStore.hasSecureNotes()

    /**
     * Rerieves a note by name.
     * @return found note on succes, `null` if no such name exists.
     */
    suspend fun getByName(name: String): Note? = noteStore.getByName(name)

    /**
     * Sets note to be a regular or favored note.
     * @param note to set value for.
     * @param favorite new favored status.
     */
    suspend fun setFavorite(note: Note, favorite: Boolean): Unit = noteStore.setFavorite(note, favorite)


    /**
     * Adds a note. If a conflict exists, skips adding proposed item.
     * @return `true` on success, `false` on conflict.
     */
    suspend fun add(note: Note): Boolean = secureToStorage(note)?.let { noteStore.add(it) } ?: false

    /** Insert-or-update (upsert) inserts the item if no such item exists, updates otherwise. */
    suspend fun upsert(note: Note): Boolean = secureToStorage(note)?.let { noteStore.upsert(it); true } ?: false

    suspend fun update(note: Note): Boolean = secureToStorage(note)?.let { noteStore.update(it); true } ?: false

    suspend fun delete(note: Note): Unit = noteStore.delete(secureDelete(note))


    private fun secureToStorage(note: Note): Note? {
        if (note.secure) {
            val encrypted = Securer.encrypt(data = note.content, alias = note.name) ?: return null
            return note.copy(content = encrypted.dataString(), iv = encrypted.iv)
        }
        return note
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
            Securer.deleteAlias(note.name)
        return note
    }
}