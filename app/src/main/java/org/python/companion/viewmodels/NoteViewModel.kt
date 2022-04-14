package org.python.companion.viewmodels

import android.app.Application
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
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
import org.python.companion.CompanionApplication
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.stateInViewModel
import org.python.companion.ui.note.SearchParameters
import org.python.companion.ui.theme.DarkColorPalette
import org.python.companion.ui.theme.LightColorPalette

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteRepository = (application as CompanionApplication).noteRepository
    val securityActor = (application as CompanionApplication).securityActor

    val hasSecureNotes: Flow<Boolean> by lazy { noteRepository.hasSecureNotes().stateInViewModel(viewModelScope, false) }
    var authenticated = securityActor.authenticated.stateInViewModel(viewModelScope, false)

    private val allNotes = MutableStateFlow(emptyFlow<PagingData<NoteWithCategory>>().cachedIn(viewModelScope))

    private val _searchParameters = MutableStateFlow<SearchParameters?>(null)
    private val _isLoading = MutableStateFlow(true)


    /**
     * Search parameters to filter [notes] with. If {{null}}, there is no ongoing search.
     * This data is also used inside note views to highlight matches.
     */
    val searchParameters: StateFlow<SearchParameters?> = _searchParameters
    val isLoading: StateFlow<Boolean> = _isLoading

    /** Function to load viewModel data. The loading state can be retrieved with [isLoading]. */
    fun load() {
        UiUtil.effect(viewModelScope) {
            _isLoading.value = true
            allNotes.value = noteRepository.allNotes().cachedIn(viewModelScope)
            _isLoading.value = false
        }
    }
    suspend fun add(note: Note): Long? = noteRepository.add(note)
    suspend fun upsert(note: Note): Boolean = noteRepository.upsert(note)
    suspend fun update(oldNote: Note, updateNote: Note): Boolean = noteRepository.update(oldNote, updateNote)

    /** Delete notes by id */
    suspend fun delete(items: Collection<Note>): Unit = noteRepository.delete(items)
    suspend fun delete(note: Note): Unit = noteRepository.delete(note)
    suspend fun deleteAllSecure(): Unit = noteRepository.deleteAllSecure()

    suspend fun get(id: Long): Note? = noteRepository.get(id)
    suspend fun getWithCategory(id: Long): NoteWithCategory? = noteRepository.getWithCategory(id)
    fun getWithCategoryLive(id: Long): Flow<NoteWithCategory?> = noteRepository.getWithCategoryLive(id)


    suspend fun getbyName(note: Note): Note? = noteRepository.getByName(note.name)
    suspend fun getbyName(name: String): Note? = noteRepository.getByName(name)

    suspend fun hasConflict(name: String): Boolean = noteRepository.hasConflict(name)

    /** Sets a note to be or not be favored */
    suspend fun setFavorite(note: Note, favorite: Boolean): Unit = noteRepository.setFavorite(note, favorite)

    fun updateSearchQuery(searchParameters: SearchParameters?) {
        _searchParameters.value = searchParameters
    }
    fun toggleSearchQuery() {
        _searchParameters.value = if (_searchParameters.value == null) SearchParameters() else null
    }


    fun filterNote(note: Note, params: SearchParameters) =
        (params.inTitle && note.name.contains(params.text, ignoreCase = !params.caseSensitive)) ||
                (params.inContent && note.content.contains(params.text, ignoreCase = !params.caseSensitive))
    fun filterNote(note: Note, params: SearchParameters, re: Regex) =
        (params.inTitle && note.name.contains(re)) || (params.inContent && note.content.contains(re))

    fun highlightTextTitle(text: String, highlightStyle: SpanStyle = SpanStyle(color = DarkColorPalette.primary, fontWeight = FontWeight.Bold)) = searchParameters.value.let {
        when {
            it != null && it.inTitle -> highlightText(text, highlightStyle)
            else -> AnnotatedString(text)
        }
    }
    fun highlightTextContent(text: String, highlightStyle: SpanStyle = SpanStyle(color = DarkColorPalette.primary, fontWeight = FontWeight.Bold)) = searchParameters.value.let {
        when {
            it != null && it.inContent -> highlightText(text, highlightStyle)
            else -> AnnotatedString(text)
        }
    }
    private fun highlightText(text: String, highlightStyle: SpanStyle): AnnotatedString {
        searchParameters.value.let { params ->
            val builder = AnnotatedString.Builder(text)

            if (params == null)
                return builder.toAnnotatedString()

            if (params.regex) {
                val re = Regex(params.text, options = when {
                        params.caseSensitive -> setOf(RegexOption.IGNORE_CASE)
                        else -> emptySet()
                    }
                )
                re.findAll(text).forEach { builder.addStyle(highlightStyle, it.range.first, it.range.last+1) }
            } else {
                var x = text.indexOf(params.text, ignoreCase = !params.caseSensitive)
                while (x != -1) {
                    builder.addStyle(highlightStyle, x, x+params.text.length)
                    x = text.indexOf(params.text, startIndex = x+1, ignoreCase = !params.caseSensitive)
                }
            }
            return builder.toAnnotatedString()
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<Flow<PagingData<NoteWithCategory>>> =
        searchParameters.flatMapLatest { params -> notes(params) }.stateInViewModel(viewModelScope, initialValue = emptyFlow())

    private fun notes(params: SearchParameters?) = when (params) {
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