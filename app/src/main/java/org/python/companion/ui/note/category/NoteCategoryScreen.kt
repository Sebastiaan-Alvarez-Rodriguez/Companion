package org.python.companion.ui.note.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
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
import org.python.backend.data.datatype.NoteCategory
import org.python.companion.R
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.SimpleFAB


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
fun NoteCategoryScreenListHeader(message: String? = null, onSearchClick: () -> Unit) {
    val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)
    UiUtil.GenericListHeader(
        listOf(
            message?.let {
                {
                    Text(text = message, modifier = Modifier.padding(horizontal = tinyPadding))
                }
            } ?: {},
            {
                IconButton(
                    modifier = Modifier.padding(tinyPadding),
                    onClick = { onSearchClick() }) {
                    Icon(Icons.Filled.Search, "Search")
                }
            }
        )
    )
}

@Composable
fun NoteCategoryScreenList(
    noteCategories: Flow<PagingData<NoteCategory>>,
    isLoading: Boolean,
    onNewClick: () -> Unit,
    onNoteCategoryClick: (NoteCategory) -> Unit,
    onCheckClick: (NoteCategory, Boolean) -> Unit,
    onFavoriteClick: (NoteCategory) -> Unit,
) {
    UiUtil.GenericList(
        items = noteCategories,
        isLoading = isLoading,
        showItemFunc = { item -> NoteCategoryItem(item, onNoteCategoryClick, onCheckClick, onFavoriteClick) },
        fab = { SimpleFAB(onClick = onNewClick) }
    )
}

/**
 * Composition for a single noteCategory item.
 * @param noteCategory NoteCategory to display.
 * @param onNoteCategoryClick Lambda to perform on noteCategory clicks.
 * @param onCheckClick Lambda to perform on checkbox clicks.
 * @param onFavoriteClick Lambda to perform on noteCategory favorite clicks.
 */
@Composable
fun NoteCategoryItem(
    noteCategory: NoteCategory,
    onNoteCategoryClick: (NoteCategory) -> Unit,
    onCheckClick: (NoteCategory, Boolean) -> Unit,
    onFavoriteClick: (NoteCategory) -> Unit
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Card(
        elevation = 5.dp,
        modifier = Modifier.padding(start = defaultPadding, end = defaultPadding),
        border = BorderStroke(width = 1.dp, Color(noteCategory.color.toArgb())),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(defaultPadding)
                .clickable { onNoteCategoryClick(noteCategory) }
                .semantics(mergeDescendants = true) {},
        ) {
            Checkbox(checked = false, onCheckedChange = { nowChecked -> onCheckClick(noteCategory, nowChecked) })
            Text(modifier = Modifier.weight(1f, fill = false), text = noteCategory.name)
            IconButton(onClick = { onFavoriteClick(noteCategory) }) {
                Icon(
                    imageVector = if (noteCategory.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite"
                )
            }
        }
    }
}
