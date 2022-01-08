package org.python.companion.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.python.backend.data.datatype.Note
import org.python.backend.security.VerificationToken
import org.python.companion.CompanionApplication
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.stateInViewModel

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteRepository = (application as CompanionApplication).noteRepository

    private val allNotes = MutableStateFlow(emptyFlow<PagingData<Note>>().cachedIn(viewModelScope))
    private val searchNotes = MutableStateFlow(emptyFlow<PagingData<Note>>())

    private val _search = MutableStateFlow(null as String?)
    private val _isLoading = MutableStateFlow(true)

    val hasSecureNotes: Flow<Boolean> by lazy { noteRepository.hasSecureNotes() }

    val search: StateFlow<String?> = _search
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * Function to load viewModel data. The loading state can be retrieved with [isLoading].
     */
    fun load(token: VerificationToken? = null) = UiUtil.effect(viewModelScope) {
        _isLoading.value = true
        allNotes.value = noteRepository.allNotes(token).cachedIn(viewModelScope)
        _isLoading.value = false
    }

    suspend fun add(note: Note): Boolean = noteRepository.add(note)
    suspend fun upsert(note: Note): Unit = noteRepository.upsert(note)
    suspend fun update(note: Note): Unit = noteRepository.update(note)

    suspend fun delete(note: Note): Unit = noteRepository.delete(note)

    suspend fun getbyName(note: Note): Note? = noteRepository.getByName(note.name)
    suspend fun getbyName(name: String): Note? = noteRepository.getByName(name)

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<Flow<PagingData<Note>>> = search.flatMapLatest { search -> notes(search) }.stateInViewModel(viewModelScope, initialValue = emptyFlow())

    fun with(func: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch { func() }
    }

    private fun notes(search: String?) = when {
        search.isNullOrEmpty() -> allNotes
        else -> searchNotes
    }
}