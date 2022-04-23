package org.python.companion.ui.note.category

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.python.backend.data.datatype.NoteCategory
import org.python.companion.R
import org.python.companion.support.LoadingState
import org.python.companion.support.UiUtil
import org.python.companion.viewmodels.NoteCategoryViewModel
import timber.log.Timber
import java.lang.RuntimeException


/** Loads notecategory to edit, then shows edit screen. */
@Composable
fun NoteCategoryScreenEdit(
    noteCategoryViewModel: NoteCategoryViewModel,
    id: Long,
    onDeleteClick: ((NoteCategory?) -> Unit)?,
    onSaveClick: (NoteCategory, NoteCategory?) -> Unit,
    navController: NavController
) {
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
            onSaveClick = { toSaveNoteCategory -> onSaveClick(toSaveNoteCategory, existingData) },
            navController = navController
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
    onSaveClick: (NoteCategory) -> Unit,
    navController: NavController
) = NoteCategoryScreenEditReady(null, onDeleteClick, onSaveClick, navController)

/**
 * Detail screen for editing a single note.
 * @param noteCategory Category to edit. `null` if we create a new one instead.
 * @param onDeleteClick: Lambda for delete operation. If `null`, cannot delete.
 * If passed category == `null`, the user creates a new noteCategory.
 * @param onSaveClick Lambda for save operation.
 * @param navController Used to handle back events.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCategoryScreenEditReady(
    noteCategory: NoteCategory?,
    onDeleteClick: ((NoteCategory?) -> Unit)?,
    onSaveClick: (NoteCategory) -> Unit,
    navController: NavController
) {
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

    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = defaultPadding)) {
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
                UiUtil.SimpleColorPick(color = color, onColorUpdate = { color = it })
            }
        }
    }

    val showGoBack = remember { mutableStateOf(false) }
    BackHandler(enabled = hasChanged.value) {
        showGoBack.value = !showGoBack.value
    }
    if (showGoBack.value)
        UiUtil.SimpleDialogBinary(
            message = "Found unsaved changes. Are you sure you want to go back?",
            onDismiss = { showGoBack.value = false },
            onNegativeClick = { showGoBack.value = false },
            onPositiveClick = { navController.navigateUp() },
        )
}