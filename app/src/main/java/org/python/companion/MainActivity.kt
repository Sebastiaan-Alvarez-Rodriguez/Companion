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
import org.python.companion.ui.note.SingleNoteBody
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
                    { Note -> })
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
                    Timber.e("Navcontroller navigation view note - note name == null")
                } else {
                    SingleNoteBody(note = noteName)
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
                Timber.d("Creating a new note")
                var noteOverrideDialogMiniState = NoteOverrideDialogMiniState.rememberState(null, null, false)
                EditNoteBody(
                    note = null,
                    overrideDialogMiniState = noteOverrideDialogMiniState,
                    onSaveClick = { note ->
                        noteViewModel.with {
                            val conflict = noteViewModel.getbyName(note.name)
                            Timber.d("New note has conflict: ${conflict!=null}")
                            if (conflict == null) {
                                val success = noteViewModel.add(note)
                                navController.navigateUp()
                            } else {
                                noteOverrideDialogMiniState.open(note, conflict)
                            }
                        }
                    },
                    onOverrideAcceptClick = { note ->
                        Timber.d("Overriding note ${note.name}...")
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
                Timber.d("Edit an existing note")
                val noteName = entry.arguments?.getString("note")
                if (noteName == null) {
                    Timber.e("Navcontroller navigation edit note - note name == null")
                } else {
                    val saveNote: (Note) -> Unit = {
                        /* TODO: Save note */
                        navController.navigateUp()
                    }
                    val noteOverrideDialogMiniState = NoteOverrideDialogMiniState.rememberState(null, null, false)
                    EditNoteBody(
                        note = null,
                        overrideDialogMiniState = noteOverrideDialogMiniState,
                        onSaveClick = saveNote,
                        onOverrideAcceptClick = { /* TODO: Override noteOverrideDialogMiniState.overridenNote with note in this lambda */ }
                    )
                }
            }
        }
    }

    private fun navigateToSingleNote(navController: NavController, note: Note) = navigateToSingleNote(navController, note.name)
    private fun navigateToSingleNote(navController: NavController, note: String) = navController.navigate("${CompanionScreen.Note.name}/view/$note")

    private fun navigateToCreateNote(navController: NavController) = navController.navigate("${CompanionScreen.Note.name}/create")

    private fun navigateToEditNote(navController: NavController, note: Note) = navigateToEditNote(navController, note.name)
    private fun navigateToEditNote(navController: NavController, note: String) = navController.navigate("${CompanionScreen.Note.name}/edit/$note") //{ popUpTo(CompanionScreen.Note.name) }

    private fun navigateToPreviewNote(navController: NavController, note: Note) = navigateToEditNote(navController, note.name)
    private fun navigateToPreviewNote(navController: NavController, note: String) = navController.navigate("${CompanionScreen.Note.name}/edit/$note")

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