package org.python.companion.ui.anniversary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.flow.Flow
import org.python.backend.datatype.Anniversary
import org.python.companion.R


@Composable
fun AnniversaryBody(
    anniversaryList: Flow<PagingData<Anniversary>>,
    onNewClick: () -> Unit,
    onAnniversaryClick: (Anniversary) -> Unit,
    onFavoriteClick: (Anniversary) -> Unit
) {
    val defaultPadding = 12.dp //dimensionResource(id = R.dimen.padding_default)
//    TODO: Maybe add sticky headers: https://developer.android.com/jetpack/compose/lists
    val items: LazyPagingItems<Anniversary> = anniversaryList.collectAsLazyPagingItems()
    Box(
        Modifier
            .fillMaxSize()
            .padding(defaultPadding)
    ) {
        LazyColumn(
            modifier = Modifier
                .semantics { contentDescription = "Anniversary Screen" }
                .padding(defaultPadding),
            contentPadding = PaddingValues(defaultPadding),
            verticalArrangement = Arrangement.spacedBy(defaultPadding),
        ) {
            items(items=items) { anniversary ->
                if (anniversary != null)
                    AnniversaryItem(anniversary, onAnniversaryClick, onFavoriteClick)
            }
            if(items.itemCount == 0) {
                item { EmptyContent() }
            }
        }
        FloatingActionButton(
            onClick = onNewClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
        ) {
            Text("+")
        }
    }
}

@Composable
private fun LazyItemScope.EmptyContent() {
    Box(
        modifier = Modifier.fillParentMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "No anniversaries yet")
    }
}

@Composable
fun AnniversaryItem(
    anniversary: Anniversary,
    onAnniversaryClick: (Anniversary) -> Unit,
    onFavoriteClick: (Anniversary) -> Unit) {

    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Row(
        modifier = Modifier
            .padding(defaultPadding)
            // Regard the whole row as one semantics node. This way each row will receive focus as
            // a whole and the focus bounds will be around the whole row content. The semantics
            // properties of the descendants will be merged. If we'd use clearAndSetSemantics instead,
            // we'd have to define the semantics properties explicitly.
            .semantics(mergeDescendants = true) {},
//        onClick = { onAnniversaryClick(anniversary) }
    ) {
        IconButton(
            onClick = { onFavoriteClick(anniversary) },
            modifier = Modifier
                .align(Alignment.Top)
                .clearAndSetSemantics {}
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null)
        }
    }
}