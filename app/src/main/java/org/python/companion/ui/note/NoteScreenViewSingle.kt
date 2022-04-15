package org.python.companion.ui.note

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteCategory
import org.python.backend.data.datatype.NoteWithCategory
import org.python.companion.R
import org.python.companion.support.UiUtil
import org.python.companion.viewmodels.NoteViewModel
import kotlin.math.roundToInt


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

@Composable
fun NoteScreenViewSingleSearchHeader(currentItem: Int, numItems: Int, onUpdate: (Int) -> Unit) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)

    UiUtil.GenericListHeader(
        listOf(
            { Text(modifier = Modifier.padding(defaultPadding), text = "${currentItem+1}/$numItems") },
            {
                Row(modifier = Modifier.padding(defaultPadding)) {
                    IconButton(onClick = { onUpdate(((currentItem + numItems) - 1) % numItems) }) {
                        Icon(imageVector = Icons.Outlined.ArrowLeft, contentDescription = "Previous result")
                    }
                    Spacer(modifier = Modifier.width(tinyPadding))
                    IconButton(onClick = { onUpdate((currentItem + 1) % numItems) }) {
                        Icon(imageVector = Icons.Outlined.ArrowRight, contentDescription = "Next result")
                    }
                }
            }
        )
    )
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

    val title = noteViewModel.highlightTextTitle(noteWithCategory.note.name)
    val content = noteViewModel.highlightTextContent(noteWithCategory.note.content)

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()


    val searchParameters by noteViewModel.searchParameters.collectAsState()

    var searchResultIndex by remember { mutableStateOf(0) } // Index of search result the user currently is interested in.
    val searchResultPositions = /* TODO: get title scrollable positions */ content.spanStyles.map { it.start } // Char offsets for each search result

    var contentTextLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val executeScroll: () -> Unit = {
        coroutineScope.launch {
            val charOffset = searchResultPositions[searchResultIndex]
            val renderedLineOffset = contentTextLayoutResult!!.getLineForOffset(charOffset)
            val pointOffset = contentTextLayoutResult!!.getLineBottom(renderedLineOffset).roundToInt()
            scrollState.animateScrollTo(pointOffset)
        }
    }

    val anyOptionsEnabled = onEditClick != null || onDeleteClick != null || onCategoryClick != null
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Column(modifier = Modifier.fillMaxSize().padding(defaultPadding)) {
        Column(modifier = Modifier.weight(0.9f, fill = false).verticalScroll(scrollState)) {
            Card(border = BorderStroke(width = 1.dp, Color(noteWithCategory.noteCategory.color.toArgb())), elevation = 5.dp) {
                Column(modifier = Modifier.padding(defaultPadding)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(text = title)
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
                Text(text = content, modifier = Modifier.fillMaxWidth().padding(defaultPadding), onTextLayout = { layout -> contentTextLayoutResult = layout })
            }
        }
        if (searchParameters != null) {
            Column(modifier = Modifier.weight(0.1f, fill = true)) {
                Spacer(modifier = Modifier.height(defaultPadding))
                NoteScreenViewSingleSearchHeader(currentItem = searchResultIndex, numItems = searchResultPositions.size) {
                    searchResultIndex = it
                    executeScroll()
                }
            }
        }
    }
}
