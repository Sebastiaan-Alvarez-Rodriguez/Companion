package org.python.companion.ui.settings

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.*
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.python.companion.ui.note.NoteScreenSettings
import org.python.companion.ui.note.NoteState
import org.python.companion.ui.security.SecurityState
import org.python.companion.viewmodels.NoteViewModel

class SettingsState(
    private val navController: NavHostController,
    private val noteViewModel: NoteViewModel,
    private val scaffoldState: ScaffoldState
) {

    fun NavGraphBuilder.settingsGraph() {
        navigation(startDestination = navigationStart, route = "settings") {
            composable(
                route = navigationStart,
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://$navigationStart/settings"
                    }
                )
            ) {
                NoteScreenSettings(
                    onSecuritySetupClick = {
                        SecurityState.navigateToSecurityPick(
                            navController,
                            allowedMethods = noteViewModel.securityActor.notSetupMethods(),
                            autoPick = false,
                            disallowedReason = "Can only setup security methods that are not setup yet.",
                            key = "pickForSetup",
                            onPicked = { type -> SecurityState.navigateToSetup(type, navController) }
                        )
                    },
                    onSecurityResetClick = {
                        SecurityState.navigateToSecurityPick(
                            navController,
                            autoPick = false,
                            allowedMethods = noteViewModel.securityActor.setupMethods(),
                            disallowedReason = "Can only reset security methods that are setup.",
                            key = "pickForReset",
                            onPicked = { type -> SecurityState.navigateToReset(type, navController) }
                        )
                    },
                    onExportClick = { /* TODO */ },
                    onImportClick = { /* TODO */ },
                    onBackClick = { navController.navigateUp() }
                )
            }
        }
    }



    companion object {
        const val navigationStart: String = "${NoteState.noteDestination}/settings"

        fun navigateToSettings(navController: NavController) =
            navController.navigate(navigationStart)

        @Composable
        fun rememberState(navController: NavHostController = rememberNavController(), noteViewModel: NoteViewModel, scaffoldState: ScaffoldState) =
            remember(navController) { SettingsState(navController, noteViewModel, scaffoldState) }
    }
}
