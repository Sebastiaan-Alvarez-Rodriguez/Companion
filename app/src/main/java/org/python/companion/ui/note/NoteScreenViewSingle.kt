package org.python.companion.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteCategory
import org.python.companion.R
import org.python.companion.support.LoadState
import org.python.companion.support.UiUtil
import org.python.companion.viewmodels.NoteViewModel
import timber.log.Timber


@Composable
fun NoteScreenViewSingle(
    noteViewModel: NoteViewModel,
    navController: NavController,
    id: Long,
    onDeleteClick: ((Note) -> Unit)? = null,
    onEditClick: ((Note) -> Unit)? = null,
    onCategoryClick: ((Note) -> Unit)? = null,
) {
    var state by remember { mutableStateOf(LoadState.STATE_LOADING) }
    var note by remember { mutableStateOf<Note?>(null) }
    var noteCategory by remember { mutableStateOf<NoteCategory?>(null) }

    val authenticated by noteViewModel.securityActor.authenticated.collectAsState()

    when (state) {
        LoadState.STATE_LOADING -> if (note == null) {
            UiUtil.SimpleLoading()
            LaunchedEffect(state) {
                val loadedNoteWithCategory = noteViewModel.getWithCategory(id)
                if (loadedNoteWithCategory == null) {
                    if (authenticated)
                        state = LoadState.STATE_FAILED
                    else
                        navController.navigateUp() // Note was edited, became secure
                } else {
                    note = loadedNoteWithCategory.note
                    noteCategory = loadedNoteWithCategory.noteCategory
                    state = LoadState.STATE_OK
                }
            }
        }
        LoadState.STATE_OK -> NoteScreenViewSingleReady(note!!, noteCategory!!, onEditClick, onDeleteClick, onCategoryClick)
        LoadState.STATE_FAILED -> {
            Timber.e("Could not find note with id: $id")
            UiUtil.SimpleProblem("Could not find note with id: $id")
        }
    }
}

/**
 * Detail screen for a single note.
 * @param note Title of the passed note.
 */
@Composable
fun NoteScreenViewSingleReady(
    note: Note,
    noteCategory: NoteCategory,
    onCategoryClick: ((Note) -> Unit)? = null,
    onDeleteClick: ((Note) -> Unit)? = null,
    onEditClick: ((Note) -> Unit)? = null,
) {
    val title by remember { mutableStateOf(note.name) }
    val content by remember { mutableStateOf(note.content) }
    val anyOptionsEnabled = onEditClick != null || onDeleteClick != null

    val defaultPadding = dimensionResource(id = R.dimen.padding_default)

    Column(modifier = Modifier.padding(defaultPadding)) {
        Card(elevation = 5.dp) {
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
                            IconButton(onClick = { onDeleteClick(note) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete note"
                                )
                            }
                        if (onCategoryClick != null)
                            IconButton(onClick = { onCategoryClick(note) }) {
                                Icon(
                                    modifier = noteCategory.color.let { Modifier.background(Color(it.toArgb())) },
                                    imageVector = Icons.Outlined.Article,
                                    contentDescription = "Edit category"
                                )
                            }
                        if (onEditClick != null)
                            IconButton(onClick = { onEditClick(note) }) {
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
            Text(text = content, modifier = Modifier.fillMaxWidth().padding(defaultPadding))
        }
    }
}
