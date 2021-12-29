package org.python.companion.ui.note

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.python.backend.datatype.Note
import org.python.companion.support.UiUtil

@Composable
fun NoteOverrideDialog(
    currentNote: Note,
    overridenNote: Note,
    onDismiss: () -> Unit,
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp)
        ) {

            Column(modifier = Modifier.padding(8.dp)) {

                Text(
                    text = "Name ${currentNote.name} already in use",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                NoteItem(note = overridenNote, onNoteClick = {}, onFavoriteClick = {})

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    TextButton(onClick = onNegativeClick) {
                        Text(text = "CANCEL")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = onPositiveClick) {
                        Text(text = "OVERRIDE")
                    }
                }
            }
        }
    }
}

class NoteOverrideDialogMiniState(
    val currentNote: MutableState<Note?>,
    val overridenNote: MutableState<Note?>,
    open: MutableState<Boolean>
) : UiUtil.DialogMiniState(open) {

    fun open(currentNote: Note, overridenNote: Note) {
        open.value = true
        this.currentNote.value = currentNote
        this.overridenNote.value = overridenNote
    }

    fun close() {
        open.value = false
        currentNote.value = null
        overridenNote.value = null
    }

    companion object {
        @Composable
        fun rememberState(currentNote: Note?, overridenNote: Note?, open: Boolean) =
            remember(open) {  NoteOverrideDialogMiniState(mutableStateOf(currentNote), mutableStateOf(overridenNote), mutableStateOf(open)) }
    }
}