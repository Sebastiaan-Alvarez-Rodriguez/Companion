package org.python.companion.ui.note

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteWithCategory
import org.python.companion.R
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.SimpleFAB


@Composable
fun NoteScreen(
    header: @Composable (ColumnScope.() -> Unit)? = null,
    list: @Composable ColumnScope.() -> Unit
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Column(modifier = Modifier.padding(defaultPadding)) {
        if (header != null)
            header()
        Spacer(modifier = Modifier.height(defaultPadding))
        list()
    }
}

@Composable
fun NoteScreenListHeader(onSettingsClick: () -> Unit, onSearchClick: () -> Unit) {
    val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)
    UiUtil.GenericListHeader(
        listOf(
            {
                IconButton(modifier = Modifier.padding(tinyPadding), onClick = onSettingsClick) {
                    Icon(Icons.Filled.Settings, "Settings")
                }
            },
            {
                IconButton(modifier = Modifier.padding(tinyPadding), onClick = onSearchClick) {
                    Icon(Icons.Filled.Search, "Search")
                }
            }
        )
    )
}

@Composable
fun NoteScreenContextListHeader(onDeleteClick: () -> Unit, onSearchClick: () -> Unit) {
    val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)
    UiUtil.GenericListHeader(
        listOf(
            {
                IconButton(modifier = Modifier.padding(tinyPadding), onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, "Delete")
                }
            },
            {
                IconButton(modifier = Modifier.padding(tinyPadding), onClick = { onSearchClick() }) {
                    Icon(Icons.Filled.Search, "Search")
                }
            }
        )
    )
}

/**
 * Overview screen for all notes.
 * @param notes List of notes to display, with their category.
 * @param selectedItems Notes currently selected.
 * @param isLoading If set, displays a loading screen.
 * @param onNewClick Lambda to perform on new-note button clicks.
 * @param onNoteClick Lambda to perform on note clicks.
 * @param onCheckClick Lambda to perform on checkbox clicks.
 * @param onFavoriteClick Lambda to perform on note favorite clicks.
 * @param securityItem Security item to show as first list item.
 */
@Composable
fun NoteScreenList(
    notes: Flow<PagingData<NoteWithCategory>>,
    selectedItems: List<Note>,
    isLoading: Boolean,
    onNewClick: () -> Unit,
    onNoteClick: (NoteWithCategory) -> Unit,
    onCheckClick: (NoteWithCategory, Boolean) -> Unit,
    onFavoriteClick: (NoteWithCategory) -> Unit,
    securityItem: (@Composable LazyItemScope.() -> Unit)? = null
) {
    UiUtil.GenericList(
        prefix = securityItem,
        items = notes,
        isLoading = isLoading,
        showItemFunc = { item -> NoteItem(item, onNoteClick, onCheckClick, onFavoriteClick, selected = selectedItems.contains(item.note)) },
        fab = { SimpleFAB(onClick = onNewClick) }
    )
}

/**
 * Composition for a single note item.
 * @param item Note to display, with its category.
 * @param onNoteClick Lambda to perform on note clicks.
 * @param onCheckClick Lambda to perform on checkbox clicks.
 * @param onFavoriteClick Lambda to perform on note favorite clicks.
 */
@Composable
fun NoteItem(
    item: NoteWithCategory,
    onNoteClick: (NoteWithCategory) -> Unit,
    onCheckClick: (NoteWithCategory, Boolean) -> Unit,
    onFavoriteClick: (NoteWithCategory) -> Unit,
    selected: Boolean,
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Card(
        elevation = 5.dp,
        border = BorderStroke(width = 1.dp, Color(item.noteCategory.color.toArgb())),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(defaultPadding)
                .clickable { onNoteClick(item) }
                .semantics(mergeDescendants = true) {},
        ) {
            Checkbox(checked = selected, onCheckedChange = { nowChecked -> onCheckClick(item, nowChecked)})
            Text(modifier = Modifier.weight(1f, fill = false), text = item.note.name)
            IconButton(onClick = { onFavoriteClick(item) }) {
                Icon(
                    imageVector = if (item.note.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite"
                )
            }
        }
    }
}

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