package org.python.companion.ui.note

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import androidx.navigation.*
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.python.backend.data.datatype.Note
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.navigateForResult
import org.python.companion.ui.components.CompanionScreen
import org.python.companion.ui.note.category.NoteCategoryState
import org.python.companion.ui.security.SecurityState
import org.python.companion.viewmodels.NoteViewModel
import timber.log.Timber

class NoteState(private val navController: NavHostController, private val noteViewModel: NoteViewModel) {
    fun load() = noteViewModel.load()

    fun NavGraphBuilder.noteGraph() {
        navigation(startDestination = noteDestination, route = "note") {
            composable(noteDestination) {
                val notes by noteViewModel.notes.collectAsState()
                val isLoading by noteViewModel.isLoading.collectAsState()
                val hasSecureNotes by noteViewModel.hasSecureNotes.collectAsState(initial = false)
                val hasAuthenticated by noteViewModel.authenticated.collectAsState()

                val selectedItems = remember { mutableStateListOf<Note>() }
                val securityItem: @Composable (LazyItemScope.() -> Unit)? = if (hasSecureNotes) {
                    if (hasAuthenticated) {
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
                NoteScreen(
                    header = {
                        if (selectedItems.isEmpty())
                            NoteScreenListHeader(
                                onSearchClick = { /* TODO */ },
                                onSettingsClick = { navigateToNoteSettings(navController = navController) }
                            )
                        else
                            NoteScreenContextListHeader(
                                onDeleteClick = { noteViewModel.viewModelScope.launch { noteViewModel.delete(selectedItems) } },
                                onSearchClick = { /* TODO */ },
                            )
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
                            securityItem = securityItem
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
                NoteScreenViewSingle(
                    noteViewModel = noteViewModel,
                    id = noteId,
                    onEditClick = { navigateToNoteEdit(navController = navController, note = it) },
                    onDeleteClick = {
                        noteViewModel.viewModelScope.launch { noteViewModel.delete(it) }
                        navController.navigateUp()
                    },
                    onCategoryClick = { category ->
                        NoteCategoryState.navigateToCategorySelectOrCreate(navController, category) { newCategoryId ->
                            Timber.w("Got new id: $newCategoryId")
                            newCategoryId?.let {
                                noteViewModel.viewModelScope.launch { noteViewModel.updateCategoryForNote(noteId, it) }
                            }
                        }
                    }
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
                NoteScreenEditNew { toSaveNote ->
                    Timber.d("Found new note: id=${toSaveNote.noteId}, cat=${toSaveNote.categoryKey}, ${toSaveNote.name}, ${toSaveNote.content}, ${toSaveNote.favorite}")
                    noteViewModel.viewModelScope.launch {
                        val conflict = noteViewModel.hasConflict(toSaveNote.name)
                        Timber.d("New note: conflict: $conflict")
                        if (!conflict) {
                            noteViewModel.add(toSaveNote)?.let {
                                navController.popBackStack()
                                navigateToNoteView(navController, it)
                            }
                        } else {
                            UiUtil.UIUtilState.navigateToOverride(navController) {
                                Timber.d("New note: Overriding ${toSaveNote.name}...")
                                noteViewModel.viewModelScope.launch { noteViewModel.upsert(toSaveNote) }
                                navController.popBackStack()
                                navigateToNoteView(navController, toSaveNote.noteId)
                            }
                        }
                    }
                }
            }

            composable(
                route = "$noteDestination/edit/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
                deepLinks = listOf(navDeepLink { uriPattern = "companion://$noteDestination/edit/{noteId}" }),
            ) { entry ->
                val noteId = entry.arguments?.getLong("noteId")!!
                val hasAuthenticated by noteViewModel.authenticated.collectAsState()

                NoteScreenEdit(
                    noteViewModel = noteViewModel,
                    id = noteId,
                    onSaveClick = { toSaveNote, existingNote ->
                        Timber.d("Found new note: ${toSaveNote.name}, ${toSaveNote.content}, ${toSaveNote.noteId}, ${toSaveNote.favorite}")
                        if (!hasAuthenticated && toSaveNote.secure) {
                            SecurityState.navigateToSecurityPick(navController)
                        } else {
                            noteViewModel.viewModelScope.launch {
                                // If note name == same as before, there is no conflict. Otherwise, we must check.
                                val conflict = if (toSaveNote.name == existingNote!!.name) false else noteViewModel.hasConflict(toSaveNote.name)
                                Timber.d("Edit note: edited note has changed name=${toSaveNote.name != existingNote.name}, now conflict: ${conflict}")
                                if (!conflict) {
                                    noteViewModel.update(existingNote, toSaveNote)
                                    navController.navigateUp()
                                } else {
                                    UiUtil.UIUtilState.navigateToOverride(navController) {
                                        Timber.d("Edit note: Overriding note (new name=${toSaveNote.name})")
                                        noteViewModel.viewModelScope.launch {
                                            noteViewModel.delete(existingNote)
                                            noteViewModel.upsert(toSaveNote)
                                        }
                                        navController.navigateUp()
                                    }
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
    private fun navigateToNoteView(navController: NavController, noteId: Long) = navController.navigate("$noteDestination/view/$noteId")

    private fun navigateToNoteCreate(navController: NavController, onCreated: (Long?) -> Unit) =
        navController.navigateForResult<Long?>(
            route = "$noteDestination/create",
            key = resultKeyNoteCreate
        ) {
            onCreated(it)
        }

    private fun navigateToNoteEdit(navController: NavController, note: Note) = navigateToNoteEdit(navController, note.noteId)
    private fun navigateToNoteEdit(navController: NavController, noteId: Long) = navController.navigate("$noteDestination/edit/$noteId")

    companion object {
        val noteDestination: String = CompanionScreen.Note.name

        private const val resultKeyNoteCreate: String = "noteState|create"

        @Composable
        fun rememberState(navController: NavHostController = rememberNavController(), noteViewModel: NoteViewModel) =
            remember(navController) { NoteState(navController, noteViewModel) }
    }
}
