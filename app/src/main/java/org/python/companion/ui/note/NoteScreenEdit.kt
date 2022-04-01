package org.python.companion.ui.note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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
fun NoteScreenEdit(
    noteViewModel: NoteViewModel,
    id: Long,
    onCategoryClick: () -> Unit,
    onSaveClick: (Note, Note?) -> Unit,
) {
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
            onCategoryClick = onCategoryClick,
            onSaveClick = { toSaveNote -> onSaveClick(toSaveNote, existingData?.note) },
        )
        LoadingState.FAILED -> {
            Timber.e("Could not find note with id: $id")
            UiUtil.SimpleProblem("Could not find note with id: $id")
        }
    }
}

/** Edits a new note directly. */
@Composable
fun NoteScreenEditNew(onSaveClick: (Note) -> Unit, onCategoryClick: () -> Unit) = NoteScreenEditReady(null, null, onCategoryClick, onSaveClick)

/**
 * Detail screen for editing a single note.
 * @param note Note to edit.
 * @param noteCategory Optional category assigned to passed note.
 * @param onSaveClick Lambda executed when the user hits the save button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteScreenEditReady(
    note: Note?,
    noteCategory: NoteCategory?,
    onCategoryClick: () -> Unit,
    onSaveClick: (Note) -> Unit,
) {
    var title by remember { mutableStateOf(note?.name ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var favorite by remember { mutableStateOf(note?.favorite ?: false)}
    var secure by remember { mutableStateOf(note?.secure ?: false)}

    var categoryKey by remember { mutableStateOf(noteCategory?.categoryId ?: NoteCategory.DEFAULT.categoryId)}
//    var categoryName by remember { mutableStateOf(noteCategory?.name ?: "") }
    var categoryColor by remember { mutableStateOf(noteCategory?.color ?: NoteCategory.DEFAULT.color) }

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

    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    val smallPadding = dimensionResource(id = R.dimen.padding_small)

    Card(modifier = Modifier.fillMaxSize().padding(defaultPadding), elevation = 5.dp) {
        Column(modifier = Modifier.fillMaxSize().padding(defaultPadding)) {
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
                    IconButton(modifier = Modifier.padding(smallPadding), onClick = onCategoryClick) {
                        Icon(
                            tint = Color(categoryColor.toArgb()),
                            imageVector = Icons.Outlined.Article,
                            contentDescription = "Edit category")
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
fun NoteScreenEditCategory(
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
                        modifier = Modifier.fillMaxSize().semantics { contentDescription = "Note Screen" },
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