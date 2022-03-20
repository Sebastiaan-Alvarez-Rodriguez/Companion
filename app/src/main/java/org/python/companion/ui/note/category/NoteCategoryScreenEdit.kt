package org.python.companion.ui.note.category

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.flow.Flow
import org.python.backend.data.datatype.NoteCategory
import org.python.companion.R
import org.python.companion.support.LoadState
import org.python.companion.support.UiUtil
import org.python.companion.viewmodels.NoteCategoryViewModel
import timber.log.Timber


/** Loads note to edit, then shows edit screen. */
@Composable
fun NoteCategoryScreenEdit(
    noteCategoryViewModel: NoteCategoryViewModel,
    id: Long,
    overrideDialogMiniState: NoteCategoryOverrideDialogMiniState,
    onCategoryClick: () -> Unit,
    onSaveClick: (NoteCategory, NoteCategory?) -> Unit,
    onOverrideAcceptClick: (NoteCategory, NoteCategory?) -> Unit
) {
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
            overrideDialogMiniState = overrideDialogMiniState,
            onCategoryClick = onCategoryClick,
            onSaveClick = { toSaveNoteCategory -> onSaveClick(toSaveNoteCategory, existingData) },
            onOverrideAcceptClick = { toSaveNoteCategory -> onOverrideAcceptClick(toSaveNoteCategory, existingData?)}
        )
        LoadState.STATE_FAILED -> {
            Timber.e("Could not find note with id: $id")
            UiUtil.SimpleProblem("Could not find note with id: $id")
        }
    }
}

/** Edits a new note directly. */
@Composable
fun NoteCategoryScreenEditNew(
    overrideDialogMiniState: NoteCategoryOverrideDialogMiniState,
    onSaveClick: (NoteCategory) -> Unit,
    onCategoryClick: () -> Unit,
    onOverrideAcceptClick: (NoteCategory) -> Unit
) = NoteCategoryScreenEditReady(null, null, overrideDialogMiniState, onCategoryClick, onSaveClick, onOverrideAcceptClick)

/**
 * Detail screen for editing a single note.
 * @param noteCategory Category to edit.
 * @param overrideDialogMiniState
 * @param onSaveClick Lambda executed when the user hits the save button.
 * @param onOverrideAcceptClick Lambda executed when the user hits the override button.
 */
@Composable
fun NoteCategoryScreenEditReady(
    noteCategory: NoteCategory,
    overrideDialogMiniState: NoteCategoryOverrideDialogMiniState,
    onCategoryClick: () -> Unit,
    onSaveClick: (NoteCategory) -> Unit,
    onOverrideAcceptClick: (NoteCategory) -> Unit
) {
    Box {
        NoteCategoryScreenOverrideDialog(overrideDialogMiniState, onOverrideAcceptClick)
        NoteCategoryScreenEditBody(noteCategory, onCategoryClick, onSaveClick)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCategoryScreenEditBody(
    category: NoteCategory?,
    onSaveClick: (NoteCategory) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var color by remember { mutableStateOf(category?.color ?: NoteCategory.DEFAULT.color) }
    var favorite by remember { mutableStateOf(category?.favorite ?: false)}

    var categoryKey by remember { mutableStateOf(-1L)}


    val hasChanged = lazy {
        if (category == null)
            name != "" || color != NoteCategory.DEFAULT.color || favorite
        else
            name != category.name || color != category.color || favorite != category.favorite
    }

    val createNoteCategoryObject: () -> NoteCategory = {
        category?.copy(
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
                label = { Text("Title") },
                singleLine = true,
            )
        }
    }

    BackHandler(enabled = noteChanged.value) {
        // TODO: Are you sure you want to go back?
    }
}

/**
 * Overview screen for all categories.
 * @param categories List of categories to display.
 * @param isLoading if set, a loading screen will be displayed.
 * @param onNewClick Lambda to perform on new-category button clicks.
 * @param onCategoryClick Lambda to perform on category clicks.
 * @param onFavoriteClick Lambda to perform on note favorite clicks.
 */
@Composable
fun NoteCategoryScreenEditCategory(
    categories: Flow<PagingData<NoteCategory>>,
    isLoading: Boolean,
    onNewClick: () -> Unit,
    onCategoryClick: (NoteCategory) -> Unit,
    onFavoriteClick: (NoteCategory) -> Unit,
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    val items: LazyPagingItems<NoteCategory> = categories.collectAsLazyPagingItems()
    val listState: LazyListState = rememberLazyListState()

    Card(modifier = Modifier.fillMaxSize().padding(defaultPadding), elevation = 5.dp) {
        Box(Modifier.fillMaxSize()) {
            when {
                isLoading -> UiUtil.SimpleLoading()
                items.itemCount == 0 -> UiUtil.SimpleText("No categories yet")
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().semantics { contentDescription = "NoteCategory Screen" },
                        contentPadding = PaddingValues(defaultPadding),
                        verticalArrangement = Arrangement.spacedBy(defaultPadding),
                        state = listState,
                    ) {
                        items(items = items) { category ->
                            if (category != null)
                                CategoryItem(category, onCategoryClick, onFavoriteClick)
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = onNewClick,
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Text("+")
            }
        }
    }
}

@Composable
fun CategoryItem(category: NoteCategory, onCategoryClick: (NoteCategory) -> Unit, onFavoriteClick: (NoteCategory) -> Unit) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
        Card(elevation = 5.dp) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .padding(defaultPadding)
                    .clickable { onCategoryClick(category) }
                    .semantics(mergeDescendants = true) {},
            ) {
                Text(modifier = Modifier.weight(1f, fill = false), text = category.name)
                IconButton(onClick = { onFavoriteClick(category) }) {
                    Icon(
                        modifier = Modifier.background(Color(category.color.toArgb())),
                        imageVector = if (category.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite"
                    )
                }
            }
        }
    }

@Composable
fun NoteCategoryScreenOverrideDialog(
    overrideDialogMiniState: NoteCategoryOverrideDialogMiniState,
    onOverrideAcceptClick: (NoteCategory) -> Unit
) {
    if (overrideDialogMiniState.open.value)
        NoteCategoryOverrideDialog(
            overrideDialogMiniState.currentNoteCategory.value!!,
            overrideDialogMiniState.overriddenNoteCategory.value!!,
            {},
            { onOverrideAcceptClick(overrideDialogMiniState.overriddenNoteCategory.value!!) },
            {}
        )

    BackHandler(enabled = overrideDialogMiniState.open.value) {
        overrideDialogMiniState.close()
    }
}