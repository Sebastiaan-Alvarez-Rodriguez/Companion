package org.python.companion.ui.note.category

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.python.backend.data.datatype.NoteCategory
import org.python.companion.R
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.SimpleFAB
import org.python.db.entities.note.RoomNoteCategory


@Composable
fun NoteCategoryScreen(
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
fun NoteCategoryScreenListHeader(sortParameters: NoteCategorySortParameters, message: String? = null, onSearchClick: () -> Unit, onSortClick: (NoteCategorySortParameters) -> Unit) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    UiUtil.GenericListHeader(
        listOf(
            message?.let {
                {
                    Text(text = message, modifier = Modifier.padding(horizontal = defaultPadding))
                }
            } ?: {},
            {
                Row {
                    NoteCategorySort(sortParameters, onSortClick)
                    IconButton(onClick = { onSearchClick() }) {
                        Icon(Icons.Filled.Search, "Search")
                    }
                }
            }
        )
    )
}

@Composable
fun NoteCategoryScreenListRadio(
    noteCategories: Flow<PagingData<NoteCategory>>,
    selectedItem: NoteCategory?,
    isLoading: Boolean,
    onNewClick: () -> Unit,
    onNoteCategoryClick: (NoteCategory) -> Unit,
    onSelectClick: (NoteCategory) -> Unit,
    onFavoriteClick: (NoteCategory) -> Unit
) {
    UiUtil.GenericList(
        items = noteCategories,
        isLoading = isLoading,
        showItemFunc = { item -> NoteCategoryItemRadio(item, onNoteCategoryClick, onSelectClick, onFavoriteClick, selected = selectedItem == item) },
        fab = { SimpleFAB(onClick = onNewClick) }
    )
}


/**
 * Composition for a single noteCategory item.
 * @param onSelectClick Lambda to perform on radiobutton clicks.
 *  @see NoteCategoryItem(noteCategory, onNoteCategoryClick, onFavoriteClick)
 */
@Composable
fun NoteCategoryItemRadio(
    noteCategory: NoteCategory,
    onNoteCategoryClick: (NoteCategory) -> Unit,
    onSelectClick: (NoteCategory) -> Unit,
    onFavoriteClick: (NoteCategory) -> Unit,
    selected: Boolean
) {
    NoteCategoryItem(
        noteCategory = noteCategory,
        onNoteCategoryClick = onNoteCategoryClick,
        onFavoriteClick = onFavoriteClick,
        selectorBox = { RadioButton(selected = selected, onClick = { onSelectClick(noteCategory) }) }
    )
}

/**
 * Composition for a single noteCategory item.
 * @param noteCategory NoteCategory to display.
 * @param onNoteCategoryClick Lambda to perform on noteCategory clicks.
 * @param onFavoriteClick Lambda to perform on noteCategory favorite clicks.
 * @param selectorBox Optional box rendered left of the item to display selections.
 */
@Composable
fun NoteCategoryItem(
    noteCategory: NoteCategory,
    onNoteCategoryClick: (NoteCategory) -> Unit,
    onFavoriteClick: (NoteCategory) -> Unit,
    selectorBox: @Composable (RowScope.() -> Unit)? = null
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)

    Card(border = BorderStroke(width = 1.dp, Color(noteCategory.color.toArgb())), elevation = 5.dp) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { onNoteCategoryClick(noteCategory) }.semantics(mergeDescendants = true) {},
        ) {
            selectorBox?.let { it() }
            Text(modifier = Modifier.weight(1f, fill = false).padding(horizontal = defaultPadding), text = noteCategory.name)
            IconButton(onClick = { onFavoriteClick(noteCategory) }) {
                Icon(
                    imageVector = if (noteCategory.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite"
                )
            }
        }
    }
}

@Composable
fun NoteCategoryScreenSearchListHeader(searchParameters: NoteCategorySearchParameters, onBack: () -> Unit, onUpdate: (NoteCategorySearchParameters) -> Unit) {
    UiUtil.GenericListHeader(
        listOf {
            NoteCategorySearch(
                searchParameters = searchParameters,
                onQueryUpdate = onUpdate,
            )
        }
    )
    BackHandler(true) {
        onBack()
    }
}


data class NoteCategorySortParameters(
    val column: RoomNoteCategory.Companion.SortableField,
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
        private const val sortKey = "noteCategorySortParameters"
        private const val columnKey = "${sortKey}Column"
        private const val ascendingKey = "${sortKey}Ascending"

        const val DEFAULT_SORT_COLUMNNAME = 0
        const val DEFAULT_ASCENDING = true
        fun fromPreferences(activity: Activity) = fromPreferences(activity.baseContext)
        fun fromPreferences(context: Context): NoteCategorySortParameters {
            val sharedPreferences: SharedPreferences = context.getSharedPreferences(sortKey, Context.MODE_PRIVATE)
            return NoteCategorySortParameters(
                column = RoomNoteCategory.Companion.SortableField.values()[sharedPreferences.getInt(columnKey, DEFAULT_SORT_COLUMNNAME)],
                ascending = sharedPreferences.getBoolean(ascendingKey, DEFAULT_ASCENDING)
            )
        }
    }
}

data class NoteCategorySearchParameters(
    val text: String = "",
    val regex: Boolean = false,
    val caseSensitive: Boolean = false
)

@Composable
fun NoteCategorySort(sortParameters: NoteCategorySortParameters, onSortClick: (NoteCategorySortParameters) -> Unit, modifier: Modifier = Modifier) {
    IconButton(modifier = modifier, onClick = { onSortClick(
        sortParameters.copy(ascending = !sortParameters.ascending)
    ) }) {
        Icon(if (sortParameters.ascending) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward, contentDescription = "Sort direction ${if (sortParameters.ascending) "ascending" else "descending"}")
    }
    IconButton(modifier = modifier, onClick = { onSortClick(
        sortParameters.copy(
            column = when(sortParameters.column) {
                RoomNoteCategory.Companion.SortableField.NAME -> RoomNoteCategory.Companion.SortableField.DATE
                RoomNoteCategory.Companion.SortableField.DATE -> RoomNoteCategory.Companion.SortableField.NAME
            }
        )
    ) }) {
        UiUtil.NestedIcon(
            mainIcon = when(sortParameters.column) {
                RoomNoteCategory.Companion.SortableField.NAME -> Icons.Rounded.Title
                RoomNoteCategory.Companion.SortableField.DATE -> Icons.Rounded.Schedule
            },
            description = "Sort on ${sortParameters.column}",
            sideIcon = Icons.Rounded.Sort,
            sideModifier = Modifier.size(10.dp)
        )
    }
}

@Composable
fun NoteCategorySearch(searchParameters: NoteCategorySearchParameters, onQueryUpdate: (NoteCategorySearchParameters) -> Unit) {
    val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)
    val focusRequester = remember { FocusRequester() }

    Column {
        TextField(
            value = searchParameters.text,
            onValueChange = { onQueryUpdate(searchParameters.copy(text = it)) },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
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
                    onCheckedChange = { onQueryUpdate(searchParameters.copy(regex = !searchParameters.regex)) }
                )
            }
            Column {
                UiUtil.LabelledCheckBox(
                    checked = searchParameters.caseSensitive,
                    label = "Case sensitive",
                    onCheckedChange = { onQueryUpdate(searchParameters.copy(caseSensitive = !searchParameters.caseSensitive)) }
                )
            }
        }
    }
}