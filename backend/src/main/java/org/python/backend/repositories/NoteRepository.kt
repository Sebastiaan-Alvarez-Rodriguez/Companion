package org.python.backend.repositories

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.python.backend.datatype.Note
import org.python.backend.stores.NoteStore
import org.python.db.note.NoteDatabase

class NoteRepository(private val noteStore: NoteStore) {
    constructor(noteDatabase: NoteDatabase) : this(NoteStore(noteDatabase))

    val allNotes : Flow<PagingData<Note>> = noteStore.getAllNotes()

    suspend fun add(note: Note) = noteStore.add(note)

    suspend fun update(note: Note) = noteStore.update(note)

    suspend fun delete(note: Note) = noteStore.delete(note)
}