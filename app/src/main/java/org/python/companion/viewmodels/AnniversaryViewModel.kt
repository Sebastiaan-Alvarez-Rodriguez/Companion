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
import org.python.backend.datatype.Anniversary
import org.python.companion.CompanionApplication
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.stateInViewModel

class AnniversaryViewModel(application: Application) : AndroidViewModel(application) {
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val anniversaryRepository = (application as CompanionApplication).anniversaryRepository

    private val allAnniversaries = MutableStateFlow(emptyFlow<PagingData<Anniversary>>())
    private val searchAnniversaries = MutableStateFlow(emptyFlow<PagingData<Anniversary>>())

    private val _search = MutableStateFlow(null as String?)
    val search: StateFlow<String?> = _search

    /**
     * Function to load viewModel data.
     * The loading state can be retrieved with [isLoading].
     */
    fun load() = UiUtil.effect(viewModelScope) {
        _isLoading.value = true
        allAnniversaries.value = anniversaryRepository.allAnniversaries
        _isLoading.value = false
    }

    suspend fun add(anniversary: Anniversary): Boolean = anniversaryRepository.add(anniversary)
    suspend fun upsert(anniversary: Anniversary): Unit = anniversaryRepository.upsert(anniversary)

    suspend fun getbyName(anniversary: Anniversary): Anniversary? = anniversaryRepository.getByName(anniversary.name)
    suspend fun getbyName(name: String): Anniversary? = anniversaryRepository.getByName(name)

    @OptIn(ExperimentalCoroutinesApi::class)
    val anniversaries: StateFlow<Flow<PagingData<Anniversary>>> = search.flatMapLatest { search -> anniversaries(search) }.stateInViewModel(viewModelScope, initialValue = emptyFlow())

    fun with(func: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch { func() }
    }

    private fun anniversaries(search: String?) = when {
        search.isNullOrEmpty() -> allAnniversaries
        else -> searchAnniversaries
    }
}