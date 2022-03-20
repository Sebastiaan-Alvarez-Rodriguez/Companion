package org.python.companion

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.python.backend.data.datatype.Anniversary
import org.python.backend.data.datatype.Note
import org.python.companion.ui.anniversary.AnniversaryBody
import org.python.companion.ui.cactus.CactusBody
import org.python.companion.ui.components.CompanionScreen
import org.python.companion.ui.components.CompanionTabRow
import org.python.companion.ui.note.*
import org.python.companion.ui.note.category.NoteCategoryScreen
import org.python.companion.ui.note.category.NoteCategoryScreenEditNew
import org.python.companion.ui.note.category.NoteCategoryScreenListHeaderStruct
import org.python.companion.ui.note.category.NoteCategoryScreenListStruct
import org.python.companion.ui.security.SecurityState
import org.python.companion.ui.splash.SplashBuilder
import org.python.companion.ui.theme.CompanionTheme
import org.python.companion.viewmodels.AnniversaryViewModel
import org.python.companion.viewmodels.NoteCategoryViewModel
import org.python.companion.viewmodels.NoteViewModel
import org.python.companion.viewmodels.SecurityViewModel
import timber.log.Timber


class MainActivity : FragmentActivity() {
    private val noteViewModel by viewModels<NoteViewModel>()
    private val noteCategoryViewModel by viewModels<NoteCategoryViewModel>()

    private val anniversaryViewModel by viewModels<AnniversaryViewModel>()
    private val securityViewModel by viewModels<SecurityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompanionTheme {
                val allScreens = CompanionScreen.values().toList()
                val navController = rememberNavController()
                val backstackEntry = navController.currentBackStackEntryAsState()
                val selectedTabScreen = CompanionScreen.fromRoute(backstackEntry.value?.destination?.route)

                val securityState = SecurityState.rememberState(
                    activity = this,
                    navController = navController,
                    securityViewModel = securityViewModel,
                    noteViewModel = noteViewModel
                )
                val noteState = NoteState.rememberState(
                    navController = navController,
                    noteViewModel = noteViewModel
                )
                val noteCategoryState = NoteCategoryState.rememberState(
                    navController = navController,
                    noteCategoryViewModel = noteCategoryViewModel
                )
                val cactusState = CactusState.rememberState(navController = navController)
                val anniversaryState = AnniversaryState.rememberState(
                    navController = navController,
                    anniversaryViewModel = anniversaryViewModel
                )

                Scaffold(
                    topBar = {
                        CompanionTabRow(
                            allScreens = allScreens,
                            onTabSelected = { screen ->
                                    Timber.w("Got tab selected: ${screen.name}")
                                     navController.navigate(screen.name) { launchSingleTop = true } },
                            currentScreen = selectedTabScreen
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash_screen",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("splash_screen") {
                            val splashScreenFunc = remember {
                                SplashBuilder(navController = navController, destination = CactusState.cactusDestination).build {
                                    noteState.load()
                                    anniversaryState.load()
                                }
                            }
                            splashScreenFunc()
                        }
                        with(cactusState) { cactusGraph() }
                        with(noteState) { noteGraph() }
                        with(anniversaryState) { anniversaryGraph() }
                        with(securityState) { securityGraph() }
                    }
                }
            }
        }
    }
}

class NoteCategoryState(
    private val navController: NavHostController,
    private val noteCategoryViewModel: NoteCategoryViewModel,
) {

    fun NavGraphBuilder.noteGraph() {
        navigation(startDestination = noteCategoryDestination, route = "category") {
            composable(noteCategoryDestination) {
                val noteCategories by noteCategoryViewModel.noteCategories.collectAsState()
                val isLoading by noteCategoryViewModel.isLoading.collectAsState()

                NoteCategoryScreen(
                    noteCategoryScreenListHeaderStruct = NoteCategoryScreenListHeaderStruct(
                        onSearchClick = { /* TODO */ }
                    ),
                    noteCategoryScreenListStruct = NoteCategoryScreenListStruct(
                        noteCategories = noteCategories,
                        isLoading = isLoading,
                        onNewClick = { /* TODO: edit screen */ },
                        onNoteCategoryClick = { /* TODO: edit screen */},
                        onFavoriteClick = { /* TODO: toggle favorite */ }
                    )
                )
            }

            composable(
                route = "${noteCategoryDestination}/create",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://${noteCategoryDestination}/create"
                    }
                )
            ) {
                val noteOverrideDialogMiniState = NoteOverrideDialogMiniState.rememberState(null, null, false)
                NoteCategoryScreenEditNew(
                    noteOverrideDialogMiniState,
                    onSaveClick = { toSaveNote ->
                        Timber.d("Found new noteCategory: ${toSaveNote.name}, ${toSaveNote.content}, ${toSaveNote.noteId}, ${toSaveNote.favorite}")
                        noteCategoryViewModel.viewModelScope.launch {
                            val conflict = noteCategoryViewModel.getbyName(toSaveNote.name)
                            Timber.d("New note: conflict: ${conflict!=null}")
                            if (conflict == null) {
                                if (noteCategoryViewModel.add(toSaveNote))
                                    navController.navigateUp()
                                else
                                    TODO("Let user know there was a problem while adding note")
                            } else {
                                noteOverrideDialogMiniState.open(toSaveNote, conflict)
                            }
                        }
                    },
                    onOverrideAcceptClick = { toSaveNote ->
                        Timber.d("New note: Overriding ${toSaveNote.name}...")
                        noteCategoryViewModel.viewModelScope.launch { noteCategoryViewModel.upsert(toSaveNote) }
                        noteOverrideDialogMiniState.close()
                        navController.navigateUp()
                    }
                )
            }
        }
    }
    companion object {
        val noteCategoryDestination: String = "${NoteState.noteDestination}/category"

        @Composable
        fun rememberState(navController: NavHostController = rememberNavController(), noteCategoryViewModel: NoteCategoryViewModel) =
            remember(navController) { NoteCategoryState(navController, noteCategoryViewModel) }
    }
}

