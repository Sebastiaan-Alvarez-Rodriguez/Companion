package org.python.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.python.backend.datatype.Anniversary
import org.python.backend.datatype.Note
import org.python.companion.ui.anniversary.AnniversaryBody
import org.python.companion.ui.cactus.CactusBody
import org.python.companion.ui.components.CompanionScreen
import org.python.companion.ui.components.CompanionTabRow
import org.python.companion.ui.note.EditNoteBody
import org.python.companion.ui.note.NoteBody
import org.python.companion.ui.note.NoteOverrideDialogMiniState
import org.python.companion.ui.note.NoteViewBody
import org.python.companion.ui.splash.SplashBuilder
import org.python.companion.ui.theme.CompanionTheme
import org.python.companion.viewmodels.AnniversaryViewModel
import org.python.companion.viewmodels.NoteViewModel
import timber.log.Timber


class MainActivity : ComponentActivity() {

    private val noteViewModel by viewModels<NoteViewModel>()
    private val anniversaryViewModel by viewModels<AnniversaryViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompanionTheme {
                val allScreens = CompanionScreen.values().toList()
                val navController = rememberNavController()
                val backstackEntry = navController.currentBackStackEntryAsState()
                val selectedTabScreen = CompanionScreen.fromRoute(backstackEntry.value?.destination?.route)

                val noteState = NoteState.rememberState(navController = navController, noteViewModel = noteViewModel)
                val cactusState = CactusState.rememberState(navController = navController)
                val anniversaryState = AnniversaryState.rememberState(navController = navController, anniversaryViewModel = anniversaryViewModel)

                Scaffold(
                    topBar = {
                        CompanionTabRow(
                            allScreens = allScreens,
                            onTabSelected = { screen ->
                                    Timber.w("Got tab selected: ${screen.name}")
                                     navController.navigate(screen.name) },
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
                                SplashBuilder(navController = navController, destination = CompanionScreen.Cactus.name).build {
                                    noteViewModel.load()
                                    anniversaryViewModel.load()
                                }
                            }
                            splashScreenFunc()
                        }
                        with(cactusState) { cactusGraph() }
                        with(noteState) { noteGraph() }
                        with(anniversaryState) { anniversaryGraph() }
                    }
                }
            }
        }
    }
}

