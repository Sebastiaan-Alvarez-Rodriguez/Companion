package org.python.backend.repositories

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.python.backend.datatype.Note
import org.python.backend.stores.NoteStore
import org.python.db.note.NoteDatabase

class NoteRepository(private val noteStore: NoteStore) {
    constructor(noteDatabase: NoteDatabase) : this(NoteStore(noteDatabase))

    val allNotes : Flow<PagingData<Note>> = noteStore.getAllNotes()

    /**
     * Adds a note. If a conflict exists, skips adding proposed item.
     * @return `true` on success, `false` on conflict.
     */
    suspend fun add(note: Note): Boolean = noteStore.add(note)

    /**
     * Rerieves a note by name.
     * @return found note on succes, `null` if no such name exists.
     */
    suspend fun getByName(name: String): Note? = noteStore.getByName(name)

//    suspend fun upsert(note: Note): Boolean = noteStore.upsert(note)

    suspend fun update(note: Note) = noteStore.update(note)

    suspend fun delete(note: Note) = noteStore.delete(note)
}