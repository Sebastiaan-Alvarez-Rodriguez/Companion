package org.python.companion.ui.note

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteCategory
import org.python.backend.data.datatype.NoteWithCategory
import org.python.backend.data.datatype.RenderType
import org.python.companion.R
import org.python.companion.support.ItemDrawCache
import org.python.companion.support.RenderUtil
import org.python.companion.support.UiUtil
import org.python.companion.viewmodels.NoteViewModel


@Composable
fun NoteScreenViewSingle(
    noteViewModel: NoteViewModel,
    id: Long,
    onDeleteClick: ((Note) -> Unit),
    onRenderTypeClick: (RenderType) -> Unit,
    onCategoryClick: ((NoteCategory) -> Unit),
    onEditClick: ((Note, Int) -> Unit),
) {
    val noteWithCategory by noteViewModel.getWithCategoryLive(id).collectAsState(null)
    noteWithCategory.let {
        if (it == null)
            UiUtil.SimpleLoading()
        else
            NoteScreenViewSingleReady(it, noteViewModel, onDeleteClick, onRenderTypeClick, onCategoryClick, onEditClick)
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
private fun NoteScreenViewSingleReady(
    noteWithCategory: NoteWithCategory,
    noteViewModel: NoteViewModel,
    onDeleteClick: (Note) -> Unit,
    onRenderTypeClick: (RenderType) -> Unit,
    onCategoryClick: (NoteCategory) -> Unit,
    onEditClick: (Note, Int) -> Unit,
) {
    val scrollState = rememberScrollState()

    val isSearching by noteViewModel.isSearching.collectAsState()
    val searchParameters by noteViewModel.searchParameters.collectAsState()
    var searchResultIndex by remember { mutableStateOf(0) } // Index of search result the user currently is interested in.

    val highlightIfSearching: (text: String, enable: Boolean?, matches: List<NoteViewModel.FindResult>) -> AnnotatedString.Builder = { text, flag, matches ->
        when {
            isSearching && flag != null && flag -> noteViewModel.highlightText(text, matches)
            else -> AnnotatedString.Builder(text)
        }
    }

    val titleMatches = rememberSaveable(searchParameters, isSearching, noteWithCategory) { searchParameters.let { if (!isSearching || it == null || !it.inTitle) emptyList() else noteViewModel.findMatches(noteWithCategory.note.name) } }
    val contentMatches = rememberSaveable(searchParameters, isSearching, noteWithCategory) {  searchParameters.let { if (!isSearching || it == null || !it.inContent) emptyList() else noteViewModel.findMatches(noteWithCategory.note.content) } }
    val searchMatchAmount = titleMatches.size + contentMatches.size

    val title = noteViewModel.highlightSelection(
        input = highlightIfSearching(noteWithCategory.note.name, searchParameters?.inTitle, titleMatches),
        matches = titleMatches,
        selectedHighlightIndex = searchResultIndex
    ).toAnnotatedString()
    val content = noteViewModel.highlightSelection(
        input = highlightIfSearching(noteWithCategory.note.content, searchParameters?.inContent, contentMatches),
        matches = contentMatches,
        selectedHighlightIndex = searchResultIndex - titleMatches.size
    ).toAnnotatedString()

    lateinit var titleScrollFunction: (Int) -> Unit
    lateinit var contentScrollFunction: (Int) -> Unit

    val defaultPadding = dimensionResource(id = R.dimen.padding_default)

    val contentDrawCache: ItemDrawCache = remember(noteWithCategory.note.renderType) { ItemDrawCache() }

    Column(modifier = Modifier.fillMaxSize().padding(defaultPadding)) {
        ViewHeader(noteWithCategory = noteWithCategory, onDeleteClick = onDeleteClick, onRenderTypeClick = onRenderTypeClick, onCategoryClick = onCategoryClick, onEditClick = { onEditClick(it, scrollState.value) })
        Spacer(Modifier.height(defaultPadding))

        Column(modifier = Modifier.weight(0.9f, fill = false).verticalScroll(scrollState)) {
            Card(elevation = 5.dp) {
                titleScrollFunction = UiUtil.simpleScrollableRenderText(
                    text = title,
                    fontSize = LocalTextStyle.current.fontSize.times(1.15),
                    renderType = noteWithCategory.note.renderType,
                    itemDrawCache = noteViewModel.drawCache.getOrDefaultPut(noteWithCategory.note.noteId, ItemDrawCache()),
                    modifier = Modifier.fillMaxWidth().padding(defaultPadding),
                    textAlign = TextAlign.Center,
                    scrollState = scrollState
                )
            }

            Spacer(Modifier.height(defaultPadding))

            Card(elevation = 5.dp) {
                contentScrollFunction = UiUtil.simpleScrollableRenderText(
                    text = content,
                    renderType = noteWithCategory.note.renderType,
                    itemDrawCache = contentDrawCache,
                    modifier = Modifier.fillMaxWidth().padding(defaultPadding),
                    scrollState = scrollState
                )
            }
        }
        if (isSearching) {
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

@Composable
private fun ViewHeader(noteWithCategory: NoteWithCategory, onDeleteClick: (Note) -> Unit, onRenderTypeClick: (RenderType) -> Unit, onCategoryClick: (NoteCategory) -> Unit, onEditClick: (Note) -> Unit) {
    var renderMenuExpanded by remember { mutableStateOf(false) }

    Card(border = BorderStroke(width = 1.dp, Color(noteWithCategory.noteCategory.color.toArgb())), elevation = 5.dp) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { onDeleteClick(noteWithCategory.note) }) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete note")
            }
            Row {
                IconButton(onClick = { renderMenuExpanded = !renderMenuExpanded }) {
                    RenderUtil.iconForRenderType(noteWithCategory.note.renderType)()
                }
                DropdownMenu(
                    expanded = renderMenuExpanded,
                    onDismissRequest = { renderMenuExpanded = false },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    for (value in RenderType.values()) {
                        if (value != noteWithCategory.note.renderType) {
                            IconButton(onClick = { onRenderTypeClick(value) }) {
                                RenderUtil.iconForRenderType(value)()
                            }
                        }
                    }
                }

                IconButton(onClick = { onCategoryClick(noteWithCategory.noteCategory) }) {
                    Icon(
                        tint = Color(noteWithCategory.noteCategory.color.toArgb()),
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = "Edit category"
                    )
                }
                IconButton(onClick = { onEditClick(noteWithCategory.note) }) {
                    Icon(imageVector = Icons.Outlined.Edit,contentDescription = "Edit note")
                }
            }
        }
    }
}