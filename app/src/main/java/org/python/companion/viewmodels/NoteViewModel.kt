package org.python.companion.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.python.backend.datatype.Note
import org.python.companion.CompanionApplication
import org.python.companion.support.Util
import org.python.companion.support.Util.stateInViewModel

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteRepository = (application as CompanionApplication).noteRepository

    private val allNotes = MutableStateFlow(emptyFlow<PagingData<Note>>())
    private val searchNotes = MutableStateFlow(emptyFlow<PagingData<Note>>())

    private val _search = MutableStateFlow(null as String?)
    val search: StateFlow<String?> = _search

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * Function to load viewModel data.
     * The loading state can be retrieved with [isLoading].
     */
    fun load() = Util.effect(viewModelScope) {
        _isLoading.value = true
        allNotes.value = noteRepository.allNotes
        _isLoading.value = false
    }

    suspend fun add(note: Note): Boolean = noteRepository.add(note)
    suspend fun upsert(note: Note): Unit = noteRepository.upsert(note)

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