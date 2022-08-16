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
import org.python.backend.data.datatype.NoteWithCategory
import org.python.backend.data.datatype.RenderType
import org.python.companion.CompanionApplication
import org.python.companion.support.DrawCache
import org.python.companion.support.RendererCache
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.stateInViewModel
import org.python.companion.ui.note.NoteSearchParameters
import org.python.companion.ui.note.NoteSortParameters
import org.python.datacomm.Result
import org.python.exim.EximUtil

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteRepository = (application as CompanionApplication).noteRepository
    val search = (application as CompanionApplication).noteSearchContext
    val securityActor = (application as CompanionApplication).securityActor

    val hasSecureNotes: StateFlow<Boolean> by lazy { noteRepository.hasSecureNotes().stateInViewModel(viewModelScope, false) }
    var clearance = securityActor.clearance.stateInViewModel(viewModelScope, 0)

    private val _sortParameters = MutableStateFlow(NoteSortParameters.fromPreferences(application.baseContext))
    private val _isLoading = MutableStateFlow(true)

    val rendererCache: RendererCache = RendererCache()
    val drawCache: DrawCache<Long> = DrawCache()

    /** Sort parameters to sort [notes] with. */
    val sortParameters: StateFlow<NoteSortParameters> = _sortParameters

    val isLoading: StateFlow<Boolean> = _isLoading

    private val allNotes = MutableStateFlow(emptyFlow<PagingData<NoteWithCategory>>().cachedIn(viewModelScope))

    /** Function to load viewModel data. The loading state can be retrieved with [isLoading]. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun load() {
        UiUtil.effect(viewModelScope) {
            _isLoading.value = true
            allNotes.value = _sortParameters.flatMapLatest { params -> noteRepository.allNotes(params.column, params.ascending) }
            _isLoading.value = false
        }
    }

    suspend fun add(note: Note): Result = noteRepository.add(note)
    suspend fun addAll(items: Collection<Note>, mergeStrategy: EximUtil.MergeStrategy): Result = noteRepository.addAll(items, mergeStrategy)

    /** Inserts-or-updates [Note]. [Result] contains [Long], the updated id, on success. */
    suspend fun upsert(note: Note): Result = noteRepository.upsert(note)
    /** Updates [Note]. [Result] contains [Long], the updated id, on success. */
    suspend fun update(oldNote: Note, updateNote: Note): Result = noteRepository.update(oldNote, updateNote)

    /** Delete notes by id */
    suspend fun delete(items: Collection<Note>): Unit = noteRepository.delete(items)
    suspend fun delete(note: Note): Result = noteRepository.delete(note)

    suspend fun deleteAll(): Unit = noteRepository.deleteAll()
    suspend fun deleteAllSecure(): Unit = noteRepository.deleteAllSecure()

    suspend fun getAll(): List<Note> = noteRepository.getAll()

    suspend fun get(id: Long): Note? = noteRepository.get(id)
    suspend fun getWithCategory(id: Long): NoteWithCategory? = noteRepository.getWithCategory(id)
    fun getWithCategoryLive(id: Long): Flow<NoteWithCategory?> = noteRepository.getWithCategoryLive(id)


    suspend fun getByName(name: String): Note? = noteRepository.getByName(name)

    suspend fun hasConflict(name: String): Boolean = noteRepository.hasConflict(name)
    suspend fun mayOverride(name: String): Boolean = noteRepository.mayOverride(name)

    /** Sets a note to be or not be favored */
    suspend fun setFavorite(note: Note, favorite: Boolean): Unit = noteRepository.setFavorite(note, favorite)

    suspend fun setRenderType(noteId: Long, renderType: RenderType): Unit = noteRepository.setRenderType(noteId, renderType)

    fun updateSortParameters(sortParameters: NoteSortParameters) {
        _sortParameters.value = sortParameters
        sortParameters.toPreferences((getApplication() as CompanionApplication).baseContext)
    }

    private fun filterNote(note: Note, params: NoteSearchParameters) =
        (params.inTitle && note.name.contains(params.text, ignoreCase = !params.caseSensitive)) ||
                (params.inContent && note.content.contains(params.text, ignoreCase = !params.caseSensitive))
    private fun filterNote(note: Note, params: NoteSearchParameters, re: Regex) =
        (params.inTitle && note.name.contains(re)) || (params.inContent && note.content.contains(re))

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<Flow<PagingData<NoteWithCategory>>> =
        search.searchParameters.flatMapLatest { params -> notes(params) }.stateInViewModel(viewModelScope, initialValue = emptyFlow())

    private fun notes(params: NoteSearchParameters?) = when (params) {
        null -> allNotes
        else -> {
            val re = if (params.caseSensitive) Regex(params.text) else Regex(params.text, option = RegexOption.IGNORE_CASE)
            allNotes.map { flow -> flow.map { page -> page.filter {
                    if (params.regex)
                        return@filter filterNote(it.note, params, re)
                    return@filter filterNote(it.note, params)
            } } }
        }
    }
}