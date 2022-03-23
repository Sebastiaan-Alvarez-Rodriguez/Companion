package org.python.companion.ui.note

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.flow.Flow
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteCategory
import org.python.backend.data.datatype.NoteWithCategory
import org.python.companion.R
import org.python.companion.support.UiUtil


@Composable
fun NoteScreen(
    noteScreenListHeaderStruct: NoteScreenListHeaderStruct,
    noteScreenListStruct: NoteScreenListStruct,
) = NoteScreenList(noteScreenListHeaderStruct, noteScreenListStruct)

@Composable
fun NoteScreenList(
    noteScreenListHeaderStruct: NoteScreenListHeaderStruct,
    noteScreenListStruct: NoteScreenListStruct,
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Column(modifier = Modifier.padding(defaultPadding)) {
        NoteScreenListHeader(noteScreenListHeaderStruct)
        Spacer(modifier = Modifier.height(defaultPadding))
        NoteScreenList(noteScreenListStruct)
    }
}

class NoteScreenListHeaderStruct(val onSettingsClick: () -> Unit, val onSearchClick: () -> Unit)

@Composable
fun NoteScreenListHeader(noteScreenListHeaderStruct: NoteScreenListHeaderStruct) =
    NoteScreenListHeader(
        onSettingsClick = noteScreenListHeaderStruct.onSettingsClick,
        onSearchClick = noteScreenListHeaderStruct.onSearchClick
    )

@Composable
fun NoteScreenListHeader(onSettingsClick: () -> Unit, onSearchClick: () -> Unit) {
    val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)

    Card(elevation = 5.dp) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(modifier = Modifier.padding(tinyPadding), onClick = { onSettingsClick() }) {
                Icon(Icons.Filled.Settings, "Settings")
            }
            Spacer(modifier = Modifier.width(defaultPadding))
            IconButton(
                modifier = Modifier.padding(tinyPadding),
                onClick = { onSearchClick() }) {
                Icon(Icons.Filled.Search, "Search")
            }
        }
    }
}

class NoteScreenListStruct(
    val notes: Flow<PagingData<NoteWithCategory>>,
    val isLoading: Boolean,
    val onNewClick: () -> Unit,
    val onNoteClick: (Note) -> Unit,
    val onFavoriteClick: (Note) -> Unit,
    val securityStruct: NoteScreenListSecurityStruct?
)

class NoteScreenListSecurityStruct(
    val securityText: String,
    val onSecurityClick: () -> Unit
)
@Composable
fun NoteScreenList(noteScreenListStruct: NoteScreenListStruct) =
    NoteScreenList(
        notes = noteScreenListStruct.notes,
        isLoading = noteScreenListStruct.isLoading,
        onNewClick = noteScreenListStruct.onNewClick,
        onNoteClick = noteScreenListStruct.onNoteClick,
        onFavoriteClick = noteScreenListStruct.onFavoriteClick,
        securityStruct = noteScreenListStruct.securityStruct
    )

/**
 * Overview screen for all notes.
 * @param notes List of notes to display, with an optional category.
 * @param isLoading If set, displays a loading screen.
 * @param onNewClick Lambda to perform on new-note button clicks.
 * @param onNoteClick Lambda to perform on note clicks.
 * @param onFavoriteClick Lambda to perform on note favorite clicks.
 * @param securityStruct Security item state struct. Pass **null** to hide security item.
 */
@Composable
fun NoteScreenList(
    notes: Flow<PagingData<NoteWithCategory>>,
    isLoading: Boolean,
    onNewClick: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onFavoriteClick: (Note) -> Unit,
    securityStruct: NoteScreenListSecurityStruct?
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
//    TODO: Maybe add sticky headers: https://developer.android.com/jetpack/compose/lists
    val items: LazyPagingItems<NoteWithCategory> = notes.collectAsLazyPagingItems()
    val listState: LazyListState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
        when {
            isLoading -> UiUtil.SimpleLoading()
            items.itemCount == 0 && securityStruct == null -> UiUtil.SimpleText("No notes yet")
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().semantics { contentDescription = "Note Screen" },
                    verticalArrangement = Arrangement.spacedBy(defaultPadding),
                    state = listState,
                ) {
                    if (securityStruct != null) {
                        item {
                            SecurityClickItem(securityStruct)
                        }
                    }
                    items(items = items) { noteWithCategory ->
                        if (noteWithCategory != null)
                            NoteItem(noteWithCategory.note, noteWithCategory.noteCategory, onNoteClick, onFavoriteClick)
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

/**
 * Composition for a single note item.
 * @param note Note to display.
 * @param noteCategory Optional note category.
 * @param onNoteClick Lambda to perform on note clicks.
 * @param onFavoriteClick Lambda to perform on note favorite clicks.
 */
@Composable
fun NoteItem(
    note: Note,
    noteCategory: NoteCategory? = null,
    onNoteClick: (Note) -> Unit,
    onFavoriteClick: (Note) -> Unit
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Card(
        elevation = 5.dp,
        border = BorderStroke(width = 1.dp, Color(noteCategory?.color?.toArgb() ?: Color.Transparent.toArgb())),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .padding(defaultPadding)
                .clickable { onNoteClick(note) }
                // Regard the whole row as one semantics node. This way each row will receive focus as
                // a whole and the focus bounds will be around the whole row content. The semantics
                // properties of the descendants will be merged. If we'd use clearAndSetSemantics instead,
                // we'd have to define the semantics properties explicitly.
                .semantics(mergeDescendants = true) {},
        ) {
            Checkbox(checked = false, onCheckedChange = {}) // TODO: Handle checkbox behaviour
            Text(modifier = Modifier.weight(1f, fill = false), text = note.name)
            IconButton(onClick = { onFavoriteClick(note) }) {
                Icon(
                    imageVector = if (note.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite"
                )
            }
        }
    }
}

@Composable
fun SecurityClickItem(securityStruct: NoteScreenListSecurityStruct) =
    SecurityClickItem(
        text = securityStruct.securityText,
        onClick = securityStruct.onSecurityClick,
    )
@Composable
fun SecurityClickItem(text: String, onClick: () -> Unit) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Card(elevation = 5.dp) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(defaultPadding)
                .clickable { onClick() }
                .fillMaxWidth()
        ) {
            Text(text)
        }
    }
}