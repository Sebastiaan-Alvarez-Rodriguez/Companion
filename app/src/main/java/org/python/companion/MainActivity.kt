package org.python.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
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
import org.python.companion.datatype.NoteParcel
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
                Surface(color = MaterialTheme.colors.background) {
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

                }
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
            route = "$noteTabName/{note}",
            arguments = listOf(
                navArgument("note") {
                    type = NavType.ParcelableType(NoteParcel::class.java)
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "companion://$noteTabName/{note.name}"
                }
            ),
        ) { entry ->
            val noteParcel: NoteParcel? = entry.arguments?.getParcelable("note")
            if (noteParcel == null) {
                Timber.e("Navhost $noteTabName/{note} got null note.")
            } else {
                SingleNoteBody(note = noteParcel.member)
            }
        }
    }
}

private fun navigateToSingleNote(navController: NavHostController, note: Note) {
    navController.navigate("${CompanionScreen.Note.name}/${note.name}")
}

private fun navigateToSingleAnniversary(navController: NavHostController, anniversary: Anniversary) {
    navController.navigate("${CompanionScreen.Anniversaries.name}/${anniversary.name}")
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