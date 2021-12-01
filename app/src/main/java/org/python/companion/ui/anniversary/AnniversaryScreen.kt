package org.python.companion.ui.anniversary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
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
import org.python.companion.R
import org.python.backend.datatype.Anniversary


@Composable
fun AnniversaryBody(
    anniversaryList: List<Anniversary>,
    onAnniversaryClick: (Anniversary) -> Unit,
    onFavoriteClick: (Anniversary) -> Unit
) {
    val scrollState = rememberScrollState()
    val defaultPadding = 12.dp //dimensionResource(id = R.dimen.padding_default)
    LazyColumn (
        modifier = Modifier
            .verticalScroll(scrollState)
            .semantics { contentDescription = "Note Screen" }
            .padding(defaultPadding),
        contentPadding = PaddingValues(defaultPadding),
        verticalArrangement = Arrangement.spacedBy(defaultPadding),
    ) {
        items(anniversaryList) { anniversary -> AnniversaryItem(anniversary, onAnniversaryClick, onFavoriteClick) }
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
//        onClick = { onNoteClick(note) }
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