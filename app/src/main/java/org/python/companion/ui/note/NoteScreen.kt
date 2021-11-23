package org.python.companion.ui.note

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import org.python.companion.R
import org.python.companion.datatype.Note


@Composable
fun NoteBody(
    noteList: List<Note>,
    onNoteClick: (Note) -> Unit,
    onFavoriteClick: (Note) -> Unit
) {
    val scrollState = rememberScrollState()
    val defaultPadding = dimensionResource(id = R.dimen.DEFAULT_PADDING)
    LazyColumn (
        modifier = Modifier
            .verticalScroll(scrollState)
            .semantics { contentDescription = "Note Screen" }
            .padding(defaultPadding),
        contentPadding = PaddingValues(defaultPadding),
        verticalArrangement = Arrangement.spacedBy(defaultPadding),
    ) {
        items(noteList) { note -> NoteItem(note, onNoteClick, onFavoriteClick) }
    }
}

@Composable
fun NoteItem(
    note: Note,
    onNoteClick: (Note) -> Unit,
    onFavoriteClick: (Note) -> Unit) {

    val defaultPadding = dimensionResource(id = R.dimen.DEFAULT_PADDING)
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
            onClick = { onFavoriteClick(note) },
            modifier = Modifier
                .align(Alignment.Top)
                .clearAndSetSemantics {}
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null)
        }
    }
}

/**
 * Detail screen for a single note.
 */
@Composable
fun SingleNoteBody(note: Note) {
    // remember calculates the value passed to it only during the first composition.
    // It then returns the same value for every subsequent composition.
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.scrollable(state = scrollState, orientation = Orientation.Vertical)) {
        Text(text = note.content)
    }
}