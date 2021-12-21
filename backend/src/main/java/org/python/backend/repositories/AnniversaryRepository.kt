package org.python.backend.repositories

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.python.backend.datatype.Anniversary
import org.python.backend.stores.AnniversaryStore
import org.python.db.CompanionDatabase

class AnniversaryRepository(private val anniversaryStore: AnniversaryStore) {
    constructor(anniversaryDatabase: CompanionDatabase) : this(AnniversaryStore(anniversaryDatabase))

    val allAnniversaries : Flow<PagingData<Anniversary>> = anniversaryStore.getAllAnniversaries()

    /**
     * Rerieves a anniversary by name.
     * @return found anniversary on succes, `null` if no such name exists.
     */
    suspend fun getByName(name: String): Anniversary? = anniversaryStore.getByName(name)

    /**
     * Adds a anniversary. If a conflict exists, skips adding proposed item.
     * @return `true` on success, `false` on conflict.
     */
    suspend fun add(anniversary: Anniversary): Boolean = anniversaryStore.add(anniversary)

    /** Insert-or-update (upsert) inserts the item if no such item exists, updates otherwise. */
    suspend fun upsert(anniversary: Anniversary): Unit = anniversaryStore.upsert(anniversary)

    suspend fun update(anniversary: Anniversary) = anniversaryStore.update(anniversary)

    suspend fun delete(anniversary: Anniversary) = anniversaryStore.delete(anniversary)
}