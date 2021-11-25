package org.python.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import org.python.companion.datatype.Anniversary
import org.python.companion.datatype.Note
import org.python.companion.ui.anniversary.AnniversaryBody
import org.python.companion.ui.cactus.CactusBody
import org.python.companion.ui.components.CompanionScreen
import org.python.companion.ui.components.CompanionTabRow
import org.python.companion.ui.note.NoteBody
import org.python.companion.ui.note.SingleNoteBody
import org.python.companion.ui.theme.CompanionTheme
import timber.log.Timber


// Basic UI: https://developer.android.com/jetpack/compose/tutorial
// Navigation: https://developer.android.com/codelabs/jetpack-compose-navigation?continue=https%3A%2F%2Fdeveloper.android.com%2Fcourses%2Fpathways%2Fcompose%23codelab-https%3A%2F%2Fdeveloper.android.com%2Fcodelabs%2Fjetpack-compose-navigation#0
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompanionTheme {
                val allScreens = CompanionScreen.values().toList()
                val navController = rememberNavController()
                val backstackEntry = navController.currentBackStackEntryAsState()
                val currentScreen = CompanionScreen.fromRoute(backstackEntry.value?.destination?.route)

                // A surface container using the 'background' color from the theme
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
                        CompanionNavHost(navController, modifier = Modifier.padding(innerPadding))
                    }

//                }
            }
        }
    }
}


@Composable
fun CompanionNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = CompanionScreen.Cactus.name,
        modifier = modifier
    ) {
        composable(CompanionScreen.Cactus.name) { // Overview
            CactusBody(onCactusClick = { })
        }
        composable(CompanionScreen.Note.name) { // Accounts
            NoteBody(noteList = emptyList(),
                { navigateToCreateNote(navController = navController) },
                { note -> navigateToSingleNote(navController = navController, note = note) },
                { Note -> })
        }
        composable(CompanionScreen.Anniversaries.name) { // Bills
            AnniversaryBody(anniversaryList = emptyList(),
                {anniversary -> navigateToSingleAnniversary(navController = navController, anniversary = anniversary) },
                {anniversary -> })
        }
        val noteTabName = CompanionScreen.Note.name
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
                Timber.e("Navcontroller navigation - note name == null")
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
        ) { entry ->
            //TODO: go to some view for creating a node
            Timber.d("Creating a new note")
        }
    }
}

private fun navigateToSingleNote(navController: NavHostController, note: Note) = navigateToSingleNote(navController, note.name)
private fun navigateToSingleNote(navController: NavHostController, note: String) = navController.navigate("${CompanionScreen.Note.name}/view/$note")

private fun navigateToCreateNote(navController: NavHostController) = navController.navigate("${CompanionScreen.Note.name}/create")

private fun navigateToEditNote(navController: NavHostController, note: Note) = navigateToEditNote(navController, note.name)
private fun navigateToEditNote(navController: NavHostController, note: String) = navController.navigate("${CompanionScreen.Note.name}/edit/$note")


private fun navigateToSingleAnniversary(navController: NavHostController, anniversary: Anniversary) = navigateToSingleAnniversary(navController, anniversary.name)
private fun navigateToSingleAnniversary(navController: NavHostController, anniversary: String) = navController.navigate("${CompanionScreen.Anniversaries.name}/${anniversary}")



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