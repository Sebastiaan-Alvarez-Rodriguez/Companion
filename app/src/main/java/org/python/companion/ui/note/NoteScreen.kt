package org.python.companion.ui.note

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteWithCategory
import org.python.companion.R
import org.python.companion.support.*
import org.python.companion.support.UiUtil.SimpleFAB
import org.python.db.entities.note.RoomNoteWithCategory


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
fun NoteScreenListHeader(
    sortParameters: NoteSortParameters,
    onSettingsClick: () -> Unit,
    onSortClick: (NoteSortParameters) -> Unit,
    onSearchClick: () -> Unit
) {
    val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)

    UiUtil.GenericListHeader(
        listOf(
            {
                IconButton(modifier = Modifier.padding(tinyPadding), onClick = onSettingsClick) {
                    Icon(Icons.Filled.Settings, "Settings")
                }
            },
            {
                Row {
                    NoteSort(sortParameters, modifier = Modifier.padding(tinyPadding), onSortClick = onSortClick)
                    IconButton(modifier = Modifier.padding(tinyPadding), onClick = { onSearchClick() }) {
                        Icon(Icons.Filled.Search, "Search")
                    }
                }
            }
        )
    )
}

@Composable
fun NoteScreenSearchListHeader(searchParameters: NoteSearchParameters, onBack: () -> Unit, onUpdate: (NoteSearchParameters) -> Unit) {
    UiUtil.GenericListHeader(
        listOf {
            NoteSearch(
                searchParameters = searchParameters,
                onQueryUpdate = onUpdate,
            )
        }
    )
    BackHandler(true) {
        onBack()
    }
}