class NoteState(private val navController: NavHostController, private val noteViewModel: NoteViewModel) {
    fun NavGraphBuilder.noteGraph() {
        val noteTabName = CompanionScreen.Note.name

        navigation(startDestination = noteTabName, route = "note") {
            composable(noteTabName) {
                val notes by noteViewModel.notes.collectAsState()
                NoteBody(notes = notes,
                    { navigateToCreateNote(navController = navController) },
                    { note -> navigateToSingleNote(navController = navController, note = note) },
                    { note -> })
            }
            composable(
                route = "$noteTabName/view/{note}",
                arguments = listOf(
                    navArgument("note") {
                        type = NavType.StringType
                    }
                ),
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://$noteTabName/view/{note}"
                    }
                ),
            ) { entry ->
                val noteName = entry.arguments?.getString("note")
                if (noteName == null) {
                    Timber.e("View note: Navcontroller navigation - note name == null")
                } else {
                    var note by remember { mutableStateOf<Note?>(null) }
                    noteViewModel.with {
                        note = noteViewModel.getbyName(noteName)
                    }
                    if (note != null)
                        NoteViewBody(
                            note = note!!,
                            onEditClick = { navigateToEditNote(navController = navController, note = it) },
                            onDeleteClick = {
                                noteViewModel.with {
                                    noteViewModel.delete(it)
                                }
                                navController.navigateUp()
                            })
                }
            }
            composable(
                route = "$noteTabName/create",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://$noteTabName/create"
                    }
                )
            ) {
                Timber.d("New note: Creating")
                val noteOverrideDialogMiniState = NoteOverrideDialogMiniState.rememberState(null, null, false)
                EditNoteBody(
                    note = null,
                    overrideDialogMiniState = noteOverrideDialogMiniState,
                    onSaveClick = { note ->
                        noteViewModel.with {
                            val conflict = noteViewModel.getbyName(note.name)
                            Timber.d("New note: conflict: ${conflict!=null}")
                            if (conflict == null) {
                                val success = noteViewModel.add(note)
                                navController.navigateUp()
                            } else {
                                noteOverrideDialogMiniState.open(note, conflict)
                            }
                        }
                    },
                    onOverrideAcceptClick = { note ->
                        Timber.d("New note: Overriding ${note.name}...")
                        noteViewModel.with {
                            noteViewModel.upsert(note)
                        }
                        noteOverrideDialogMiniState.close()
                        navController.navigateUp()
                    }
                )
            }
            composable(
                route = "$noteTabName/edit/{note}",
                arguments = listOf(
                    navArgument("note") {
                        type = NavType.StringType
                    }
                ),
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://$noteTabName/edit/{note}"
                    }
                ),
            ) { entry ->
                Timber.d("Edit note: Editing")
                val noteName = entry.arguments?.getString("note")
                if (noteName == null) {
                    Timber.e("Edit note: Navcontroller navigation: note name == null")
                } else {
                    val noteOverrideDialogMiniState = NoteOverrideDialogMiniState.rememberState(null, null, false)
                    var existingNote by remember { mutableStateOf<Note?>(null) }
                    noteViewModel.with {
                        existingNote = noteViewModel.getbyName(noteName)!!
                    }
                    if (existingNote != null) {
                        EditNoteBody(
                            note = existingNote,
                            overrideDialogMiniState = noteOverrideDialogMiniState,
                            onSaveClick = { note ->
                                noteViewModel.with {
                                    // If note name == same as before, there is no conflict. Otherwise, we must check.
                                    val conflict: Note? =
                                        if (note.name == noteName) null else noteViewModel.getbyName(
                                            note.name
                                        )
                                    Timber.d("Edit note: edited note has changed name=${note.name != noteName}, now conflict: ${conflict != null}")
                                    if (conflict == null) {
                                        val success = noteViewModel.update(note)
                                        navController.navigateUp()
                                    } else {
                                        noteOverrideDialogMiniState.open(note, conflict)
                                    }
                                }
                            },
                            onOverrideAcceptClick = { note ->
                                Timber.d("Edit note: Overriding note (old name=${noteName}) (new name=${note.name})")
                                noteViewModel.with {
                                    noteViewModel.delete(existingNote!!)
                                    noteViewModel.upsert(note)
                                }
                                noteOverrideDialogMiniState.close()
                                navController.navigateUp()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun navigateToSingleNote(navController: NavController, note: Note) = navigateToSingleNote(navController, note.name)
    private fun navigateToSingleNote(navController: NavController, note: String) = navController.navigate("${CompanionScreen.Note.name}/view/$note")

    private fun navigateToCreateNote(navController: NavController) = navController.navigate("${CompanionScreen.Note.name}/create")

    private fun navigateToEditNote(navController: NavController, note: Note) = navigateToEditNote(navController, note.name)
    private fun navigateToEditNote(navController: NavController, note: String) = navController.navigate("${CompanionScreen.Note.name}/edit/$note") //{ popUpTo(CompanionScreen.Note.name) }

    companion object {
        @Composable
        fun rememberState(navController: NavHostController = rememberNavController(), noteViewModel: NoteViewModel) =
            remember(navController) { NoteState(navController, noteViewModel) }
    }
}

class CactusState(private val navController: NavHostController) {
    fun NavGraphBuilder.cactusGraph() {
        val cactusTabName = CompanionScreen.Cactus.name
        navigation(startDestination = cactusTabName, route = "cactus") {
            composable(cactusTabName) { // Overview
                CactusBody(onCactusClick = { })
            }
        }
    }

    companion object {
        @Composable
        fun rememberState(
            navController: NavHostController = rememberNavController(),
        ) = remember(navController) {
            CactusState(navController)
        }
    }
}

class AnniversaryState(private val navController: NavHostController, private val anniversaryViewModel: AnniversaryViewModel) {
    fun NavGraphBuilder.anniversaryGraph() {
        val anniversaryTabName = CompanionScreen.Anniversary.name
        navigation(startDestination = anniversaryTabName, route = "anniversary") {
            composable(anniversaryTabName) { // Overview
                val anniversaries by anniversaryViewModel.anniversaries.collectAsState()

                AnniversaryBody(anniversaryList = anniversaries,
                    onNewClick = { navigateToCreateAnniversary(navController) },
                    onAnniversaryClick = {anniversary -> navigateToSingleAnniversary(navController = navController, anniversary = anniversary) },
                    onFavoriteClick = {anniversary -> })
            }
            composable(
                route = "$anniversaryTabName/create",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://$anniversaryTabName/create"
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