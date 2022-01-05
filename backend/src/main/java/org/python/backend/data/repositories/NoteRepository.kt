package org.python.backend.data.repositories

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.python.backend.data.datatype.Note
import org.python.backend.data.stores.NoteStore
import org.python.backend.security.VerificationToken
import org.python.db.CompanionDatabase

class NoteRepository(private val noteStore: NoteStore) {
    constructor(companionDatabase: CompanionDatabase) : this(NoteStore(companionDatabase))

    fun allNotes(token: VerificationToken?) : Flow<PagingData<Note>> {
        return when (token) {
            null -> noteStore.getAllNotes()
            else -> noteStore.getAllNotesWithSecure()
        }
    }

    /**
     * Rerieves a note by name.
     * @return found note on succes, `null` if no such name exists.
     */
    suspend fun getByName(name: String): Note? = noteStore.getByName(name)

    /**
     * Adds a note. If a conflict exists, skips adding proposed item.
     * @return `true` on success, `false` on conflict.
     */
    suspend fun add(note: Note): Boolean = noteStore.add(note)

    /** Insert-or-update (upsert) inserts the item if no such item exists, updates otherwise. */
    suspend fun upsert(note: Note): Unit = noteStore.upsert(note)

    suspend fun update(note: Note): Unit = noteStore.update(note)

    suspend fun delete(note: Note): Unit = noteStore.delete(note)
}