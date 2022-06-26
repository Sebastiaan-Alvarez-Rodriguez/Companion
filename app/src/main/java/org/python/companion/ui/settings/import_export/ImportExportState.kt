package org.python.companion.ui.settings.import_export

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.Type
import org.python.backend.data.datatype.Note
import org.python.companion.support.UiUtil
import org.python.companion.ui.note.NoteState
import org.python.companion.ui.security.SecurityState
import org.python.companion.viewmodels.NoteViewModel
import org.python.exim.Export
import org.python.exim.Exportable
import org.python.exim.Exports
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

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

//                importView.setOnClickListener(v -> {
//                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*");
//                    intent.addCategory(Intent.CATEGORY_OPENABLE);
//                    Intent finalIntent = Intent.createChooser(intent, "Select file to import from");
//                    startActivityForResult(finalIntent, REQUEST_CODE_IMPORT);
//                });
//                exportView.setOnClickListener(v -> {
//                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("*/*");
//                    intent.addCategory(Intent.CATEGORY_OPENABLE);
//                    Intent finalIntent = Intent.createChooser(intent, "Select location to export to");
//                    startActivityForResult(finalIntent, REQUEST_CODE_EXPORT);
//                });
                val location = rememberSaveable { mutableStateOf<Uri?>(null) }
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) {
                    location.value = it
                }

                val password = rememberSaveable { mutableStateOf("") }

                if (!hasSecureNotes || isAuthorized > 0) {
                    ImportExportScreenSettings(
                        isExport = true,
                        path = location.value?.path,
                        password = password.value,
                        onLocationSelectClick = { launcher.launch("*/*") },
                        onPasswordChange = { password.value = it },
                        onBackClick = { navController.navigateUp() },
                        onStartClick = {
                            val _location = location.value
                            val _password = password.value
                            if (_location == null) {
                                //TODO: display error
                                return@ImportExportScreenSettings
                            }
                            if (_password.isEmpty()) {
                                //TODO: display error
                                return@ImportExportScreenSettings
                            }
                            navigateToExportExecute(navController, _location, _password) }
                    )
                } else {
                    // TODO "Log in to continue" -> log in
                    SecurityState.navigateToSecurityPick(navController = navController, onPicked = { type -> SecurityState.navigateToLogin(type, navController = navController)})
                }
            }

            composable(route = "$navigationStart/execute/{location}/{password}") { entry ->
                val location: String = entry.arguments?.getString("title") ?: throw IllegalStateException("Exporting requires a location")
                val pass: String = entry.arguments?.getString("title") ?: throw IllegalStateException("Exporting requires a password")

                val hasSecureNotes by noteViewModel.hasSecureNotes.collectAsState()
                val isAuthorized by noteViewModel.securityActor.clearance.collectAsState()

                require(!hasSecureNotes || isAuthorized > 0)

                var progressNotes by remember { mutableStateOf(0f) }
                var progressZipNotes by remember { mutableStateOf(0f) }
                var detailsDescription by remember { mutableStateOf("") }

                val context = LocalContext.current
                val outputDir = context.cacheDir // context being the Activity pointer
                val tmpNotesFile = File.createTempFile("notes", ".pq", outputDir)

                ImportExportExecutionScreen(
                    progress = listOf(progressNotes, progressZipNotes),
                    detailsDescription = detailsDescription,
                    onBackClick = {
                        navigateToStop(navController, isExport = true) {
                            tmpNotesFile.delete()
                            navController.navigateUp()
                        }
                    },
                )

                LaunchedEffect(true) {
                    val exportJob = doExport(
                        data = noteViewModel.getAll(),
                        outputFile = tmpNotesFile
                    ) { progress, item ->
                        progressNotes = progress
                        detailsDescription = "Processing note '${item.name}'"
                    }
                    exportJob.join()

                    val zippingJob = doZip(
                        input = tmpNotesFile,
                        password = pass.toCharArray(),
                        destination = Paths.get(location)
                    ) { progress ->
                        progressZipNotes = progress
                        detailsDescription = "Archiving notes..."
                    }

                    val zippingState = zippingJob.await()
                    if (zippingState.state != Export.FinishState.SUCCESS) {
                        scaffoldState.snackbarHostState.showSnackbar(
                            message = zippingState.error ?: "Error during zipping"
                        )
                    }
                    tmpNotesFile.delete()
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

//    private fun doPickFileImport(): Intent {
//        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
//        intent.type = "*/*"
//        intent.addCategory(Intent.CATEGORY_OPENABLE)
//        val finalIntent = Intent.createChooser(intent, "Select location to export to");
//        return finalIntent
//    }

    private suspend fun <T: Exportable> doExport(
        data: List<T>,
        outputFile: File,
        onProgress: (Float, T) -> Unit,
    ): Job {
        val types: List<Type> = Note.EMPTY.values().map { item -> Exports.parquet.transform(item.value, item.name) }
        val parquetExport = Exports.parquet(schema = MessageType("note", types))

        return Export.export(
            type = parquetExport,
            destination = outputFile,
            content = data
        ) { item: T, amountProcessed: Long ->
            onProgress(amountProcessed.toFloat() / data.size, item)
        }
    }

    private suspend fun doZip(
        input: File,
        password: CharArray,
        destination: Path,
        onProgress: (Float) -> Unit
    ): Deferred<Export.ZippingState> {
        //TODO: When also writing categories: write lock?
        return Export.zip(
            file = input,
            password = password,
            pollTimeMS = 80,
            path = destination,
            onProgress = onProgress
        )
    }

    companion object {
        const val navigationStart: String = "${NoteState.noteDestination}/export"

        fun navigateToExport(navController: NavController) =
            navController.navigate(navigationStart)

        private fun navigateToExportExecute(navController: NavController, location: Uri, password: String) =
            navController.navigate(UiUtil.createRoute(
                "$navigationStart/execute",
                args = listOf(location.toString(), password)
            ))

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
