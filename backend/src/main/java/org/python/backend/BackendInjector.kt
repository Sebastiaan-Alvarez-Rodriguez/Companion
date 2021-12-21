package org.python.backend

import android.app.Application
import org.python.backend.repositories.AnniversaryRepository
import org.python.backend.repositories.NoteRepository
import org.python.db.CompanionDatabase

open class BackendInjector : Application() {
    private val companionDatabase by lazy {
        CompanionDatabase.getInstance(this)
    }
    val noteRepository by lazy { NoteRepository(companionDatabase) }
    val anniversaryRepository by lazy { AnniversaryRepository(companionDatabase) }
}