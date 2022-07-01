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
import org.python.companion.support.UiUtil
import org.python.companion.ui.note.NoteState
import org.python.companion.ui.security.SecurityState
import org.python.companion.viewmodels.NoteViewModel
import org.python.exim.Export
import org.python.exim.Exportable
import timber.log.Timber
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant

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
                var location by rememberSaveable { mutableStateOf<Uri?>(null) }
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) {
                    location = it
                }

                var pathError by rememberSaveable { mutableStateOf<String?>(null) }
                var passwordError by rememberSaveable { mutableStateOf<String?>(null) }

                var password by rememberSaveable { mutableStateOf("") }

                if (!hasSecureNotes || isAuthorized > 0) {
                    ImportExportScreenSettings(
                        isExport = true,
                        path = location?.path,
                        password = password,
                        pathError = pathError,
                        passwordError = passwordError,
                        onLocationSelectClick = {
                            launcher.launch(zipName())
                            pathError = null
                        },
                        onPasswordChange = {
                            password = it
                            passwordError = null
                        },
                        onBackClick = { navController.navigateUp() },
                        onStartClick = {
                            val _location = location
                            val _password = password

                            var hasErrors = false
                            if (_location == null) {
                                pathError = "Path has not been set"
                                hasErrors = true
                            }
                            if (_password.length < 1) { //todo make 12 again
                                passwordError = "Password length is too short (must be > 12, was ${_password.length})"
                                hasErrors = true
                            }
                            if (hasErrors)
                                return@ImportExportScreenSettings
                            navigateToExportExecute(navController, _location!!, _password) }
                    )
                } else {
                    // TODO "Log in to continue" -> log in
                    SecurityState.navigateToSecurityPick(navController = navController, onPicked = { type -> SecurityState.navigateToLogin(type, navController = navController)})
                }
            }

            composable(route = "$navigationStart/execute/{location}/{password}") { entry ->
                val location: String = entry.arguments?.getString("location") ?: throw IllegalStateException("Exporting requires a location")
                val pass: String = entry.arguments?.getString("password") ?: throw IllegalStateException("Exporting requires a password")

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
                    Timber.e("launching export job")

                    val exportJob = doExport(
                        data = noteViewModel.getAll(),
                        outputFile = tmpNotesFile
                    ) { progress, item ->
                        progressNotes = progress
                        detailsDescription = "Processing note '${item.name}'"
                        Timber.e("export ($progress%): $detailsDescription")
                    } ?: throw IllegalStateException("No notes to process")//return@LaunchedEffect
                    Timber.e("starting export job")
                    exportJob.start()
                    Timber.e("joining export job")
                    exportJob.join()
                    Timber.e("launching zip job")

                    val zippingJob = doZip(
                        input = tmpNotesFile,
                        password = pass.toCharArray(),
                        destination = Paths.get(location)
                    ) { progress ->
                        progressZipNotes = progress
                        detailsDescription = "Archiving notes..."
                        Timber.e("zip ($progress%): $detailsDescription")
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

    companion object {
        const val navigationStart: String = "${NoteState.noteDestination}/export"

        fun navigateToExport(navController: NavController) =
            navController.navigate(navigationStart)

        private fun zipName() = "companion-${Instant.now()}"

        private suspend fun <T: Exportable> doExport(
            data: List<T>,
            outputFile: File,
            onProgress: (Float, T) -> Unit,
        ): Job? {
            if (data.isEmpty()) {
                return null
            }
//            val types: List<Type> = data.first().values().map { item -> Exports.parquet.transform(item.value, item.name) }
//            val parquetExport = Exports.parquet(schema = MessageType("note", types))

            Timber.e("export2: $data")
            return Export.export(
                type = TODO(),//parquetExport,
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

        private fun navigateToExportExecute(navController: NavController, location: Uri, password: String) =
            navController.navigate(UiUtil.createRoute(
                "$navigationStart/execute",
                args = listOf(Uri.encode(location.path), password)
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
