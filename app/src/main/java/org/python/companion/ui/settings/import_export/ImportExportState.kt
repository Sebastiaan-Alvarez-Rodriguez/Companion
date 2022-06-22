package org.python.companion.ui.settings.import_export

import androidx.activity.compose.BackHandler
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import org.python.companion.support.UiUtil
import org.python.companion.ui.note.NoteState
import org.python.companion.ui.security.SecurityState
import org.python.companion.viewmodels.NoteViewModel

class ImportExportState(
    private val navController: NavHostController,
    private val noteViewModel: NoteViewModel,
    private val scaffoldState: ScaffoldState
) {

    fun NavGraphBuilder.importExportGraph() {
        navigation(startDestination = navigationStart, route = "export") {
            composable(route = navigationStart) {
                val hasSecureNotes by noteViewModel.hasSecureNotes.collectAsState()
                val isAuthorized by noteViewModel.securityActor.clearance.collectAsState()

                if (!hasSecureNotes || isAuthorized > 0) {
                    ImportExportScreenSettings(
                        isExport = true,
                        onStartClick = { /* TODO ask for password for backup first */ navigateToExportExecute(navController) },
                        onBackClick = { navController.navigateUp() }
                    )
                } else {
                    // TODO "Log in to continue" -> log in
                    SecurityState.navigateToSecurityPick(navController = navController, onPicked = { type -> SecurityState.navigateToLogin(type, navController = navController)})
                }
            }
            composable(route = "$navigationStart/execute") {
                val hasSecureNotes by noteViewModel.hasSecureNotes.collectAsState()
                val isAuthorized by noteViewModel.securityActor.clearance.collectAsState()

                require(!hasSecureNotes || isAuthorized > 0)

                var progress by remember { mutableStateOf(0f) }
                var detailsDescription by remember { mutableStateOf("") }


                ImportExportExecutionScreen(
                    progress = progress,
                    detailsDescription = detailsDescription,
                    onBackClick = {
                        navigateToStop(navController, isExport = true) {
                            // TODO: delete export file & go back.
                            navController.navigateUp()
                        }
                    },
                )

                LaunchedEffect(true) {
                    // TODO: Pick a file first.
                    val notes = noteViewModel.getAll()
                    // TODO: end by navigating to success or going up.
                    // TODO: Use snappy-compressed parquet.

                }


                BackHandler(enabled = true) {
                    navigateToStop(navController, isExport = true) {
                        // TODO: delete export file & go back.
                        navController.navigateUp()
                    }
                }
            }
        }
    }



    companion object {
        const val navigationStart: String = "${NoteState.noteDestination}/export"

        fun navigateToExport(navController: NavController) =
            navController.navigate(navigationStart)

        private fun navigateToExportExecute(navController: NavController) =
            navController.navigate("$navigationStart/execute")

        private fun navigateToStop(navController: NavHostController, isExport: Boolean = true, onStopClick: () -> Unit) =
            UiUtil.UIUtilState.navigateToBinary(
                navController = navController,
                title = "${if (isExport) "Exporting" else "Importing"} unfinished",
                message = "Are you sure you want to go back? ${if (isExport) "Export" else "Import"} process will be cancelled.",
                positiveText = "Stop ${if (isExport) "exporting" else "importing"}",
                onPositiveClick = { if (it) onStopClick() }
            )

        @Composable
        fun rememberState(navController: NavHostController = rememberNavController(), noteViewModel: NoteViewModel, scaffoldState: ScaffoldState) =
            remember(navController) { ImportExportState(navController, noteViewModel, scaffoldState) }
    }
}
