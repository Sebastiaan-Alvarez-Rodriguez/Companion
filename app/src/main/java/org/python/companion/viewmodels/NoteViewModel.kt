package org.python.companion.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteWithCategory
import org.python.companion.CompanionApplication
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.stateInViewModel

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteRepository = (application as CompanionApplication).noteRepository
    val securityActor = (application as CompanionApplication).securityActor

    val hasSecureNotes: Flow<Boolean> by lazy { noteRepository.hasSecureNotes().stateInViewModel(viewModelScope, false) }
    var authenticated = securityActor.authenticated.stateInViewModel(viewModelScope, false)

    private val allNotes = MutableStateFlow(emptyFlow<PagingData<NoteWithCategory>>().cachedIn(viewModelScope))
    private val searchNotes = MutableStateFlow(emptyFlow<PagingData<NoteWithCategory>>())

    private val _search = MutableStateFlow(null as String?)
    private val _isLoading = MutableStateFlow(true)


    val search: StateFlow<String?> = _search
    val isLoading: StateFlow<Boolean> = _isLoading

    /** Function to load viewModel data. The loading state can be retrieved with [isLoading]. */
    fun load() {
        UiUtil.effect(viewModelScope) {
            _isLoading.value = true
            allNotes.value = noteRepository.allNotes().cachedIn(viewModelScope)
            _isLoading.value = false
        }
    }
    suspend fun add(note: Note): Boolean = noteRepository.add(note)
    suspend fun upsert(note: Note): Boolean = noteRepository.upsert(note)
    suspend fun update(oldNote: Note, updateNote: Note): Boolean = noteRepository.update(oldNote, updateNote)

    /** Delete notes by id */
    suspend fun delete(items: Collection<Note>): Unit = noteRepository.delete(items)
    suspend fun delete(note: Note): Unit = noteRepository.delete(note)
    suspend fun deleteAllSecure(): Unit = noteRepository.deleteAllSecure()

    suspend fun get(id: Long): Note? = noteRepository.get(id)
    suspend fun getWithCategory(id: Long): NoteWithCategory? = noteRepository.getWithCategory(id)

    suspend fun getbyName(note: Note): Note? = noteRepository.getByName(note.name)
    suspend fun getbyName(name: String): Note? = noteRepository.getByName(name)

    /** Sets a note to be or not be favored */
    suspend fun setFavorite(note: Note, favorite: Boolean): Unit = noteRepository.setFavorite(note, favorite)

    suspend fun updateCategoryForNote(noteId: Long, categoryId: Long): Unit = noteRepository.updateCategoryForNote(noteId, categoryId)

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<Flow<PagingData<NoteWithCategory>>> =
        search.flatMapLatest { search -> notes(search) }.stateInViewModel(viewModelScope, initialValue = emptyFlow())

    private fun notes(search: String?) = when {
        search.isNullOrEmpty() -> allNotes
        else -> searchNotes
    }
}