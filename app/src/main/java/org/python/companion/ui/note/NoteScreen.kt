package org.python.companion.ui.note

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import org.python.companion.datatype.Note


/**
 * Overview screen for all notes.
 * @param noteList List of notes to display.
 * @param onNewClick Lambda to perform on new-note button clicks.
 * @param onNoteClick Lambda to perform on note clicks.
 * @param onFavoriteClick Lambda to perform on note favorite clicks.
 */
@Composable
fun NoteBody(
    noteList: List<Note>,
    onNewClick: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onFavoriteClick: (Note) -> Unit
) {
    val defaultPadding = 12.dp //dimensionResource(id = R.dimen.padding_default)
//    TODO: Maybe add sticky headers: https://developer.android.com/jetpack/compose/lists

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
            items(noteList) { note -> NoteItem(note, onNoteClick, onFavoriteClick) }
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

//    Row(
//        modifier = Modifier
//            .padding(defaultPadding)
//            .semantics(mergeDescendants = true) {},
////        onClick = { onNoteClick(note) }
//    ) {
//        IconButton(
//            onClick = { onFavoriteClick(note) },
//            modifier = Modifier
//                .align(Alignment.Top)
//                .clearAndSetSemantics {}
//        ) {
//            Icon(Icons.Filled.Favorite, contentDescription = null)
//        }
//    }
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
            .scrollable(state = scrollState, orientation = Orientation.Vertical)
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
                modifier = Modifier.fillMaxSize(),
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                singleLine = false
            )

        }
    }
}