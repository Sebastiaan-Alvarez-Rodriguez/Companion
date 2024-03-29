package org.python.companion.ui.note.category

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import java.time.Instant


/** Loads note category to edit, then shows edit screen. */
@Composable
fun NoteCategoryScreenEdit(
    noteCategoryViewModel: NoteCategoryViewModel,
    id: Long,
    onDeleteClick: ((NoteCategory?) -> Unit)?,
    onBackClick: (Boolean) -> Unit,
    onSaveClick: (NoteCategory, NoteCategory?) -> Unit
) {
    var state by rememberSaveable { mutableStateOf(LoadingState.LOADING) }
    var existingData by rememberSaveable { mutableStateOf<NoteCategory?>(null) }

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
            onBackClick = onBackClick,
            onSaveClick = { toSaveNoteCategory -> onSaveClick(toSaveNoteCategory, existingData) },
        )
        else -> {
            Timber.e("Could not find note category with id: $id")
            UiUtil.SimpleProblem("Could not find note category with id: $id")
        }
    }
}

/** Edits a new note directly. */
@Composable
fun NoteCategoryScreenEditNew(
    onDeleteClick: ((NoteCategory?) -> Unit)?,
    onBackClick: (Boolean) -> Unit,
    onSaveClick: (NoteCategory) -> Unit
) = NoteCategoryScreenEditReady(null, onDeleteClick, onBackClick, onSaveClick)

/**
 * Detail screen for editing a single note.
 * @param noteCategory Category to edit. `null` if we create a new one instead.
 * @param onDeleteClick: Lambda for delete operation. If `null`, cannot delete.
 * If passed category == `null`, the user creates a new noteCategory.
 * @param onBackClick Lambda executed when the user intends to go back. Parameter indicates whether the note has been edited.
 * @param onSaveClick Lambda for save operation.
 */
@Composable
fun NoteCategoryScreenEditReady(
    noteCategory: NoteCategory?,
    onDeleteClick: ((NoteCategory?) -> Unit)?,
    onBackClick: (Boolean) -> Unit,
    onSaveClick: (NoteCategory) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(noteCategory?.name ?: "") }
    var _color by rememberSaveable { mutableStateOf((noteCategory?.color ?: NoteCategory.DEFAULT.color).toArgb()) }
    var favorite by rememberSaveable { mutableStateOf(noteCategory?.favorite ?: false)}

    val color: android.graphics.Color = android.graphics.Color.valueOf(_color)
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
            favorite = favorite,
            date = Instant.now()
        ) ?:
        NoteCategory(
            name = name,
            color = color,
            favorite = favorite,
            date = Instant.now()
        )
    }

    val defaultPadding = dimensionResource(id = R.dimen.padding_default)

    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = defaultPadding).padding(top = defaultPadding)) {
            NoteCategoryItem(noteCategory = createNoteCategoryObject(), onNoteCategoryClick = {}, onFavoriteClick = { favorite = !favorite})
        }
        Card(modifier = Modifier.fillMaxWidth().padding(defaultPadding).verticalScroll(rememberScrollState()), elevation = 5.dp) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { favorite = !favorite }) {
                        Icon(
                            imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (favorite) "Stop favoring" else "Favorite"
                        )
                    }
                    if (onDeleteClick != null)
                        IconButton(onClick = { onDeleteClick(noteCategory) }) {
                            Icon(imageVector = Icons.Outlined.Delete,contentDescription = "Delete note category")
                        }
                    Spacer(Modifier.width(defaultPadding))

                    IconButton(onClick = { onSaveClick(createNoteCategoryObject()) }) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = "Save")
                    }
                }
                Spacer(Modifier.height(defaultPadding))
                OutlinedTextField(
                    value = name,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = defaultPadding),
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                )

                Spacer(Modifier.height(defaultPadding))
                UiUtil.SimpleColorPick(color = color, onColorUpdate = { _color = it.toArgb() })
            }
        }
    }

    BackHandler(enabled = true) {
        onBackClick(hasChanged.value)
    }
}