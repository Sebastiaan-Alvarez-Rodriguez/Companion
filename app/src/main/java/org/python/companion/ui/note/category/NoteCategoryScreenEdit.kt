package org.python.companion.ui.note.category

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import org.python.backend.data.datatype.NoteCategory
import org.python.companion.R
import org.python.companion.support.LoadState
import org.python.companion.support.UiUtil
import org.python.companion.viewmodels.NoteCategoryViewModel
import timber.log.Timber


/** Loads notecategory to edit, then shows edit screen. */
@Composable
fun NoteCategoryScreenEdit(noteCategoryViewModel: NoteCategoryViewModel, id: Long, onSaveClick: (NoteCategory, NoteCategory?) -> Unit) {
    var state by remember { mutableStateOf(LoadState.STATE_LOADING) }
    var existingData by remember { mutableStateOf<NoteCategory?>(null) }

    when (state) {
        LoadState.STATE_LOADING -> if (existingData == null) {
            UiUtil.SimpleLoading()
            LaunchedEffect(state) {
                existingData = noteCategoryViewModel.get(id)
                state = LoadState.STATE_OK
            }
        }
        LoadState.STATE_OK -> NoteCategoryScreenEditReady(
            noteCategory = existingData,
            onSaveClick = { toSaveNoteCategory -> onSaveClick(toSaveNoteCategory, existingData) }
        )
        LoadState.STATE_FAILED -> {
            Timber.e("Could not find note with id: $id")
            UiUtil.SimpleProblem("Could not find note with id: $id")
        }
    }
}

/** Edits a new note directly. */
@Composable
fun NoteCategoryScreenEditNew(onSaveClick: (NoteCategory) -> Unit) = NoteCategoryScreenEditReady(null, onSaveClick)

/**
 * Detail screen for editing a single note.
 * @param noteCategory Category to edit.
 * @param onSaveClick Lambda executed when the user hits the save button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCategoryScreenEditReady(noteCategory: NoteCategory?, onSaveClick: (NoteCategory) -> Unit) {
    var name by remember { mutableStateOf(noteCategory?.name ?: "") }
    var color by remember { mutableStateOf(noteCategory?.color ?: NoteCategory.DEFAULT.color) }
    var favorite by remember { mutableStateOf(noteCategory?.favorite ?: false)}

    var categoryKey by remember { mutableStateOf(-1L)}


    val hasChanged = lazy {
        if (noteCategory == null)
            name != "" || color != NoteCategory.DEFAULT.color || favorite
        else
            name != noteCategory.name || color != noteCategory.color || favorite != noteCategory.favorite
    }

    val createNoteCategoryObject: () -> NoteCategory = {
        noteCategory?.copy(
            name = name,
            color = color,
            favorite = favorite
        ) ?:
        NoteCategory(
            name = name,
            color = color,
            favorite = favorite,
        )
    }

    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    Card(modifier = Modifier.fillMaxSize().padding(defaultPadding), elevation = 5.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(defaultPadding)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(modifier = Modifier.padding(smallPadding), onClick = { favorite = !favorite }) {
                    Icon(
                        imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (favorite) "Stop favoring" else "Favorite"
                    )
                }
                Spacer(Modifier.width(defaultPadding))

                Button(onClick = { onSaveClick(createNoteCategoryObject()) }) {
                    Text(text = "Save")
                }
            }
            Spacer(Modifier.height(defaultPadding))
            OutlinedTextField(
                value = name,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
            )
        }
    }

    BackHandler(enabled = hasChanged.value) {
        // TODO: Are you sure you want to go back?
    }
}