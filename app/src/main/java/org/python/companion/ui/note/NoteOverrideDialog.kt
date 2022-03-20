package org.python.companion.ui.note

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.python.backend.data.datatype.Note
import org.python.companion.support.UiUtil

@Composable
fun NoteOverrideDialog(
    currentNote: Note,
    overriddenNote: Note,
    onDismiss: () -> Unit,
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit
) {
    UiUtil.SimpleDialogOverride(
        onDismiss = onDismiss,
        onNegativeClick = onNegativeClick,
        onPositiveClick = onPositiveClick,
        message = "Name ${currentNote.name} already in use"
    ) {
        NoteItem(note = overriddenNote, onNoteClick = {}, onFavoriteClick = {})
    }
}

class NoteOverrideDialogMiniState(
    val currentNote: MutableState<Note?>,
    val overriddenNote: MutableState<Note?>,
    open: MutableState<Boolean>
) : UiUtil.OpenableMiniState(open) {
    override fun open() {
        throw RuntimeException("I should not be here") // TODO: What about this call?
    }

    fun open(currentNote: Note, overriddenNote: Note) {
        open.value = true
        this.currentNote.value = currentNote
        this.overriddenNote.value = overriddenNote
    }

    override fun close() {
        open.value = false
        currentNote.value = null
        overriddenNote.value = null
    }

    companion object {
        @Composable
        fun rememberState(currentNote: Note? = null, overriddenNote: Note? = null, open: Boolean = false) =
            remember(open) {
                NoteOverrideDialogMiniState(
                    mutableStateOf(currentNote),
                    mutableStateOf(overriddenNote),
                    mutableStateOf(open)
                )
            }
    }
}