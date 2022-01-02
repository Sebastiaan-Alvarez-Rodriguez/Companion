package org.python.companion.ui.note

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import org.python.backend.data.datatype.Note
import org.python.companion.R


/**
 * Detail screen for a single note.
 * @param note Title of the passed note.
 */
@Composable
fun NoteScreenViewSingle(
    note: Note,
    onEditClick: ((Note) -> Unit)? = null,
    onDeleteClick: ((Note) -> Unit)? = null) {
    val title by remember { mutableStateOf(note.name) }
    val content by remember { mutableStateOf(note.content) }
    val anyOptionsEnabled = onEditClick != null || onDeleteClick != null

    val defaultPadding = dimensionResource(id = R.dimen.padding_default)

    Column(modifier = Modifier.padding(defaultPadding)) {
        Card(elevation = 5.dp) {
            Column(modifier = Modifier.padding(defaultPadding)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = title)
                }
                if (anyOptionsEnabled) {
                    Spacer(Modifier.height(defaultPadding))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (onEditClick != null)
                            Button(onClick = { onEditClick(note) }) {
                                Text(text = "Edit")
                            }
                        if (onDeleteClick != null)
                            Button(onClick = {onDeleteClick(note) }) {
                                Text(text = "Delete")
                            }
                    }
                }
            }
        }
        Spacer(Modifier.height(defaultPadding))
        Card(elevation = 5.dp) {
            Text(text = content, modifier = Modifier
                .fillMaxWidth()
                .padding(defaultPadding))
        }
    }
}
