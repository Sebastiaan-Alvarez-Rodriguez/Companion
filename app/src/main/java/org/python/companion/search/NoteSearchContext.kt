package org.python.companion.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.python.backend.data.datatype.Note
import org.python.companion.ui.note.NoteSearchParameters

class NoteSearchContext {
    private val _searchParameters = MutableStateFlow<NoteSearchParameters?>(null)
    /**
     * Search parameters to filter [Note] objects with.
     * This data is also used inside note views to highlight matches.
     */
    val searchParameters: StateFlow<NoteSearchParameters?> = _searchParameters

    val isSearching: Flow<Boolean> = _searchParameters.map { it != null && it.text.isNotEmpty() }

    /** Given a text, finds all matches for [searchParameters] */
    fun findMatches(text: String): List<FindResult> = _searchParameters.value.let {
        when {
            it == null || it.text.isEmpty() -> emptyList()
            it.regex -> {
                val re = Regex(it.text, options = when {
                    it.caseSensitive -> setOf(RegexOption.IGNORE_CASE)
                    else -> emptySet()
                })
                return@let re.findAll(text).map { FindResult(it.range.first, it.range.last+1) }.toList()
            }
            else -> {
                var x = text.indexOf(it.text, ignoreCase = !it.caseSensitive)
                val data: ArrayList<FindResult> = ArrayList()

                while (x != -1) {
                    data.add(FindResult(x, x + it.text.length))
                    x = text.indexOf(it.text, startIndex = x+1, ignoreCase = !it.caseSensitive)
                }
                return@let data
            }
        }
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
}