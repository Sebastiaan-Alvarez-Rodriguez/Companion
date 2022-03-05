package org.python.companion.ui.note

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.python.backend.data.datatype.Note
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
    onEditClick: ((Note) -> Unit)? = null,
    onDeleteClick: ((Note) -> Unit)? = null
) {
    var state by remember { mutableStateOf(LoadState.STATE_LOADING) }
    var note by remember { mutableStateOf<Note?>(null) }

    val authenticated by noteViewModel.securityActor.authenticated.collectAsState()

    when (state) {
        LoadState.STATE_LOADING -> if (note == null) {
            UiUtil.SimpleLoading()
            LaunchedEffect(state) {
                when (val loadedNote = noteViewModel.get(id)) {
                    null ->
                        if (authenticated)
                            state = LoadState.STATE_FAILED
                        else
                            navController.navigateUp() // Note was edited, became secure
                    else -> {
                        note = loadedNote
                        state = LoadState.STATE_OK
                    }
                }
            }
        }
        LoadState.STATE_OK -> NoteScreenViewSingleReady(note!!, onEditClick, onDeleteClick)
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
fun NoteScreenViewSingleReady(note: Note, onEditClick: ((Note) -> Unit)? = null, onDeleteClick: ((Note) -> Unit)? = null) {
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
                            Button(onClick = { onDeleteClick(note) }) {
                                Text(text = "Delete")
                            }
                        if (onEditClick != null)
                            Button(onClick = { onEditClick(note) }) {
                                Text(text = "Edit")
                            }
                    }
                }
            }
        }
        Spacer(Modifier.height(defaultPadding))
        Card(elevation = 5.dp) {
            Text(text = content, modifier = Modifier
                .fillMaxWidth()
                .padding(defaultPadding))
        }
    }
}
