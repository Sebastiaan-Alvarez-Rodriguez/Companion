package org.python.companion.ui.note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.python.backend.data.datatype.Note
import org.python.companion.R


/**
 * Detail screen for editing a single note.
 * @param note Title of the passed note.
 * @param overrideDialogMiniState
 * @param onSaveClick Lambda executed when the user hits the save button.
 * @param onOverrideAcceptClick Lambda executed when the user hits the override button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteScreenEdit(
    note: Note?,
    overrideDialogMiniState: NoteOverrideDialogMiniState,
    onSaveClick: (Note) -> Unit,
    onOverrideAcceptClick: (Note) -> Unit
) {
    var title by remember { mutableStateOf(note?.name ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    val changed = lazy { (note != null && (title != note.name || content != note.content)) ||
            (note == null && (title != "" || content != ""))}

    val createNoteObject: () -> Note = { note?.copy(name = title, content = content) ?: Note(name = title, content = content, secure = /* TODO: allow user to set security */ false) }

    val defaultPadding = dimensionResource(id = R.dimen.padding_default)

    Box {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(defaultPadding),
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
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                    )
                    Spacer(Modifier.width(defaultPadding))
                    Button(
                        onClick = { onSaveClick(createNoteObject()) }) {
                        Text(text = "Save")
                    }
                }
                Spacer(Modifier.width(defaultPadding))
                val relocation = remember { BringIntoViewRequester() }
                val scope = rememberCoroutineScope()

                OutlinedTextField(
                    modifier = Modifier
                        .bringIntoViewRequester(relocation)
                        .onFocusEvent {
                            if (it.isFocused) scope.launch { delay(300); relocation.bringIntoView() }
                        }
                        .fillMaxWidth(),
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    singleLine = false,
                )
            }
        }
        if (overrideDialogMiniState.open.value)
            NoteOverrideDialog(
                overrideDialogMiniState.currentNote.value!!,
                overrideDialogMiniState.overridenNote.value!!,
                {},
                { onOverrideAcceptClick(createNoteObject()) },
                {}
            )

        BackHandler(enabled = overrideDialogMiniState.open.value) {
            overrideDialogMiniState.close()
        }

        BackHandler(enabled = changed.value) {
            // TODO: Are you sure you want to go back?
        }
    }
}