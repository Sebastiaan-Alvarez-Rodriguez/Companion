package org.python.companion.ui.note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.python.backend.datatype.Note
import org.python.companion.R
import org.python.companion.support.UiUtil
import timber.log.Timber


@Composable
fun NoteScreen(
    noteScreenHeaderStruct: NoteScreenHeaderStruct,
    noteScreenListStruct: NoteScreenListStruct
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)

    Column(modifier = Modifier.padding(defaultPadding)) {
        NoteScreenHeader(noteScreenHeaderStruct)
        Spacer(modifier = Modifier.height(defaultPadding))
        NoteScreenList(noteScreenListStruct)
    }
}

class NoteScreenHeaderStruct(val onSettingsClick: () -> Unit, val onSearchClick: () -> Unit)

@Composable
fun NoteScreenHeader(noteScreenHeaderStruct: NoteScreenHeaderStruct) =
    NoteScreenHeader(
        onSettingsClick = noteScreenHeaderStruct.onSettingsClick,
        onSearchClick = noteScreenHeaderStruct.onSearchClick
    )

@Composable
fun NoteScreenHeader(onSettingsClick: () -> Unit, onSearchClick: () -> Unit) {
    val tinyPadding = dimensionResource(id = R.dimen.padding_tiny)
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)

    Card(elevation = 5.dp) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(modifier = Modifier.padding(tinyPadding), onClick = { onSettingsClick() }) {
                Icon(Icons.Filled.Settings, "Settings")
            }
            Spacer(modifier = Modifier.width(defaultPadding))
            IconButton(
                modifier = Modifier.padding(tinyPadding),
                onClick = { onSearchClick() }) {
                Icon(Icons.Filled.Search, "Search")
            }
        }
    }
}

class NoteScreenListStruct(
    val notes: Flow<PagingData<Note>>,
    val onNewClick: () -> Unit,
    val onNoteClick: (Note) -> Unit,
    val onFavoriteClick: (Note) -> Unit
)

@Composable
fun NoteScreenList(noteScreenListStruct: NoteScreenListStruct) =
    NoteScreenList(
        notes = noteScreenListStruct.notes,
        onNewClick = noteScreenListStruct.onNewClick,
        onNoteClick = noteScreenListStruct.onNoteClick,
        onFavoriteClick = noteScreenListStruct.onFavoriteClick
    )

/**
 * Overview screen for all notes.
 * @param notes List of notes to display.
 * @param onNewClick Lambda to perform on new-note button clicks.
 * @param onNoteClick Lambda to perform on note clicks.
 * @param onFavoriteClick Lambda to perform on note favorite clicks.
 */
@Composable
fun NoteScreenList(
    notes: Flow<PagingData<Note>>,
    onNewClick: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onFavoriteClick: (Note) -> Unit
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
//    TODO: Maybe add sticky headers: https://developer.android.com/jetpack/compose/lists
    val items: LazyPagingItems<Note> = notes.collectAsLazyPagingItems()
    val listState = rememberLazyListState(1, 1)
    Timber.d("List state redraw. " +
            "first_visible=${listState.firstVisibleItemIndex}, " +
            "offset=${listState.firstVisibleItemScrollOffset}, " +
            "layoutInfo=${listState}")

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().semantics { contentDescription = "Note Screen" },
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
 * @param note Note to display.
 * @param onNoteClick Lambda to perform on note clicks.
 * @param onFavoriteClick Lambda to perform on note favorite clicks.
 */
@Composable
fun NoteItem(
    note: Note,
    onNoteClick: (Note) -> Unit,
    onFavoriteClick: (Note) -> Unit) {

    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    Card(elevation = 5.dp) {
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
 * NoteScreen settings screen, where users can change several settings.
 * @param onExportClick Lambda to perform on note export clicks.
 * @param onImportClick Lambda to perform on note import clicks.
 */
@Composable
fun NoteScreenSettings(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)

    Card(elevation = 5.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(modifier = Modifier.fillMaxWidth().padding(defaultPadding), onClick = onExportClick) {
                Text(text = "Export")
            }
            Button(modifier = Modifier.fillMaxWidth().padding(defaultPadding), onClick = onImportClick) {
                Text(text = "Import")
            }
        }
    }
}

@Composable
fun NoteScreenSettingsTransfers(exporting: Boolean) {

}

/**
 * Detail screen for a single note.
 * @param note Title of the passed note.
 */
@Composable
fun NoteViewBody(
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

/**
 * Detail screen for editing a single note.
 * @param note Title of the passed note.
 * @param overrideDialogMiniState
 * @param onSaveClick Lambda executed when the user hits the save button.
 * @param onOverrideAcceptClick Lambda executed when the user hits the override button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditNoteBody(
    note: Note?,
    overrideDialogMiniState: NoteOverrideDialogMiniState,
    onSaveClick: (Note) -> Unit,
    onOverrideAcceptClick: (Note) -> Unit
) {
    var title by remember { mutableStateOf(note?.name ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    val changed = lazy { (note != null && (title != note.name || content != note.content)) ||
            (note == null && (title != "" || content != ""))}

    val createNoteObject: () -> Note = { note?.copy(name = title, content = content) ?: Note(name = title, content = content) }

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
    open: MutableState<Boolean>) : UiUtil.DialogMiniState(open) {

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