class NoteState(
    private val navController: NavHostController,
    private val noteViewModel: NoteViewModel,
) {
    fun load() = noteViewModel.load()

    fun NavGraphBuilder.noteGraph() {
        navigation(startDestination = noteDestination, route = "note") {
            composable(noteDestination) {
                val notes by noteViewModel.notes.collectAsState()
                val isLoading by noteViewModel.isLoading.collectAsState()
                val hasSecureNotes by noteViewModel.hasSecureNotes.collectAsState(initial = false)
                val hasAuthenticated by noteViewModel.authenticated.collectAsState()

                val securityStruct = if (hasSecureNotes) {
                    if (hasAuthenticated)
                        NoteScreenListSecurityStruct(
                            securityText = "Lock secure notes",
                            onSecurityClick = {
                                Timber.w("Authstate disable busy: $hasAuthenticated")
                                noteViewModel.securityActor.logout()
                                Timber.w("Authstate disable complete: $hasAuthenticated")
                            }
                        )
                    else
                        NoteScreenListSecurityStruct(
                            securityText = "Unlock secure notes",
                            onSecurityClick = { SecurityState.navigateToSecurityPick(navController) }
                        )
                } else {
                    null
                }
                NoteScreen(
                    noteScreenListHeaderStruct = NoteScreenListHeaderStruct(
                        onSearchClick = { /* TODO */ },
                        onSettingsClick = { navigateToNoteSettings(navController = navController) }
                    ),
                    noteScreenListStruct = NoteScreenListStruct(
                        notes = notes,
                        isLoading = isLoading,
                        onNewClick = { navigateToNoteCreate(navController = navController) },
                        onNoteClick = { note -> navigateToNoteSingle(navController = navController, note = note) },
                        onFavoriteClick = { note ->
                            noteViewModel.viewModelScope.launch { noteViewModel.setFavorite(note, !note.favorite) }
                        },
                        securityStruct = securityStruct,
                    )
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
                    navController = navController,
                    id = noteId,
                    onEditClick = { navigateToNoteEdit(navController = navController, note = it) },
                    onDeleteClick = {
                        noteViewModel.viewModelScope.launch { noteViewModel.delete(it) }
                        navController.navigateUp()
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
                val noteOverrideDialogMiniState = NoteOverrideDialogMiniState.rememberState(null, null, false)
                NoteScreenEditNew(
                    noteOverrideDialogMiniState,
                    onCategoryClick = { TODO("Implement category editing from note edit") },
                    onSaveClick = { toSaveNote ->
                        Timber.d("Found new note: ${toSaveNote.name}, ${toSaveNote.content}, ${toSaveNote.noteId}, ${toSaveNote.favorite}")
                        noteViewModel.viewModelScope.launch {
                            val conflict = noteViewModel.getbyName(toSaveNote.name)
                            Timber.d("New note: conflict: ${conflict!=null}")
                            if (conflict == null) {
                                if (noteViewModel.add(toSaveNote))
                                    navController.navigateUp()
                                else
                                    TODO("Let user know there was a problem while adding note")
                            } else {
                                noteOverrideDialogMiniState.open(toSaveNote, conflict)
                            }
                        }
                    },
                    onOverrideAcceptClick = { toSaveNote ->
                        Timber.d("New note: Overriding ${toSaveNote.name}...")
                        noteViewModel.viewModelScope.launch { noteViewModel.upsert(toSaveNote) }
                        noteOverrideDialogMiniState.close()
                        navController.navigateUp()
                    }
                )
            }

            composable(
                route = "$noteDestination/edit/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
                deepLinks = listOf(navDeepLink { uriPattern = "companion://$noteDestination/edit/{noteId}" }),
            ) { entry ->
                val noteId = entry.arguments?.getLong("noteId")!!

                val noteOverrideDialogMiniState = NoteOverrideDialogMiniState.rememberState()

                NoteScreenEdit(
                    noteViewModel = noteViewModel,
                    id = noteId,
                    overrideDialogMiniState = noteOverrideDialogMiniState,
                    onCategoryClick = { TODO("Implement category editing from note edit") },
                    onSaveClick = { toSaveNote, existingNote ->
                        Timber.d("Found new note: ${toSaveNote.name}, ${toSaveNote.content}, ${toSaveNote.noteId}, ${toSaveNote.favorite}")
                        noteViewModel.viewModelScope.launch {
                            // If note name == same as before, there is no conflict. Otherwise, we must check.
                            val conflict: Note? = if (toSaveNote.name == existingNote!!.name) null else noteViewModel.getbyName(toSaveNote.name)
                            Timber.d("Edit note: edited note has changed name=${toSaveNote.name != existingNote.name}, now conflict: ${conflict != null}")
                            if (conflict == null) {
                                noteViewModel.update(existingNote, toSaveNote)
                                navController.navigateUp()
                            } else {
                                noteOverrideDialogMiniState.open(toSaveNote, conflict)
                            }
                        }
                    },
                    onOverrideAcceptClick = { toSaveNote, existingNote ->
                        Timber.d("Edit note: Overriding note (new name=${toSaveNote.name})")
                        noteViewModel.viewModelScope.launch {
                            noteViewModel.delete(existingNote!!)
                            noteViewModel.upsert(toSaveNote)
                        }
                        noteOverrideDialogMiniState.close()
                        navController.navigateUp()
                    }
                )
            }

            composable( // TODO: Implement later
                route = "$noteDestination/edit/{noteId}/category",
                arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
                deepLinks = listOf(navDeepLink { uriPattern = "companion://$noteDestination/edit/{noteId}" }),
            ) { entry ->

            }
        }
    }


    private fun navigateToNoteSettings(navController: NavController) = navController.navigate("$noteDestination/settings")
    private fun navigateToNoteSingle(navController: NavController, note: Note) = navigateToNoteSingle(navController, note.noteId)
    private fun navigateToNoteSingle(navController: NavController, noteId: Long) = navController.navigate("$noteDestination/view/$noteId")

    private fun navigateToNoteCreate(navController: NavController) = navController.navigate("$noteDestination/create")

    private fun navigateToNoteEdit(navController: NavController, note: Note) = navigateToNoteEdit(navController, note.noteId)
    private fun navigateToNoteEdit(navController: NavController, noteId: Long) = navController.navigate("$noteDestination/edit/$noteId")

    companion object {
        val noteDestination: String = CompanionScreen.Note.name

        @Composable
        fun rememberState(navController: NavHostController = rememberNavController(), noteViewModel: NoteViewModel) =
            remember(navController) { NoteState(navController, noteViewModel) }
    }
}

class CactusState(private val navController: NavHostController) {
    fun NavGraphBuilder.cactusGraph() {
        navigation(startDestination = cactusDestination, route = "cactus") {
            composable(cactusDestination) { // Overview
                CactusBody(onCactusClick = { })
            }
        }
    }

    companion object {
        val cactusDestination = CompanionScreen.Cactus.name
        @Composable
        fun rememberState(
            navController: NavHostController = rememberNavController(),
        ) = remember(navController) {
            CactusState(navController)
        }
    }
}

class AnniversaryState(private val navController: NavHostController, private val anniversaryViewModel: AnniversaryViewModel) {
    fun load() {
        anniversaryViewModel.load()
    }

    fun NavGraphBuilder.anniversaryGraph() {
        navigation(startDestination = anniversaryDestination, route = "anniversary") {
            composable(anniversaryDestination) { // Overview
                val anniversaries by anniversaryViewModel.anniversaries.collectAsState()

                AnniversaryBody(anniversaryList = anniversaries,
                    onNewClick = { navigateToCreateAnniversary(navController) },
                    onAnniversaryClick = {anniversary -> navigateToSingleAnniversary(navController = navController, anniversary = anniversary) },
                    onFavoriteClick = {anniversary -> })
            }
            composable(
                route = "$anniversaryDestination/create",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://$anniversaryDestination/create"
                    }
                )
            ) {
                Timber.d("Creating a new anniversary")
                // TODO: Implement anniversary creation
            }
        }
    }

    private fun navigateToCreateAnniversary(navController: NavHostController) = navController.navigate("${CompanionScreen.Anniversary.name}/create")
    private fun navigateToSingleAnniversary(navController: NavHostController, anniversary: Anniversary) = navigateToSingleAnniversary(navController, anniversary.name)
    private fun navigateToSingleAnniversary(navController: NavHostController, anniversary: String) = navController.navigate("${CompanionScreen.Anniversary.name}/${anniversary}")


    companion object {
        val anniversaryDestination = CompanionScreen.Anniversary.name

        @Composable
        fun rememberState(navController: NavHostController = rememberNavController(), anniversaryViewModel: AnniversaryViewModel)
        = remember(navController) { AnniversaryState(navController, anniversaryViewModel) }
    }
}


@Composable
fun Greeting(name: String) {
    Column {
        Text(text = "Hello $name!")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CompanionTheme {
        Greeting("Android")
    }
}