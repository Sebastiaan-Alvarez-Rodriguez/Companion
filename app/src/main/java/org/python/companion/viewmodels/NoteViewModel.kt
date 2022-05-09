package org.python.companion.viewmodels

import android.app.Application
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.stateInViewModel
import org.python.companion.ui.note.NoteSearchParameters
import org.python.companion.ui.note.NoteSortParameters
import org.python.companion.ui.theme.DarkColorPalette
import org.python.companion.ui.theme.Purple500
import org.python.datacomm.Result

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteRepository = (application as CompanionApplication).noteRepository
    val securityActor = (application as CompanionApplication).securityActor

    val hasSecureNotes: Flow<Boolean> by lazy { noteRepository.hasSecureNotes().stateInViewModel(viewModelScope, false) }
    var clearance = securityActor.clearance.stateInViewModel(viewModelScope, 0)

    private val _sortParameters = MutableStateFlow(NoteSortParameters.fromPreferences(application.baseContext))
    private val _searchParameters = MutableStateFlow<NoteSearchParameters?>(null)
    private val _isLoading = MutableStateFlow(true)


    /** Sort parameters to sort [notes] with. */
    val sortParameters: StateFlow<NoteSortParameters> = _sortParameters
    /**
     * Search parameters to filter [notes] with.
     * This data is also used inside note views to highlight matches.
     */
    val searchParameters: StateFlow<NoteSearchParameters?> = _searchParameters
    val isSearching: StateFlow<Boolean> = _searchParameters.map { it != null && it.text.isNotEmpty() }.stateInViewModel(viewModelScope, false)

    val isLoading: StateFlow<Boolean> = _isLoading

    private val allNotes = MutableStateFlow(emptyFlow<PagingData<NoteWithCategory>>().cachedIn(viewModelScope))

    /** Function to load viewModel data. The loading state can be retrieved with [isLoading]. */
    fun load() {
        UiUtil.effect(viewModelScope) {
            _isLoading.value = true
            allNotes.value = _sortParameters.flatMapLatest { params -> noteRepository.allNotes(params.column, params.ascending) }
            _isLoading.value = false
        }
    }
    suspend fun add(note: Note): Result = noteRepository.add(note)

    /** Inserts-or-updates [Note]. [Result] contains [Long], the updated id, on success. */
    suspend fun upsert(note: Note): Result = noteRepository.upsert(note)
    /** Updates [Note]. [Result] contains [Long], the updated id, on success. */
    suspend fun update(oldNote: Note, updateNote: Note): Result = noteRepository.update(oldNote, updateNote)

    /** Delete notes by id */
    suspend fun delete(items: Collection<Note>): Unit = noteRepository.delete(items)
    suspend fun delete(note: Note): Result = noteRepository.delete(note)
    suspend fun deleteAllSecure(): Unit = noteRepository.deleteAllSecure()

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
    fun updateSearchParameters(searchParameters: NoteSearchParameters?) {
        _searchParameters.value = searchParameters
    }
    fun toggleSearch() {
        _searchParameters.value = if (_searchParameters.value == null) NoteSearchParameters() else null
    }


    private fun filterNote(note: Note, params: NoteSearchParameters) =
        (params.inTitle && note.name.contains(params.text, ignoreCase = !params.caseSensitive)) ||
                (params.inContent && note.content.contains(params.text, ignoreCase = !params.caseSensitive))
    private fun filterNote(note: Note, params: NoteSearchParameters, re: Regex) =
        (params.inTitle && note.name.contains(re)) || (params.inContent && note.content.contains(re))

    /** Given a text, finds all matches for [searchParameters] */
    fun findMatches(text: String): List<FindResult> =
        when {
            !isSearching.value -> emptyList()
            else -> searchParameters.value.let { params ->
                if (params == null)
                    return emptyList()

                if (params.regex) {
                    val re = Regex(params.text, options = when {
                        params.caseSensitive -> setOf(RegexOption.IGNORE_CASE)
                        else -> emptySet()
                    })
                    return re.findAll(text).map { FindResult(it.range.first, it.range.last+1) }.toList()
                } else {
                    var x = text.indexOf(params.text, ignoreCase = !params.caseSensitive)
                    val data: ArrayList<FindResult> = ArrayList()

                    while (x != -1) {
                        data.add(FindResult(x, x + params.text.length))
                        x = text.indexOf(params.text, startIndex = x+1, ignoreCase = !params.caseSensitive)
                    }
                    return data
                }
            }
        }

    fun highlightSelection(
        input: AnnotatedString.Builder,
        matches: List<FindResult>,
        selectedHighlightIndex: Int,
        selectedStyle: SpanStyle = SpanStyle(color = DarkColorPalette.primary, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, background = Purple500),
    ): AnnotatedString.Builder {
        if (selectedHighlightIndex >= 0 && selectedHighlightIndex < matches.size)
            input.addStyle(selectedStyle, matches[selectedHighlightIndex].start, matches[selectedHighlightIndex].end)
        return input
    }

    fun highlightText(input: String, matches: List<FindResult>, highlightStyle: SpanStyle = SpanStyle(color = DarkColorPalette.primary, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)): AnnotatedString.Builder {
        val builder = AnnotatedString.Builder(input)
        matches.forEach { match -> builder.addStyle(highlightStyle, match.start, match.end) }
        return builder
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<Flow<PagingData<NoteWithCategory>>> =
        searchParameters.flatMapLatest { params -> notes(params) }.stateInViewModel(viewModelScope, initialValue = emptyFlow())

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

    data class FindResult(val start: Int, val end: Int)
}