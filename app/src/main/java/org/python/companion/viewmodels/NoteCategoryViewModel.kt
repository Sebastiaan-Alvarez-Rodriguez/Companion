package org.python.companion.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteCategory
import org.python.companion.CompanionApplication
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.stateInViewModel

class NoteCategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val noteCategoryRepository = (application as CompanionApplication).noteCategoryRepository

    private val allNoteCategories = MutableStateFlow(emptyFlow<PagingData<NoteCategory>>().cachedIn(viewModelScope))
    private val searchNoteCategories = MutableStateFlow(emptyFlow<PagingData<NoteCategory>>())

    private val _search = MutableStateFlow(null as String?)
    private val _isLoading = MutableStateFlow(true)


    val search: StateFlow<String?> = _search
    val isLoading: StateFlow<Boolean> = _isLoading

    /** Function to load viewModel data. The loading state can be retrieved with [isLoading]. */
    fun load() {
        UiUtil.effect(viewModelScope) {
            _isLoading.value = true
            allNoteCategories.value = noteCategoryRepository.allNoteCategories().cachedIn(viewModelScope)
            _isLoading.value = false
        }
    }
    suspend fun add(noteCategory: NoteCategory): Boolean = noteCategoryRepository.add(noteCategory)
    suspend fun upsert(noteCategory: NoteCategory): Boolean = noteCategoryRepository.upsert(noteCategory)
    suspend fun update(updateCategory: NoteCategory): Boolean = noteCategoryRepository.update(updateCategory)

    suspend fun delete(noteCategory: NoteCategory): Unit = noteCategoryRepository.delete(noteCategory)

    suspend fun get(id: Long): NoteCategory? = noteCategoryRepository.get(id)

    suspend fun getbyName(noteCategory: NoteCategory): NoteCategory? = noteCategoryRepository.getByName(noteCategory.name)
    suspend fun getbyName(name: String): NoteCategory? = noteCategoryRepository.getByName(name)

    /** Sets a noteCategory to be or not be favored */
    suspend fun setFavorite(noteCategory: NoteCategory, favorite: Boolean): Unit = noteCategoryRepository.setFavorite(noteCategory, favorite)

    /** Returns the live category for a note */
    fun categoryForNoteLive(note: Note) = categoryForNoteLive(note.noteId)
    fun categoryForNoteLive(noteId: Long): Flow<NoteCategory> = noteCategoryRepository.categoryForNoteLive(noteId)

    suspend fun updateCategoryForNote(noteId: Long, categoryId: Long): Unit = noteCategoryRepository.updateCategoryForNote(noteId, categoryId)

    @OptIn(ExperimentalCoroutinesApi::class)
    val noteCategories: StateFlow<Flow<PagingData<NoteCategory>>> =
        search.flatMapLatest { search -> noteCategories(search) }.stateInViewModel(viewModelScope, initialValue = emptyFlow())

    private fun noteCategories(search: String?) = when {
        search.isNullOrEmpty() -> allNoteCategories
        else -> searchNoteCategories
    }
}