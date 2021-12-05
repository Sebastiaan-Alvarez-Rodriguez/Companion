package org.python.companion.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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

    fun load() = Util.effect(viewModelScope) {
        _isLoading.value = true
        allNotes.value = noteRepository.allNotes
        _isLoading.value = false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<Flow<PagingData<Note>>> =
        search.flatMapLatest { search -> notes(search) }
            .stateInViewModel(viewModelScope, initialValue = emptyFlow())

    private fun notes(search: String?) = when {
        search.isNullOrEmpty() -> allNotes
        else -> searchNotes
    }
}