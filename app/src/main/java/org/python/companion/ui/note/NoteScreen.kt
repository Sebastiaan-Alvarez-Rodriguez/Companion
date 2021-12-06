package org.python.companion.ui.note

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.flow.Flow
import org.python.backend.datatype.Note


/**
 * Overview screen for all notes.
 * @param noteList List of notes to display.
 * @param onNewClick Lambda to perform on new-note button clicks.
 * @param onNoteClick Lambda to perform on note clicks.
 * @param onFavoriteClick Lambda to perform on note favorite clicks.
 */
@Composable
fun NoteBody(
    notes: Flow<PagingData<Note>>,
    onNewClick: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onFavoriteClick: (Note) -> Unit
) {
    val defaultPadding = 12.dp //dimensionResource(id = R.dimen.padding_default)
//    TODO: Maybe add sticky headers: https://developer.android.com/jetpack/compose/lists
    val items: LazyPagingItems<Note> = notes.collectAsLazyPagingItems()

    Box(
        Modifier
            .fillMaxSize()
            .padding(defaultPadding)
    ) {
        LazyColumn(
            modifier = Modifier
                .semantics { contentDescription = "Note Screen" }
                .padding(defaultPadding),
            contentPadding = PaddingValues(defaultPadding),
            verticalArrangement = Arrangement.spacedBy(defaultPadding),
        ) {
            items(items=items) { note ->
                if (note != null)
                    NoteItem(note, onNoteClick, onFavoriteClick)
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
        Text(text = "No notes yet")
    }
}

/**
 * Composition for a single note item.
 * @param note Note to diplay.
 * @param onNoteClick Lambda to perform on note clicks.
 * @param onFavoriteClick Lambda to perform on note favorite clicks.
 */
@Composable
fun NoteItem(
    note: Note,
    onNoteClick: (Note) -> Unit,
    onFavoriteClick: (Note) -> Unit) {

    val defaultPadding = 12.dp //dimensionResource(id = R.dimen.padding_default)
    Card(
        elevation = 5.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(defaultPadding)
                .clickable { onNoteClick(note) }
                // Regard the whole row as one semantics node. This way each row will receive focus as
                // a whole and the focus bounds will be around the whole row content. The semantics
                // properties of the descendants will be merged. If we'd use clearAndSetSemantics instead,
                // we'd have to define the semantics properties explicitly.
                .semantics(mergeDescendants = true) {},
        )
        {
            Checkbox(checked = false, onCheckedChange = {}) // TODO: Handle checkbox behaviour
            Spacer(modifier = Modifier.weight(1f))
            Text(note.name)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { onFavoriteClick(note) }) {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            }
        }
    }
}

/**
 * Detail screen for a single note.
 * @param note Title of the passed note.
 */
@Composable
fun SingleNoteBody(note: String) {
    // remember calculates the value passed to it only during the first composition.
    // It then returns the same value for every subsequent composition.
//    val scrollState = rememberScrollState()
//    Column(modifier = Modifier.scrollable(state = scrollState, orientation = Orientation.Vertical)) {
//        Text(text = note) // TODO: Need to collect note from a repository, and display obtained content here.
//    }
    Card(
        elevation = 5.dp,
    ) {
        Column {
            Text(text = note) // TODO: Need to collect note from a repository, and display obtained content here.
        }
    }
}


/**
 * Detail screen for editing a single note.
 * @param note Title of the passed note.
 * @param onSaveClick Lambda executed when the user hits the save button.
 */
@Composable
fun EditNoteBody(note: String?, onSaveClick: (Note) -> Unit) {
    var title by remember { mutableStateOf(if (note == null) "" else "Note title loaded") }
    var content by remember { mutableStateOf(if (note == null) "" else "Oh hi note") }

    val defaultPadding = 12.dp //dimensionResource(id = R.dimen.padding_default)

    val scrollState = rememberScrollState()
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = 5.dp,
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(defaultPadding)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    modifier = Modifier,
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                )
                Spacer(Modifier.width(defaultPadding))
                Button(
                    modifier = Modifier,
                    onClick = { onSaveClick(Note(title, content)) }) {
                    Text(text = "Save")
                }
            }
            Spacer(Modifier.width(defaultPadding))
            OutlinedTextField(
                modifier = Modifier
                    .scrollable(state = scrollState, orientation = Orientation.Vertical)
                    .fillMaxSize(),
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                singleLine = false
            )

        }
    }
}

//fun OverrideDialog() TODO!