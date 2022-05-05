package org.python.backend.data.repositories

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.python.backend.data.datatype.NoteCategory
import org.python.backend.data.stores.NoteCategoryStore
import org.python.security.SecurityActor
import org.python.db.CompanionDatabase
import org.python.db.entities.note.RoomNoteCategory
import org.python.db.entities.note.RoomNoteWithCategory

class NoteCategoryRepository(private val securityActor: SecurityActor, private val noteCategoryStore: NoteCategoryStore) {
    constructor(securityActor: SecurityActor, companionDatabase: CompanionDatabase) :
            this(securityActor, NoteCategoryStore(companionDatabase))

    /** @return All notes in the collection when authorized. All non-secure notes when unauthorized. */
    fun allNoteCategories(sortColumn: RoomNoteCategory.Companion.SortableField, ascending: Boolean): Flow<PagingData<NoteCategory>> =
        noteCategoryStore.getAll(sortColumn, ascending)


    suspend fun get(id: Long): NoteCategory? = noteCategoryStore.get(id)

    /**
     * Rerieves a note category by name.
     * @return found note category on succes, `null` if no such name exists.
     */
    suspend fun getByName(name: String): NoteCategory? = noteCategoryStore.getByName(name)

    /**
     * Sets note category to be regular or favored.
     * @param category Category to set value for.
     * @param favorite new favored status.
     */
    suspend fun setFavorite(category: NoteCategory, favorite: Boolean): Unit = noteCategoryStore.setFavorite(category, favorite)

    /** Returns the live category for a note */
    fun categoryForNoteLive(noteId: Long): Flow<NoteCategory> =
        noteCategoryStore.categoryForNoteLive(noteId, securityActor.clearance.value)

    /** Sets note category for given note */
    suspend fun updateCategoryForNote(noteId: Long, categoryId: Long): Unit = noteCategoryStore.updateCategoryForNote(noteId, categoryId)

    /**
     * Adds a note. If a conflict exists, skips adding proposed item.
     * @return `true` on success, `false` on conflict.
     */
    suspend fun add(category: NoteCategory): Boolean = noteCategoryStore.add(category)

    /** Insert-or-update (upsert) inserts the item if no such item exists, updates otherwise. */
    suspend fun upsert(category: NoteCategory): Boolean {
        noteCategoryStore.upsert(category)
        return true
    }

    suspend fun update(updatedCategory: NoteCategory): Boolean {
        noteCategoryStore.update(updatedCategory)
        return true
    }

    suspend fun delete(category: NoteCategory): Unit = noteCategoryStore.delete(category)
}