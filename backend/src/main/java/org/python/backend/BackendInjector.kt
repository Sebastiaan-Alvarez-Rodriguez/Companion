package org.python.backend

import org.python.backend.data.repositories.NoteCategoryRepository
import org.python.backend.data.repositories.NoteRepository
import org.python.security.SecurityActor
import org.python.db.DBInjector

open class BackendInjector : DBInjector() {

    // Security actor singleton
    val securityActor by lazy { SecurityActor() }

    // Repositories
    val noteRepository by lazy { NoteRepository(securityActor, companionDatabase) }
    val noteCategoryRepository by lazy { NoteCategoryRepository(securityActor, companionDatabase) }
}