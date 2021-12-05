package org.python.backend

import android.app.Application
import org.python.backend.repositories.NoteRepository
import org.python.db.note.NoteDatabase

open class BackendInjector : Application() {
    private val noteDatabase by lazy {
        NoteDatabase.getInstance(this)
    }
    val noteRepository by lazy { NoteRepository(noteDatabase) }
}