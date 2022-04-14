package org.python.companion.ui.note

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
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

//    val firstMatch = title.spanStyles.first()

    val anyOptionsEnabled = onEditClick != null || onDeleteClick != null || onCategoryClick != null
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Column(modifier = Modifier
        .verticalScroll(scrollState)
        .padding(defaultPadding)) {
        Card(border = BorderStroke(width = 1.dp, Color(noteWithCategory.noteCategory.color.toArgb())), elevation = 5.dp) {
            Column(modifier = Modifier.padding(defaultPadding)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = title)
                }
                if (anyOptionsEnabled) {
                    Spacer(Modifier.height(defaultPadding))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (onDeleteClick != null)
                            IconButton(onClick = { onDeleteClick(noteWithCategory.note) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete note"
                                )
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
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit note"
                                )
                            }
                    }
                }
            }
        }
        Spacer(Modifier.height(defaultPadding))
        Card(elevation = 5.dp) {
            Text(text = content, modifier = Modifier
                .fillMaxWidth()
                .padding(defaultPadding)
                .onGloballyPositioned { coordinates ->
//                    LaunchedEffect(firstMatch) {
//                        scrollState.animateScrollTo(coordinates.positionInParent().y.roundToInt())
//                    }


                }
            )
        }
    }
}
