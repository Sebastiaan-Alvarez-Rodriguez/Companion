package org.python.backend

import org.python.backend.data.repositories.AnniversaryRepository
import org.python.backend.data.repositories.NoteRepository
import org.python.backend.security.SecurityActor
import org.python.db.DBInjector

open class BackendInjector : DBInjector() {

    // Security actor singleton
    val securityActor by lazy { SecurityActor() }

    // Repositories
    val noteRepository by lazy { NoteRepository(companionDatabase) }
    val anniversaryRepository by lazy { AnniversaryRepository(companionDatabase) }
}