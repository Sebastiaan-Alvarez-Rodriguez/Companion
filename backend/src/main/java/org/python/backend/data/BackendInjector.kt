package org.python.backend.data

import android.app.Application
import org.python.backend.data.repositories.AnniversaryRepository
import org.python.backend.data.repositories.NoteRepository
import org.python.db.CompanionDatabase

open class BackendInjector : Application() {
    private val companionDatabase by lazy {
        CompanionDatabase.getInstance(this)
    }
    val noteRepository by lazy { NoteRepository(companionDatabase) }
    val anniversaryRepository by lazy { AnniversaryRepository(companionDatabase) }
}