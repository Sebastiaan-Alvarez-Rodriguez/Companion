package org.python.companion.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteCategory
import org.python.companion.CompanionApplication
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.stateInViewModel
import org.python.companion.ui.note.category.NoteCategorySearchParameters

class NoteCategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val noteCategoryRepository = (application as CompanionApplication).noteCategoryRepository

    private val allNoteCategories = MutableStateFlow(emptyFlow<PagingData<NoteCategory>>().cachedIn(viewModelScope))

    private val _searchParameters = MutableStateFlow<NoteCategorySearchParameters?>(null)
    private val _isLoading = MutableStateFlow(true)


    /**
     * Search parameters to filter [noteCategories] with. If {{null}}, there is no ongoing search.
     * This data is also used inside note views to highlight matches.
     */
    val searchParameters: StateFlow<NoteCategorySearchParameters?> = _searchParameters
    val isSearching: StateFlow<Boolean> = _searchParameters.map { it != null && it.text.isNotEmpty() }.stateInViewModel(viewModelScope, false)

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

    fun updateSearchQuery(searchParameters: NoteCategorySearchParameters?) {
        _searchParameters.value = searchParameters
    }
    fun toggleSearchQuery() {
        _searchParameters.value = if (_searchParameters.value == null) NoteCategorySearchParameters() else null
    }

    fun filterNoteCategories(category: NoteCategory, params: NoteCategorySearchParameters) =
        category.name.contains(params.text, ignoreCase = !params.caseSensitive)

    fun filterNoteCategories(category: NoteCategory, re: Regex) =
        category.name.contains(re)

    @OptIn(ExperimentalCoroutinesApi::class)
    val noteCategories: StateFlow<Flow<PagingData<NoteCategory>>> =
        searchParameters.flatMapLatest { search -> noteCategories(search) }.stateInViewModel(viewModelScope, initialValue = emptyFlow())

    private fun noteCategories(params: NoteCategorySearchParameters?) = when(params) {
        null -> allNoteCategories
        else -> {
            val re = if (params.caseSensitive) Regex(params.text) else Regex(params.text, option = RegexOption.IGNORE_CASE)
            allNoteCategories.map { flow -> flow.map { page -> page.filter {
                if (params.regex)
                    return@filter filterNoteCategories(it, re)
                return@filter filterNoteCategories(it, params)
            } } }
        }
    }
}