package org.python.companion.ui.note.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import org.python.companion.support.UiUtil


@Composable
fun NoteCategoryScreen(
    noteCategoryScreenListHeaderStruct: NoteCategoryScreenListHeaderStruct,
    noteCategoryScreenListStruct: NoteCategoryScreenListStruct,
) = NoteCategoryScreenList(noteCategoryScreenListHeaderStruct, noteCategoryScreenListStruct)

@Composable
fun NoteCategoryScreenList(
    noteCategoryScreenListHeaderStruct: NoteCategoryScreenListHeaderStruct,
    noteCategoryScreenListStruct: NoteCategoryScreenListStruct,
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Column(modifier = Modifier.padding(defaultPadding)) {
        NoteCategoryScreenListHeader(noteCategoryScreenListHeaderStruct)
        Spacer(modifier = Modifier.height(defaultPadding))
        NoteCategoryScreenList(noteCategoryScreenListStruct)
    }
}

class NoteCategoryScreenListHeaderStruct(val onSearchClick: () -> Unit)

@Composable
fun NoteCategoryScreenListHeader(noteCategoryScreenListHeaderStruct: NoteCategoryScreenListHeaderStruct) =
    NoteCategoryScreenListHeader(
        onSearchClick = noteCategoryScreenListHeaderStruct.onSearchClick
    )

@Composable
fun NoteCategoryScreenListHeader(onSearchClick: () -> Unit) {
    val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)

    Card(elevation = 5.dp) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.width(defaultPadding))
            IconButton(
                modifier = Modifier.padding(tinyPadding),
                onClick = { onSearchClick() }) {
                Icon(Icons.Filled.Search, "Search")
            }
        }
    }
}

class NoteCategoryScreenListStruct(
    val noteCategories: Flow<PagingData<NoteCategory>>,
    val isLoading: Boolean,
    val onNewClick: () -> Unit,
    val onNoteCategoryClick: (NoteCategory) -> Unit,
    val onFavoriteClick: (NoteCategory) -> Unit,
)

@Composable
fun NoteCategoryScreenList(noteCategoryScreenListStruct: NoteCategoryScreenListStruct) =
    NoteCategoryScreenList(
        NoteCategories = noteCategoryScreenListStruct.noteCategories,
        isLoading = noteCategoryScreenListStruct.isLoading,
        onNewClick = noteCategoryScreenListStruct.onNewClick,
        onNoteCategoryClick = noteCategoryScreenListStruct.onNoteCategoryClick,
        onFavoriteClick = noteCategoryScreenListStruct.onFavoriteClick,
    )


@Composable
fun NoteCategoryScreenList(
    NoteCategories: Flow<PagingData<NoteCategory>>,
    isLoading: Boolean,
    onNewClick: () -> Unit,
    onNoteCategoryClick: (NoteCategory) -> Unit,
    onFavoriteClick: (NoteCategory) -> Unit,
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
//    TODO: Maybe add sticky headers: https://developer.android.com/jetpack/compose/lists
    val items: LazyPagingItems<NoteCategory> = NoteCategories.collectAsLazyPagingItems()
    val listState: LazyListState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
        when {
            isLoading -> UiUtil.SimpleLoading()
            items.itemCount == 0 -> UiUtil.SimpleText("Nothing here yet")
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().semantics { contentDescription = "Note Category Screen" },
                    contentPadding = PaddingValues(defaultPadding),
                    verticalArrangement = Arrangement.spacedBy(defaultPadding),
                    state = listState,
                ) {
                    items(items = items) { NoteCategory ->
                        if (NoteCategory != null)
                            NoteCategoryItem(NoteCategory, onNoteCategoryClick, onFavoriteClick)
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
 * Composition for a single noteCategory item.
 * @param noteCategory NoteCategory to display.
 * @param onNoteCategoryClick Lambda to perform on noteCategory clicks.
 * @param onFavoriteClick Lambda to perform on noteCategory favorite clicks.
 */
@Composable
fun NoteCategoryItem(
    noteCategory: NoteCategory,
    onNoteCategoryClick: (NoteCategory) -> Unit,
    onFavoriteClick: (NoteCategory) -> Unit
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Card(elevation = 5.dp) {
        Card(elevation = 0.dp, modifier = Modifier.padding(start = 16.dp).background(Color(noteCategory.color.toArgb()))) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .padding(defaultPadding)
                    .clickable { onNoteCategoryClick(noteCategory) }
                    .semantics(mergeDescendants = true) {},
            ) {
                Checkbox(checked = false, onCheckedChange = {}) // TODO: Handle checkbox behaviour
                Text(modifier = Modifier.weight(1f, fill = false), text = noteCategory.name)
                IconButton(onClick = { onFavoriteClick(noteCategory) }) {
                    Icon(modifier = Modifier.background(Color(noteCategory.color.toArgb())),
                        imageVector = if (noteCategory.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite"
                    )
                }
            }
        }
    }
}