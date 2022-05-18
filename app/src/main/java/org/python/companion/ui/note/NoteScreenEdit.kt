package org.python.companion.ui.note

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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
fun NoteScreenEdit(noteViewModel: NoteViewModel, id: Long, offset: Int?, navController: NavController, onSaveClick: (Note, Note?) -> Unit) {
    var state by remember { mutableStateOf(LoadingState.LOADING) }
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
            navController = navController,
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
fun NoteScreenEditNew(navController: NavController, onSaveClick: (Note) -> Unit) = NoteScreenEditReady(null, null, null, navController, onSaveClick)

/**
 * Detail screen for editing a single note.
 * @param note Note to edit.
 * @param noteCategory Optional category assigned to passed note.
 * @param offset Optional initial scroll offset in px. Passing `null` prevents scrolling.
 * @param navController Used to handle back events.
 * @param onSaveClick Lambda executed when the user hits the save button.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteScreenEditReady(
    note: Note?,
    noteCategory: NoteCategory?,
    offset: Int? = null,
    navController: NavController,
    onSaveClick: (Note) -> Unit,
) {
    var title by remember { mutableStateOf(note?.name ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var favorite by remember { mutableStateOf(note?.favorite ?: false) }
    var securityLevel by remember { mutableStateOf(note?.securityLevel ?: 0) }
    var renderType by remember { mutableStateOf(note?.renderType ?: RenderType.DEFAULT) }

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
        Card(modifier = Modifier.fillMaxWidth(), elevation = 5.dp) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row {
                    IconButton(onClick = { favorite = !favorite }) {
                        Icon(
                            imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (favorite) "Stop favoring" else "Favorite"
                        )
                    }
                    IconButton(onClick = { securityLevel = if (securityLevel == 0) 1 else 0 }) {
                        Icon(
                            imageVector = if (securityLevel > 0) Icons.Filled.Lock else Icons.Outlined.Lock,
                            contentDescription = if (securityLevel > 0) "Stop securing" else "Secure"
                        )
                    }
                }
                Spacer(Modifier.width(defaultPadding))
                Row {
                    IconButton(onClick = { renderType = RenderType.nextInLine(renderType) }) {
                        when (renderType) {
                            RenderType.DEFAULT -> Icon(
                                imageVector = Icons.Outlined.TextFields,
                                contentDescription = "Text rendering"
                            )
                            RenderType.MARKDOWN -> Icon(
                                painter = painterResource(id = R.drawable.ic_menu_markdown),
                                contentDescription = "Markdown rendering"
                            )
                            RenderType.LATEX -> Icon(
                                painter = painterResource(id = R.drawable.ic_menu_latex),
                                contentDescription = "Latex rendering"
                            )
                        }
                    }
                    IconButton(onClick = { onSaveClick(createNoteObject()) }) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = "Save")
                    }
                }
            }
        }

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

    val showGoBack = remember { mutableStateOf(false) }
    BackHandler(enabled = noteChanged.value) {
        showGoBack.value = !showGoBack.value
    }
    if (showGoBack.value)
        UiUtil.SimpleDialogBinary(
            message = "Found unsaved changes. Are you sure you want to go back?",
            onDismiss = { showGoBack.value = false },
            onNegativeClick = { showGoBack.value = false },
            onPositiveClick = { navController.navigateUp() },
        )
    offset?.let {
        UiUtil.LaunchedEffectSaveable(Unit) {
            scrollState.animateScrollTo(it)
        }
    }
}