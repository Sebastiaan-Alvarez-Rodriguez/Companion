package org.python.companion.ui.note.category

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import org.python.backend.data.datatype.NoteCategory
import org.python.companion.R
import org.python.companion.support.LoadingState
import org.python.companion.support.UiUtil
import org.python.companion.viewmodels.NoteCategoryViewModel
import timber.log.Timber


/** Loads notecategory to edit, then shows edit screen. */
@Composable
fun NoteCategoryScreenEdit(
    noteCategoryViewModel: NoteCategoryViewModel,
    id: Long,
    onDeleteClick: ((NoteCategory?) -> Unit)?,
    onSaveClick: (NoteCategory, NoteCategory?) -> Unit) {
    var state by remember { mutableStateOf(LoadingState.LOADING) }
    var existingData by remember { mutableStateOf<NoteCategory?>(null) }

    when (state) {
        LoadingState.LOADING -> if (existingData == null) {
            UiUtil.SimpleLoading()
            LaunchedEffect(state) {
                existingData = noteCategoryViewModel.get(id)
                state = LoadingState.READY
            }
        }
        LoadingState.READY -> NoteCategoryScreenEditReady(
            noteCategory = existingData,
            onDeleteClick = onDeleteClick,
            onSaveClick = { toSaveNoteCategory -> onSaveClick(toSaveNoteCategory, existingData) }
        )
        LoadingState.FAILED -> {
            Timber.e("Could not find note with id: $id")
            UiUtil.SimpleProblem("Could not find note with id: $id")
        }
    }
}

/** Edits a new note directly. */
@Composable
fun NoteCategoryScreenEditNew(
    onDeleteClick: ((NoteCategory?) -> Unit)?,
    onSaveClick: (NoteCategory) -> Unit
) = NoteCategoryScreenEditReady(null, onDeleteClick, onSaveClick)

/**
 * Detail screen for editing a single note.
 * @param noteCategory Category to edit. `null` if we create a new one instead.
 * @param onDeleteClick: Lambda for delete operation. If `null`, cannot delete.
 * If passed category == `null`, the user creates a new noteCategory.
 * @param onSaveClick Lambda for save operation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCategoryScreenEditReady(
    noteCategory: NoteCategory?,
    onDeleteClick: ((NoteCategory?) -> Unit)?,
    onSaveClick: (NoteCategory) -> Unit) {
    var name by remember { mutableStateOf(noteCategory?.name ?: "") }
    var color by remember { mutableStateOf(noteCategory?.color ?: NoteCategory.DEFAULT.color) }
    var favorite by remember { mutableStateOf(noteCategory?.favorite ?: false)}

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

    Column {
        NoteCategoryItem(noteCategory = createNoteCategoryObject(), onNoteCategoryClick = {}, onFavoriteClick = { favorite = !favorite})
        Spacer(modifier = Modifier.height(defaultPadding))
        Card(modifier = Modifier.fillMaxWidth().padding(defaultPadding).verticalScroll(rememberScrollState()), elevation = 5.dp) {
            Column(modifier = Modifier.fillMaxSize().padding(defaultPadding)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        modifier = Modifier.padding(smallPadding),
                        onClick = { favorite = !favorite }) {
                        Icon(
                            imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (favorite) "Stop favoring" else "Favorite"
                        )
                    }
                    if (onDeleteClick != null)
                        IconButton(onClick = { onDeleteClick(noteCategory) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete note category"
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

                Spacer(Modifier.height(defaultPadding))
                UiUtil.SimpleColorPick(color = color, onColorUpdate = { color = it })
            }
        }
    }

    BackHandler(enabled = hasChanged.value) {
        // TODO: Are you sure you want to go back?
    }
}