@Composable
fun NoteScreenContextListHeader(
    sortParameters: NoteSortParameters,
    onDeleteClick: () -> Unit,
    onSortClick: (NoteSortParameters) -> Unit,
    onSearchClick: () -> Unit
) {
    val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)
    UiUtil.GenericListHeader(
        listOf(
            {
                IconButton(modifier = Modifier.padding(tinyPadding), onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, "Delete")
                }
            },
            {
                Row {
                    NoteSort(sortParameters, modifier = Modifier.padding(tinyPadding), onSortClick = onSortClick)
                    IconButton(modifier = Modifier.padding(tinyPadding), onClick = { onSearchClick() }) {
                        Icon(Icons.Filled.Search, "Search")
                    }
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
 * @param rendererCache Optional collection of renderers to use.
 * @param drawCache Optional collection of drawCaches to use.
 * If not set, re-renders every recomposition for every item.
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
    securityItem: (@Composable LazyItemScope.() -> Unit)? = null,
    rendererCache: RendererCache? = null,
    drawCache: DrawCache<Long>? = null
) {
    UiUtil.GenericList(
        prefix = securityItem,
        items = notes,
        isLoading = isLoading,
        showItemFunc = {
            item -> NoteItem(item, onNoteClick, onCheckClick, onFavoriteClick, selected = selectedItems.contains(item.note), rendererCache, drawCache?.getOrDefaultPut(item.note.noteId, ItemDrawCache()))
       },
        fab = { SimpleFAB(onClick = onNewClick) }
    )
}

/**
 * Composition for a single note item.
 * @param item Note to display, with its category.
 * @param onNoteClick Lambda to perform on note clicks.
 * @param onCheckClick Lambda to perform on checkbox clicks.
 * @param onFavoriteClick Lambda to perform on note favorite clicks.
 * @param selected If set, marks current item as selected.
 * @param rendererCache Optional rendererCache to use. If not set, uses a new renderer for each new item.
 * @param itemDrawCache Optional drawCache to use. If not set, re-renders every recomposition.
 */
@Composable
fun NoteItem(
    item: NoteWithCategory,
    onNoteClick: (NoteWithCategory) -> Unit,
    onCheckClick: (NoteWithCategory, Boolean) -> Unit,
    onFavoriteClick: (NoteWithCategory) -> Unit,
    selected: Boolean,
    rendererCache: RendererCache? = null,
    itemDrawCache: ItemDrawCache? = null
) {
    val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)
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
                .clickable { onNoteClick(item) }
                .semantics(mergeDescendants = true) {},
        ) {
            Checkbox(modifier = Modifier.weight(0.1f, fill = false), checked = selected, onCheckedChange = { nowChecked -> onCheckClick(item, nowChecked)})
            RenderUtil.RenderText(
                text = item.note.name,
                modifier = Modifier.weight(0.8f, fill = false),
                renderType = item.note.renderType,
                rendererCache = rendererCache,
                itemDrawCache = itemDrawCache
            )
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.1f, fill = true)
            ) {
                IconButton(onClick = { onFavoriteClick(item) }) {
                    Icon(
                        imageVector = if (item.note.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite"
                    )
                }
                if (item.note.securityLevel > 0)
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Protected",
                        modifier = Modifier
                            .size(width = 12.dp, height = 12.dp)
                            .padding(end = tinyPadding, bottom = tinyPadding)
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

data class NoteSortParameters(
    val column: RoomNoteWithCategory.Companion.SortableField,
    val ascending: Boolean = true
) {

    fun toPreferences(activity: Activity) = toPreferences(activity.baseContext)
    fun toPreferences(context: Context) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(sortKey, Context.MODE_PRIVATE)
        val preferencesEditor = sharedPreferences.edit()
        preferencesEditor.putInt(columnKey, column.ordinal)
        preferencesEditor.putBoolean(ascendingKey, ascending)
        preferencesEditor.apply()
    }

    companion object {
        private const val sortKey = "noteSortParameters"
        private const val columnKey = "${sortKey}Column"
        private const val ascendingKey = "${sortKey}Ascending"

        const val DEFAULT_SORT_COLUMNNAME = 0
        const val DEFAULT_ASCENDING = true
        fun fromPreferences(activity: Activity) = fromPreferences(activity.baseContext)
        fun fromPreferences(context: Context): NoteSortParameters {
            val sharedPreferences: SharedPreferences = context.getSharedPreferences(sortKey, Context.MODE_PRIVATE)
            return NoteSortParameters(
                column = RoomNoteWithCategory.Companion.SortableField.values()[sharedPreferences.getInt(columnKey, DEFAULT_SORT_COLUMNNAME)],
                ascending = sharedPreferences.getBoolean(ascendingKey, DEFAULT_ASCENDING)
            )
        }
    }
}

data class NoteSearchParameters(
    val text: String = "",
    val inTitle: Boolean = true,
    val inContent: Boolean = false,
    val regex: Boolean = false,
    val caseSensitive: Boolean = false
)

@Composable
fun NoteSort(sortParameters: NoteSortParameters, onSortClick: (NoteSortParameters) -> Unit, modifier: Modifier = Modifier) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    IconButton(modifier = modifier, onClick = { onSortClick(
        sortParameters.copy(ascending = !sortParameters.ascending)
    ) }) {
         Icon(
             if (sortParameters.ascending) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
             contentDescription = "Sort direction ${if (sortParameters.ascending) "ascending" else "descending"}"
         )
    }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(modifier = modifier, onClick = { sortMenuExpanded = !sortMenuExpanded }) {
            UiUtil.NestedIcon(
                mainIcon = when(sortParameters.column) {
                    RoomNoteWithCategory.Companion.SortableField.NAME -> Icons.Rounded.Title
                    RoomNoteWithCategory.Companion.SortableField.DATE -> Icons.Rounded.Schedule
                    RoomNoteWithCategory.Companion.SortableField.CATEGORYNAME -> Icons.Rounded.Bolt
                    RoomNoteWithCategory.Companion.SortableField.SECURITYLEVEL -> Icons.Rounded.Lock
                },
                description = "Sort on ${sortParameters.column}",
                sideIcon = Icons.Rounded.Sort,
                sideModifier = Modifier.size(10.dp)
            )
        }
        DropdownMenu(
            expanded = sortMenuExpanded,
            onDismissRequest = { sortMenuExpanded = false },
            modifier = modifier.align(Center)
        ) {
            for (value in RoomNoteWithCategory.Companion.SortableField.values()) {
                if (value != sortParameters.column) {
                    IconButton(modifier = modifier, onClick = { onSortClick(sortParameters.copy(column = value)) }) {
                        UiUtil.NestedIcon(
                            mainIcon = when (value) {
                                RoomNoteWithCategory.Companion.SortableField.NAME -> Icons.Rounded.Title
                                RoomNoteWithCategory.Companion.SortableField.DATE -> Icons.Rounded.Schedule
                                RoomNoteWithCategory.Companion.SortableField.CATEGORYNAME -> Icons.Rounded.Bolt
                                RoomNoteWithCategory.Companion.SortableField.SECURITYLEVEL -> Icons.Rounded.Lock
                            },
                            description = "Sort on $value",
                            sideIcon = Icons.Rounded.Sort,
                            sideModifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoteSearch(searchParameters: NoteSearchParameters, onQueryUpdate: (NoteSearchParameters) -> Unit) {
    val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)
    val focusRequester = remember { FocusRequester() }

    Column {
        TextField(
            value = searchParameters.text,
            onValueChange = { onQueryUpdate(searchParameters.copy(text = it)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", modifier = Modifier.padding(tinyPadding)) },
            trailingIcon = {
                if (searchParameters.text.isNotEmpty()) {
                    IconButton( onClick = { onQueryUpdate(searchParameters.copy(text = "")) }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "reset", modifier = Modifier.padding(tinyPadding))
                    }
                }
            }
        )
        UiUtil.LaunchedEffectSaveable(Unit) { // We want to take focus only once per search
            focusRequester.requestFocus()
        }

        Row {
            Column(modifier = Modifier.fillMaxWidth(0.5f)) {
                UiUtil.LabelledCheckBox(
                    checked = searchParameters.regex,
                    label = "Regex",
                    onCheckedChange = { onQueryUpdate(searchParameters.copy(regex = !searchParameters.regex))}
                )
                UiUtil.LabelledCheckBox(
                    checked = searchParameters.inTitle,
                    label = "Search in title",
                    onCheckedChange = { onQueryUpdate(searchParameters.copy(inTitle = !searchParameters.inTitle))}
                )
            }
            Column {
                UiUtil.LabelledCheckBox(
                    checked = searchParameters.caseSensitive,
                    label = "Case sensitive",
                    onCheckedChange = { onQueryUpdate(searchParameters.copy(caseSensitive = !searchParameters.caseSensitive))}
                )
                UiUtil.LabelledCheckBox(
                    checked = searchParameters.inContent,
                    label = "Search in contents",
                    onCheckedChange = { onQueryUpdate(searchParameters.copy(inContent = !searchParameters.inContent))}
                )
            }
        }
    }
}