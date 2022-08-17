package org.python.companion.ui.note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteCategory
import org.python.backend.data.datatype.NoteWithCategory
import org.python.backend.data.datatype.RenderType
import org.python.companion.R
import org.python.companion.support.LoadingState
import org.python.companion.support.RenderUtil
import org.python.companion.support.UiUtil
import org.python.companion.viewmodels.NoteViewModel
import timber.log.Timber
import java.time.Instant


/** Loads note to edit, then shows edit screen. */
@Composable
fun NoteScreenEdit(
    noteViewModel: NoteViewModel,
    id: Long,
    offset: Int?,
    onBackClick: (Boolean) -> Unit,
    onSaveClick: (Note, Note?) -> Unit
) {
    var state by rememberSaveable { mutableStateOf(LoadingState.LOADING) }
    var existingData by remember { mutableStateOf<NoteWithCategory?>(null) }

    when (state) {
        LoadingState.LOADING -> if (existingData == null) {
            UiUtil.SimpleLoading()
            LaunchedEffect(state) {
                existingData = noteViewModel.getWithCategory(id)
                state = LoadingState.READY
            }
        }
        LoadingState.READY -> NoteScreenEditReady(
            note = existingData?.note,
            noteCategory = existingData?.noteCategory,
            offset = offset,
            onBackClick = onBackClick,
            onSaveClick = { toSaveNote -> onSaveClick(toSaveNote, existingData?.note) },
        )
        else -> {
            Timber.e("Could not find note with id: $id")
            UiUtil.SimpleProblem("Could not find note with id: $id")
        }
    }
}

/** Edits a new note directly. */
@Composable
fun NoteScreenEditNew(onBackClick: (Boolean) -> Unit, onSaveClick: (Note) -> Unit) =
    NoteScreenEditReady(null, null, null, onBackClick, onSaveClick)

/**
 * Detail screen for editing a single note.
 * @param note Note to edit.
 * @param noteCategory Optional category assigned to passed note.
 * @param offset Optional initial scroll offset in px. Passing `null` prevents scrolling.
 * @param onBackClick Lambda executed when the user intends to go back. Parameter indicates whether the note has been edited.
 * @param onSaveClick Lambda executed when the user hits the save button.
 */
@Composable
fun NoteScreenEditReady(
    note: Note?,
    noteCategory: NoteCategory?,
    offset: Int? = null,
    onBackClick: (Boolean) -> Unit,
    onSaveClick: (Note) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(note?.name ?: "") }
    var content by rememberSaveable { mutableStateOf(note?.content ?: "") }
    var favorite by rememberSaveable { mutableStateOf(note?.favorite ?: false) }
    var securityLevel by rememberSaveable { mutableStateOf(note?.securityLevel ?: 0) }
    var renderType by rememberSaveable { mutableStateOf(note?.renderType ?: RenderType.DEFAULT) }

    val categoryKey = noteCategory?.categoryId ?: NoteCategory.DEFAULT.categoryId

    val noteChanged = lazy {
        if (note == null)
            title != "" || content != "" || favorite || securityLevel > 0 || renderType != RenderType.DEFAULT
        else
            title != note.name || content != note.content || favorite != note.favorite || securityLevel != note.securityLevel || renderType != note.renderType
    }

    val createNoteObject: () -> Note = {
        note?.copy(
            name = title,
            content = content,
            favorite = favorite,
            securityLevel = securityLevel,
            categoryKey = categoryKey,
            renderType = renderType,
            date = Instant.now()
        ) ?:
        Note(
            name = title,
            content = content,
            favorite = favorite,
            securityLevel = securityLevel,
            categoryKey = categoryKey,
            renderType = renderType,
            date = Instant.now()
        )
    }

    val scrollState = rememberScrollState()

    val defaultPadding = dimensionResource(id = R.dimen.padding_default)

    Column(modifier = Modifier.fillMaxSize().padding(defaultPadding)) {
        ViewHeader(
            favorite = favorite,
            securityLevel = securityLevel,
            renderType = renderType,
            onFavoriteClick = { favorite = it },
            onSecurityLevelChange = { securityLevel = it },
            onRenderTypeClick = { renderType = it },
            onSaveClick = { onSaveClick(createNoteObject()) }
        )
        Spacer(Modifier.height(defaultPadding))

        Column(modifier = Modifier.weight(0.9f, fill = false).verticalScroll(scrollState)) {
            Card(modifier = Modifier.fillMaxWidth(), elevation = 5.dp) {
                RenderUtil.RenderTextField(
                    value = title,
                    renderType = renderType,
                    modifier = Modifier.fillMaxWidth().padding(defaultPadding),
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = false,
                )
            }
            Spacer(Modifier.height(defaultPadding))

            Card(modifier = Modifier.fillMaxSize(), elevation = 5.dp) {
                RenderUtil.RenderTextField(
                    value = content,
                    renderType = renderType,
                    modifier = Modifier.fillMaxWidth().padding(defaultPadding),
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    singleLine = false,
                )
            }
        }
    }
    BackHandler(enabled = true) {
        onBackClick(noteChanged.value)
    }

    offset?.let {
        UiUtil.LaunchedEffectSaveable(Unit) {
            scrollState.animateScrollTo(it)
        }
    }
}

@Composable
private fun ViewHeader(
    favorite: Boolean,
    securityLevel: Int,
    renderType: RenderType,
    onFavoriteClick: (Boolean) -> Unit,
    onSecurityLevelChange: (Int) -> Unit,
    onRenderTypeClick: (RenderType) -> Unit,
    onSaveClick: () -> Unit
) {
    val defaultPadding = dimensionResource(id = R.dimen.padding_default)
    var renderMenuExpanded by rememberSaveable { mutableStateOf(false) }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row {
            IconButton(onClick = { onFavoriteClick(!favorite) }) {
                Icon(
                    imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (favorite) "Stop favoring" else "Favorite"
                )
            }
            IconButton(onClick = { onSecurityLevelChange(if (securityLevel == 0) 1 else 0) }) {
                Icon(
                    imageVector = if (securityLevel > 0) Icons.Filled.Lock else Icons.Outlined.Lock,
                    contentDescription = if (securityLevel > 0) "Stop securing" else "Secure"
                )
            }
        }
        Spacer(Modifier.width(defaultPadding))
        Row {
            IconButton(onClick = { renderMenuExpanded = !renderMenuExpanded }) {
                RenderUtil.iconForRenderType(renderType)()
            }
            DropdownMenu(
                expanded = renderMenuExpanded,
                onDismissRequest = { renderMenuExpanded = false },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                for (value in RenderType.values()) {
                    if (value != renderType) {
                        IconButton(onClick = { onRenderTypeClick(value) }) {
                            RenderUtil.iconForRenderType(value)()
                        }
                    }
                }
            }

            IconButton(onClick = onSaveClick) {
                Icon(imageVector = Icons.Filled.Save, contentDescription = "Save")
            }
        }
    }
}