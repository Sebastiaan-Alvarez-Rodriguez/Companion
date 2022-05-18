package org.python.companion.ui.note

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.viewModelScope
import androidx.navigation.*
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.RenderType
import org.python.companion.R
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.navigateForResult
import org.python.companion.ui.components.CompanionScreen
import org.python.companion.ui.note.category.NoteCategoryState
import org.python.companion.ui.security.SecurityState
import org.python.companion.viewmodels.NoteViewModel
import org.python.datacomm.Result
import org.python.datacomm.ResultType
import timber.log.Timber

class NoteState(
    private val navController: NavHostController,
    private val noteViewModel: NoteViewModel,
    private val scaffoldState: ScaffoldState
) {
    fun load() = noteViewModel.load()

    fun NavGraphBuilder.noteGraph() {
        navigation(startDestination = noteDestination, route = "note") {
            composable(noteDestination) {
                val notes by noteViewModel.notes.collectAsState()
                val isLoading by noteViewModel.isLoading.collectAsState()
                val hasSecureNotes by noteViewModel.hasSecureNotes.collectAsState(initial = false)
                val clearance by noteViewModel.clearance.collectAsState()

                val sortParameters by noteViewModel.sortParameters.collectAsState()
                val searchParameters by noteViewModel.searchParameters.collectAsState()

                val selectedItems = remember { mutableStateListOf<Note>() }
                val securityItem: @Composable (LazyItemScope.() -> Unit)? = if (hasSecureNotes) {
                    if (clearance > 0) {
                        {
                            SecurityClickItem(
                                text = "Lock secure notes",
                                onClick = { noteViewModel.securityActor.logout() }
                            )
                        }
                    } else {
                        {
                            SecurityClickItem(
                                text = "Unlock secure notes",
                                onClick = { SecurityState.navigateToSecurityPick(navController) }
                            )
                        }
                    }
                } else {
                    null
                }
                val defaultPadding = dimensionResource(id = R.dimen.padding_default)
                NoteScreen(
                    header = {
                        if (selectedItems.isEmpty())
                            NoteScreenListHeader(
                                sortParameters = sortParameters,
                                onSettingsClick = { navigateToNoteSettings(navController = navController) },
                                onSortClick = { params -> noteViewModel.updateSortParameters(params) },
                                onSearchClick = { noteViewModel.toggleSearch() }
                            )
                        else
                            NoteScreenContextListHeader(
                                sortParameters = sortParameters,
                                onDeleteClick = { noteViewModel.viewModelScope.launch { noteViewModel.delete(selectedItems) } },
                                onSortClick = { params -> noteViewModel.updateSortParameters(params) },
                                onSearchClick = {  noteViewModel.toggleSearch() },
                            )

                        searchParameters?.let {
                            Spacer(modifier = Modifier.height(defaultPadding))
                            NoteScreenSearchListHeader(
                                searchParameters = it,
                                onBack = { noteViewModel.toggleSearch() },
                                onUpdate = { params -> noteViewModel.updateSearchParameters(params) }
                            )
                        }
                    },
                    list = {
                        NoteScreenList(
                            notes = notes,
                            selectedItems = selectedItems,
                            isLoading = isLoading,
                            onNewClick = { navigateToNoteCreate(navController = navController) {
                                it?.let { navigateToNoteView(navController, noteId = it) }
                            } },
                            onNoteClick = { item -> navigateToNoteView(navController = navController, note = item.note) },
                            onCheckClick = {item, nowChecked -> if (nowChecked) selectedItems.add(item.note) else selectedItems.remove(item.note)},
                            onFavoriteClick = { item ->
                                noteViewModel.viewModelScope.launch { noteViewModel.setFavorite(item.note, !item.note.favorite) }
                            },
                            securityItem = securityItem,
                            rendererCache = noteViewModel.rendererCache
                        )
                    }
                )
            }

            composable(
                route = "$noteDestination/settings",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://$noteDestination/settings"
                    }
                )
            ) {
                Timber.d("Note Settings")
                NoteScreenSettings(
                    onExportClick = { /* TODO */ },
                    onImportClick = { /* TODO */ }
                )
            }

            composable(
                route = "$noteDestination/view/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
                deepLinks = listOf(navDeepLink { uriPattern = "companion://$noteDestination/view/{noteId}" }),
            ) { entry ->
                val noteId = entry.arguments?.getLong("noteId")!!
                Timber.d("Viewing note with id: $noteId")
                NoteScreenViewSingle(
                    noteViewModel = noteViewModel,
                    id = noteId,
                    onDeleteClick = {
                        noteViewModel.viewModelScope.launch { noteViewModel.delete(it) }
                        navController.navigateUp()
                    },
                    onRenderTypeClick = {
                        noteViewModel.viewModelScope.launch { noteViewModel.setRenderType(noteId, RenderType.nextInLine(it)) }
                    },
                    onEditClick = { note, offset -> navigateToNoteEdit(navController = navController, note = note, offset = offset) },
                    onCategoryClick = { NoteCategoryState.navigateToCategorySelect(navController, noteId) }
                )
            }


            composable(
                route = "$noteDestination/create",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://$noteDestination/create"
                    }
                )
            ) {
                val clearance by noteViewModel.clearance.collectAsState()

                NoteScreenEditNew(navController = navController) { toSaveNote ->
                    Timber.d("Found new note: id=${toSaveNote.noteId}, cat=${toSaveNote.categoryKey}, lvl=${toSaveNote.securityLevel}, ${toSaveNote.name}, ${toSaveNote.content}, ${toSaveNote.favorite}")
                    noteViewModel.viewModelScope.launch {
                        if (clearance == 0 && toSaveNote.securityLevel > 0)
                            return@launch SecurityState.navigateToSecurityPick(navController)

                        val conflict = noteViewModel.hasConflict(toSaveNote.name)
                        val conflictBlocking = conflict && !noteViewModel.mayOverride(toSaveNote.name)

                        Timber.d("New note: conflict: $conflict (critical: $conflictBlocking)")
                        when {
                            conflictBlocking -> {
                                val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                                    message = "Cannot override conflicting note with higher security level",
                                    actionLabel = "Login",
                                    duration = SnackbarDuration.Short
                                )
                                when (snackbarResult) {
                                    SnackbarResult.ActionPerformed -> return@launch SecurityState.navigateToSecurityPick(navController)
                                    else -> {}
                                }
                            }
                            else -> {
                                val func: suspend (Result) -> Unit = { result ->
                                    when(result.type) {
                                        ResultType.SUCCESS -> result.pipeData<Long, Unit> { newId ->
                                            navController.popBackStack()
                                            navigateToNoteView(navController, newId)
                                        }
                                        else -> result.message?.let {
                                            scaffoldState.snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
                                        }
                                    }
                                }
                                if (conflict)
                                    UiUtil.UIUtilState.navigateToOverride(navController) { noteViewModel.viewModelScope.launch {func(noteViewModel.upsert(toSaveNote)) } }
                                else
                                    func(noteViewModel.add(toSaveNote))
                            }
                        }
                    }
                }
            }

            composable(
                route = "$noteDestination/edit/{noteId}?offset={offset}",
                arguments = listOf(navArgument("noteId") { type = NavType.LongType }, navArgument("offset") { type = NavType.IntType }),
                deepLinks = listOf(navDeepLink { uriPattern = "companion://$noteDestination/edit/{noteId}?offset={offset}" }),
            ) { entry ->
                val noteId = entry.arguments?.getLong("noteId")!!
                val offset = entry.arguments?.getInt("offset")

                val clearance by noteViewModel.clearance.collectAsState()

                NoteScreenEdit(
                    noteViewModel = noteViewModel,
                    id = noteId,
                    offset = offset,
                    navController = navController,
                    onSaveClick = { toSaveNote, existingNote ->
                        Timber.d("Found edited note: ${toSaveNote.name}, ${toSaveNote.content}, ${toSaveNote.noteId}, ${toSaveNote.favorite}")
                        noteViewModel.viewModelScope.launch {
                            if (clearance == 0 && toSaveNote.securityLevel > 0) // authenticate before saving notes with authentication enabled)
                                return@launch SecurityState.navigateToSecurityPick(navController)

                            // If note name == same as before, there is no conflict. Otherwise, we must check.
                            val conflict = if (toSaveNote.name == existingNote!!.name) false else noteViewModel.hasConflict(toSaveNote.name)
                            val conflictBlocking = conflict && !noteViewModel.mayOverride(toSaveNote.name)
                            Timber.d("Edit note: edited note has changed name=${toSaveNote.name != existingNote.name}, now conflict: $conflict (critical: $conflictBlocking)")
                            when {
                                conflictBlocking -> {
                                    val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                                        message = "Cannot override conflicting note with higher security level",
                                        actionLabel = "Login",
                                        duration = SnackbarDuration.Short
                                    )
                                    when (snackbarResult) {
                                        SnackbarResult.ActionPerformed -> return@launch SecurityState.navigateToSecurityPick(navController)
                                        else -> {}
                                    }
                                }
                                else -> {
                                    val func: suspend (Result) -> Unit = { result ->
                                        when(result.type) {
                                            ResultType.SUCCESS -> result.pipeData<Long, Unit> { newId ->
                                                navController.popBackStack()
                                                navigateToNoteView(navController, newId)
                                            }
                                            else -> result.message?.let {
                                                scaffoldState.snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
                                            }
                                        }
                                    }
                                    if (conflict)
                                        UiUtil.UIUtilState.navigateToOverride(navController) {
                                            noteViewModel.viewModelScope.launch { func(noteViewModel.delete(existingNote) pipe { noteViewModel.upsert(toSaveNote) }) }
                                        }
                                    else
                                        func(noteViewModel.update(existingNote, toSaveNote))
                                }
                            }
                        }
                    }
                )
            }
        }
    }


    private fun navigateToNoteSettings(navController: NavController) = navController.navigate("$noteDestination/settings")
    private fun navigateToNoteView(navController: NavController, note: Note) = navigateToNoteView(navController, note.noteId)
    private fun navigateToNoteView(navController: NavController, noteId: Long) = navController.navigate("$noteDestination/view/$noteId") {
        launchSingleTop = true
    }

    private fun navigateToNoteCreate(navController: NavController, onCreated: (Long?) -> Unit) =
        navController.navigateForResult<Long?>(route = "$noteDestination/create", key = resultKeyNoteCreate) {
            onCreated(it)
        }

    private fun navigateToNoteEdit(navController: NavController, note: Note, offset: Int? = null) = navigateToNoteEdit(navController, note.noteId, offset)
    private fun navigateToNoteEdit(navController: NavController, noteId: Long, offset: Int? = null) =
        navController.navigate(UiUtil.createRoute(base = "$noteDestination/edit", args = listOf(noteId.toString()), optionals = mapOf("offset" to offset?.toString())))

    companion object {
        val noteDestination: String = CompanionScreen.Note.name

        private const val resultKeyNoteCreate: String = "noteState|create"

        @Composable
        fun rememberState(navController: NavHostController = rememberNavController(), noteViewModel: NoteViewModel, scaffoldState: ScaffoldState) =
            remember(navController) { NoteState(navController, noteViewModel, scaffoldState) }
    }
}
