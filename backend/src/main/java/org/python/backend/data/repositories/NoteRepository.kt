package org.python.backend.data.repositories

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
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
            else -> noteStore.getAllNotesWithSecure()
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
    suspend fun add(note: Note): Boolean = noteStore.add(secureToStorage(note))

    /** Insert-or-update (upsert) inserts the item if no such item exists, updates otherwise. */
    suspend fun upsert(note: Note): Unit = noteStore.upsert(secureToStorage(note))

    suspend fun update(note: Note): Unit = noteStore.update(secureToStorage(note))

    suspend fun delete(note: Note): Unit = noteStore.delete(note)


    private fun secureToStorage(note: Note): Note {
        if (note.secure) {
            val encrypted = Securer.encrypt(data = note.content, alias = note.name)
            return note.copy(content = encrypted.dataString(), iv = encrypted.iv)
        }
        return note
    }
    private fun secureToUI(note: Note): Note {
        if (note.secure) {
            // TODO: Decryption
        }
        return note
    }
}