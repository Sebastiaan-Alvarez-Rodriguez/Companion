package org.python.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.lifecycle.viewModelScope
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.python.backend.datatype.Anniversary
import org.python.backend.datatype.Note
import org.python.companion.ui.anniversary.AnniversaryBody
import org.python.companion.ui.cactus.CactusBody
import org.python.companion.ui.components.CompanionScreen
import org.python.companion.ui.components.CompanionTabRow
import org.python.companion.ui.note.EditNoteBody
import org.python.companion.ui.note.NoteBody
import org.python.companion.ui.note.NoteOverrideDialog
import org.python.companion.ui.note.SingleNoteBody
import org.python.companion.ui.theme.CompanionTheme
import org.python.companion.viewmodels.NoteViewModel
import timber.log.Timber


class MainActivity : ComponentActivity() {

    private val noteViewModel by viewModels<NoteViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        noteViewModel.load()
        setContent {
            CompanionTheme {
                val allScreens = CompanionScreen.values().toList()
                val navController = rememberNavController()
                val backstackEntry = navController.currentBackStackEntryAsState()
                val currentScreen = CompanionScreen.fromRoute(backstackEntry.value?.destination?.route)

                val noteState = NoteState.rememberState(navController = navController, noteViewModel = noteViewModel)
                val cactusState = CactusState.rememberState(navController = navController)
                val anniversaryState = AnniversaryState.rememberState(navController = navController)
//                A surface container using the 'background' color from the theme
//                Surface(color = MaterialTheme.colors.background) {
                Scaffold(
                    topBar = {
                        CompanionTabRow(
                            allScreens = allScreens,
                            onTabSelected = { screen -> navController.navigate(screen.name) },
                            currentScreen = currentScreen
                        )
                    }
                ) { innerPadding ->
                    CompanionNavHost(
                        appNavController = navController,
                        noteState = noteState,
                        cactusState = cactusState,
                        anniversaryState = anniversaryState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
//                }
            }
        }
    }
}


@Composable
fun CompanionNavHost(
    appNavController: NavHostController,
    noteState: NoteState,
    cactusState: CactusState,
    anniversaryState: AnniversaryState,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = appNavController,
        startDestination = "cactus",
        modifier = modifier
    ) {
        with(cactusState) { cactusGraph() }
        with(noteState) { noteGraph() }
        with(anniversaryState) { anniversaryGraph() }
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
                EditNoteBody(note = null, onSaveClick = { note ->
                    noteViewModel.with {
                        //TODO: Add save handling here
//                        val success = noteViewModel.add(note)
                        val conflict = noteViewModel.getbyName(note.name)
                        Timber.d("New note has conflict: ${conflict!=null}")
                        if (conflict != null) {
                            NoteOverrideDialog(
                                currentNote = note,
                                overridenNote = conflict,
                                onDismiss = {},
                                onNegativeClick = {},
                                onPositiveClick = {
                                    // TODO: Override old note
                                }
                            )
                        } else {
                            navController.navigateUp()
                        }
                    }
                })
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
                    EditNoteBody(note = null, onSaveClick = { navController.navigateUp() })
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

class AnniversaryState(private val navController: NavHostController) {
    fun NavGraphBuilder.anniversaryGraph() {
        val anniversaryTabName = CompanionScreen.Anniversary.name
        navigation(startDestination = anniversaryTabName, route = "anniversary") {
            composable(anniversaryTabName) { // Overview
                AnniversaryBody(anniversaryList = emptyList(),
                    {anniversary -> navigateToSingleAnniversary(navController = navController, anniversary = anniversary) },
                    {anniversary -> })
            }
        }
    }

    private fun navigateToSingleAnniversary(navController: NavHostController, anniversary: Anniversary) = navigateToSingleAnniversary(navController, anniversary.name)
    private fun navigateToSingleAnniversary(navController: NavHostController, anniversary: String) = navController.navigate("${CompanionScreen.Anniversary.name}/${anniversary}")


    companion object {
        @Composable
        fun rememberState(
            navController: NavHostController = rememberNavController(),
        ) = remember(navController) {
            AnniversaryState(navController)
        }
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