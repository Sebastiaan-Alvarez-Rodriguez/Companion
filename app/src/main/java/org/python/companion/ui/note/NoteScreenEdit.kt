package org.python.companion.ui.note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.python.backend.data.datatype.Note
import org.python.companion.R
import org.python.companion.support.LoadState
import org.python.companion.support.UiUtil
import org.python.companion.viewmodels.NoteViewModel
import timber.log.Timber


/** Loads note to edit, then shows edit screen. */
@Composable
fun NoteScreenEdit(
    noteViewModel: NoteViewModel,
    noteName: String,
    overrideDialogMiniState: NoteOverrideDialogMiniState,
    onSaveClick: (Note, Note?) -> Unit,
    onOverrideAcceptClick: (Note, Note?) -> Unit
) {
    var state by remember { mutableStateOf(LoadState.STATE_LOADING) }
    var existingNote by remember { mutableStateOf<Note?>(null) }

    when (state) {
        LoadState.STATE_LOADING -> if (existingNote == null) {
            UiUtil.SimpleLoading()
            LaunchedEffect(state) {
                existingNote = noteViewModel.getbyName(noteName)
                state = LoadState.STATE_OK
            }
        }
        LoadState.STATE_OK -> NoteScreenEditReady(existingNote, overrideDialogMiniState, { toSaveNote -> onSaveClick(toSaveNote, existingNote) }, {  toSaveNote -> onOverrideAcceptClick(toSaveNote, existingNote)})
        LoadState.STATE_FAILED -> {
            Timber.e("Could not find note name: $noteName")
            UiUtil.SimpleProblem("Could not find note named: $noteName")
        }
    }
}

/** Edits a new note directly. */
@Composable
fun NoteScreenEditNew(
    overrideDialogMiniState: NoteOverrideDialogMiniState,
    onSaveClick: (Note) -> Unit,
    onOverrideAcceptClick: (Note) -> Unit
) = NoteScreenEditReady(null, overrideDialogMiniState, onSaveClick, onOverrideAcceptClick)

/**
 * Detail screen for editing a single note.
 * @param note Title of the passed note.
 * @param overrideDialogMiniState
 * @param onSaveClick Lambda executed when the user hits the save button.
 * @param onOverrideAcceptClick Lambda executed when the user hits the override button.
 */
@Composable
fun NoteScreenEditReady(
    note: Note?,
    overrideDialogMiniState: NoteOverrideDialogMiniState,
    onSaveClick: (Note) -> Unit,
    onOverrideAcceptClick: (Note) -> Unit
) {
    Box {
        NoteScreenOverrideDialog(overrideDialogMiniState, onOverrideAcceptClick)
        NoteScreenEditBody(note, onSaveClick)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteScreenEditBody(
    note: Note?,
    onSaveClick: (Note) -> Unit
) {
    var title by remember { mutableStateOf(note?.name ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var favorite by remember { mutableStateOf(note?.favorite ?: false)}
    var secure by remember { mutableStateOf(note?.secure ?: false)}

    val changed = lazy {
        if (note == null)
            title != "" || content != "" || favorite || secure
        else
            title != note.name || content != note.content || favorite != note.favorite || secure != note.secure
    }

    val createNoteObject: () -> Note = {
        note?.copy(
            name = title,
            content = content,
            favorite = favorite,
            secure = secure) ?:
        Note(
            name = title,
            content = content,
            favorite = favorite,
            secure = secure
        )
    }

    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(defaultPadding),
        elevation = 5.dp,
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(defaultPadding)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row {
                    IconButton(modifier = Modifier.padding(smallPadding), onClick = { favorite = !favorite }) {
                        Icon(
                            imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (favorite) "Stop favoring" else "Favorite"
                        )
                    }
                    IconButton(modifier = Modifier.padding(smallPadding), onClick = { secure = !secure }) {
                        Icon(
                            imageVector = if (secure) Icons.Filled.Lock else Icons.Outlined.Lock,
                            contentDescription =if (secure) "Stop securing" else "Secure")
                    }
                }
                Spacer(Modifier.width(defaultPadding))

                Button(onClick = { onSaveClick(createNoteObject()) }) {
                    Text(text = "Save")
                }
            }
            Spacer(Modifier.height(defaultPadding))
            OutlinedTextField(
                value = title,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
            )
            Spacer(Modifier.height(defaultPadding))

            val relocation = remember { BringIntoViewRequester() }
            val scope = rememberCoroutineScope()

            OutlinedTextField(
                modifier = Modifier
                    .bringIntoViewRequester(relocation)
                    .onFocusEvent {
                        if (it.isFocused) scope.launch { delay(300); relocation.bringIntoView() }
                    }
                    .fillMaxWidth(),
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                singleLine = false,
            )
        }
    }

    BackHandler(enabled = changed.value) {
        // TODO: Are you sure you want to go back?
    }
}

@Composable
fun NoteScreenOverrideDialog(
    overrideDialogMiniState: NoteOverrideDialogMiniState,
    onOverrideAcceptClick: (Note) -> Unit
) {
    if (overrideDialogMiniState.open.value)
        NoteOverrideDialog(
            overrideDialogMiniState.currentNote.value!!,
            overrideDialogMiniState.overriddenNote.value!!,
            {},
            { onOverrideAcceptClick(overrideDialogMiniState.overriddenNote.value!!) },
            {}
        )

    BackHandler(enabled = overrideDialogMiniState.open.value) {
        overrideDialogMiniState.close()
    }
}