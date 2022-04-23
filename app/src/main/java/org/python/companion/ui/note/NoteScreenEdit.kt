package org.python.companion.ui.note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteCategory
import org.python.backend.data.datatype.NoteWithCategory
import org.python.companion.R
import org.python.companion.support.LoadingState
import org.python.companion.support.UiUtil
import org.python.companion.viewmodels.NoteViewModel
import timber.log.Timber


/** Loads note to edit, then shows edit screen. */
@Composable
fun NoteScreenEdit(noteViewModel: NoteViewModel, id: Long, offset: Int?, onSaveClick: (Note, Note?) -> Unit) {
    var state by remember { mutableStateOf(LoadingState.LOADING) }
    var existingData by remember { mutableStateOf<NoteWithCategory?>(null) }

    when (state) {
        LoadingState.LOADING -> if (existingData == null) {
            UiUtil.SimpleLoading()
            LaunchedEffect(state) {
                existingData = noteViewModel.getWithCategory(id)
                state = LoadingState.READY
            }
        }
        LoadingState.READY -> NoteScreenEditReady(
            note = existingData?.note,
            noteCategory = existingData?.noteCategory,
            offset = offset,
            onSaveClick = { toSaveNote -> onSaveClick(toSaveNote, existingData?.note) },
        )
        else -> {
            Timber.e("Could not find note with id: $id")
            UiUtil.SimpleProblem("Could not find note with id: $id")
        }
    }
}

/** Edits a new note directly. */
@Composable
fun NoteScreenEditNew(onSaveClick: (Note) -> Unit) = NoteScreenEditReady(null, null, null, onSaveClick)

/**
 * Detail screen for editing a single note.
 * @param note Note to edit.
 * @param noteCategory Optional category assigned to passed note.
 * @param offset Optional initial scroll offset in px.
 * @param onSaveClick Lambda executed when the user hits the save button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteScreenEditReady(
    note: Note?,
    noteCategory: NoteCategory?,
    offset: Int?,
    onSaveClick: (Note) -> Unit,
) {
    var title by remember { mutableStateOf(note?.name ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var favorite by remember { mutableStateOf(note?.favorite ?: false)}
    var secure by remember { mutableStateOf(note?.secure ?: false)}

    val categoryKey = noteCategory?.categoryId ?: NoteCategory.DEFAULT.categoryId

    val noteChanged = lazy {
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
            secure = secure,
            categoryKey = categoryKey
        ) ?:
        Note(
            name = title,
            content = content,
            favorite = favorite,
            secure = secure,
            categoryKey = categoryKey
        )
    }

    val scrollState = rememberScrollState()

    val defaultPadding = dimensionResource(id = R.dimen.padding_default)

    Column(modifier = Modifier.fillMaxSize().padding(defaultPadding)) {
        Card(modifier = Modifier.fillMaxWidth(), elevation = 5.dp) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row {
                    IconButton(onClick = { favorite = !favorite }) {
                        Icon(
                            imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (favorite) "Stop favoring" else "Favorite"
                        )
                    }
                    IconButton(onClick = { secure = !secure }) {
                        Icon(
                            imageVector = if (secure) Icons.Filled.Lock else Icons.Outlined.Lock,
                            contentDescription = if (secure) "Stop securing" else "Secure"
                        )
                    }
                }
                Spacer(Modifier.width(defaultPadding))

                Button(onClick = { onSaveClick(createNoteObject()) }) {
                    Text(text = "Save")
                }
            }
        }

        Spacer(Modifier.height(defaultPadding))

        Column(modifier = Modifier.weight(0.9f, fill = false).verticalScroll(scrollState)) {
            Card(modifier = Modifier.fillMaxWidth(), elevation = 5.dp) {
                OutlinedTextField(
                    value = title,
                    modifier = Modifier.fillMaxWidth().padding(defaultPadding),
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = false,
                )
            }
            Spacer(Modifier.height(defaultPadding))

            Card(modifier = Modifier.fillMaxSize(), elevation = 5.dp) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(defaultPadding),
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    singleLine = false,
                )
            }
        }
    }

    BackHandler(enabled = noteChanged.value) {
        // TODO: Are you sure you want to go back?
    }

    offset?.let {
        UiUtil.LaunchedEffectSaveable(Unit) {
            scrollState.animateScrollTo(it)
        }
    }
}