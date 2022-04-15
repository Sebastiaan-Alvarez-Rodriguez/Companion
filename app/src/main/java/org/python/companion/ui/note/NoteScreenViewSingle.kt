package org.python.companion.ui.note

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteCategory
import org.python.backend.data.datatype.NoteWithCategory
import org.python.companion.R
import org.python.companion.support.UiUtil
import org.python.companion.viewmodels.NoteViewModel


@Composable
fun NoteScreenViewSingle(
    noteViewModel: NoteViewModel,
    id: Long,
    onDeleteClick: ((Note) -> Unit)? = null,
    onEditClick: ((Note) -> Unit)? = null,
    onCategoryClick: ((NoteCategory) -> Unit)? = null,
) {
    val noteWithCategory by noteViewModel.getWithCategoryLive(id).collectAsState(null)
    noteWithCategory.let {
        if (it == null)
            UiUtil.SimpleLoading()
        else
            NoteScreenViewSingleReady(it, noteViewModel, onEditClick, onDeleteClick, onCategoryClick)
    }
}

/**
 * Detail screen for a single note.
 * @param noteWithCategory Note together with its category.
 * @param onEditClick Lambda for edit clicks.
 * @param onDeleteClick Lambda for delete clicks.
 * @param onCategoryClick Lambda for category clicks.
 */
@Composable
fun NoteScreenViewSingleReady(
    noteWithCategory: NoteWithCategory,
    noteViewModel: NoteViewModel,
    onEditClick: ((Note) -> Unit)? = null,
    onDeleteClick: ((Note) -> Unit)? = null,
    onCategoryClick: ((NoteCategory) -> Unit)? = null,
) {
    val scrollState = rememberScrollState()

    val searchParameters by noteViewModel.searchParameters.collectAsState()
    var searchResultIndex by remember { mutableStateOf(0) } // Index of search result the user currently is interested in.

    val highlightIfSearching: (text: String, enable: Boolean, matches: List<NoteViewModel.FindResult>) -> AnnotatedString.Builder = { text, flag, matches ->
        when {
            searchParameters != null && flag -> noteViewModel.highlightText(text, matches)
            else -> AnnotatedString.Builder(text)
        }
    }

    val titleMatches = rememberSaveable { searchParameters.let { if (it == null || !it.inTitle) emptyList() else noteViewModel.findMatches(noteWithCategory.note.name) } }
    val contentMatches = rememberSaveable {  searchParameters.let { if (it == null || !it.inContent) emptyList() else noteViewModel.findMatches(noteWithCategory.note.content) } }
    val searchMatchAmount = titleMatches.size + contentMatches.size

    val title = noteViewModel.highlightSelection(
        input = highlightIfSearching(noteWithCategory.note.name, searchParameters?.inTitle ?: false, titleMatches),
        matches = titleMatches,
        selectedHighlightIndex = searchResultIndex
    ).toAnnotatedString()
    val content = noteViewModel.highlightSelection(
        input = highlightIfSearching(noteWithCategory.note.content, searchParameters?.inContent ?: false, contentMatches),
        matches = contentMatches,
        selectedHighlightIndex = searchResultIndex - titleMatches.size
    ).toAnnotatedString()
    lateinit var titleScrollFunction: (Int) -> Unit
    lateinit var contentScrollFunction: (Int) -> Unit

    val anyOptionsEnabled = onEditClick != null || onDeleteClick != null || onCategoryClick != null
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)


    Column(modifier = Modifier.fillMaxSize().padding(defaultPadding)) {
        Column(modifier = Modifier.weight(0.9f, fill = false).verticalScroll(scrollState)) {
            Card(border = BorderStroke(width = 1.dp, Color(noteWithCategory.noteCategory.color.toArgb())), elevation = 5.dp) {
                Column(modifier = Modifier.padding(defaultPadding)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        titleScrollFunction = UiUtil.simpleScrollableText(text = title, scrollState = scrollState)
                    }
                    if (anyOptionsEnabled) {
                        Spacer(Modifier.height(defaultPadding))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            if (onDeleteClick != null)
                                IconButton(onClick = { onDeleteClick(noteWithCategory.note) }) {
                                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete note")
                                }
                            if (onCategoryClick != null)
                                IconButton(onClick = { onCategoryClick(noteWithCategory.noteCategory) }) {
                                    Icon(
                                        tint = Color(noteWithCategory.noteCategory.color.toArgb()),
                                        imageVector = Icons.Outlined.Article,
                                        contentDescription = "Edit category"
                                    )
                                }
                            if (onEditClick != null)
                                IconButton(onClick = { onEditClick(noteWithCategory.note) }) {
                                    Icon(imageVector = Icons.Outlined.Edit,contentDescription = "Edit note")
                                }
                        }
                    }
                }
            }

            Spacer(Modifier.height(defaultPadding))
            Card(elevation = 5.dp) {
                contentScrollFunction = UiUtil.simpleScrollableText(text = content, modifier = Modifier.fillMaxWidth().padding(defaultPadding), scrollState = scrollState)
            }
        }
        if (searchParameters != null) {
            Column(modifier = Modifier.weight(0.1f, fill = true)) {
                Spacer(modifier = Modifier.height(defaultPadding))
                UiUtil.SimpleSearchMatchIteratorHeader(currentItem = searchResultIndex, numItems = searchMatchAmount) {
                    searchResultIndex = it
                    when {
                        it < titleMatches.size -> titleScrollFunction(it)
                        else -> contentScrollFunction(it - titleMatches.size)
                    }
                }
            }
        }
    }
}
