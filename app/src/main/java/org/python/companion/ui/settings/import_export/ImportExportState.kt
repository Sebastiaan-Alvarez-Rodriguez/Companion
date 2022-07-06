package org.python.companion.ui.settings.import_export

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.core.content.ContextCompat
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
import org.python.companion.R
import org.python.companion.support.FileUtil
import org.python.companion.support.UiUtil
import org.python.companion.ui.note.NoteState
import org.python.companion.ui.security.SecurityState
import org.python.companion.viewmodels.NoteViewModel
import org.python.datacomm.Result
import org.python.datacomm.ResultType
import org.python.exim.Export
import org.python.exim.Exportable
import org.python.exim.Exports
import timber.log.Timber
import java.io.File
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

                if (hasSecureNotes && isAuthorized <= 0) {
                    // TODO "Log in to continue" -> log in
                    SecurityState.navigateToSecurityPick(
                        navController = navController,
                        onPicked = { type -> SecurityState.navigateToLogin(type, navController = navController)}
                    )
                    return@composable
                }

                // TODO: Use below import code
//                importView.setOnClickListener(v -> {
//                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*");
//                    intent.addCategory(Intent.CATEGORY_OPENABLE);
//                    Intent finalIntent = Intent.createChooser(intent, "Select file to import from");
//                    startActivityForResult(finalIntent, REQUEST_CODE_IMPORT);
//                });

                val isExporting = rememberSaveable { mutableStateOf(false) }

                // settings for exporting
                val location = rememberSaveable { mutableStateOf<Uri?>(null) }
                val password = rememberSaveable { mutableStateOf("") }

                if (!isExporting.value) {
                    ExportSettingsScreen(location, password, isExporting)
                } else {
                    ExportExecuteScreen(location.value!!, password.value)
                }
            }
        }
    }

    @Composable
    private fun ExportSettingsScreen(location: MutableState<Uri?>, password: MutableState<String>, isExporting: MutableState<Boolean>) {
        // error indicators
        var pathError by rememberSaveable { mutableStateOf<String?>(null) }
        var passwordError by rememberSaveable { mutableStateOf<String?>(null) }

        val defaultPadding = dimensionResource(id = R.dimen.padding_default)

        val context = LocalContext.current

        // launcher to make user select file
        val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) {
            location.value = it
        }

        val requestLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                isExporting.value = true
            } else {
                // TODO Snackbar: 'need permission'
            }
        }
        ImportExportScreenSettings(
            progressContent = { NestedCircularProgressIndicator(progresses = listOf(1f, 1f, 1f)) },
            subContent = {
                ExportPickFileCard(location.value?.path, pathError) {
                    fileLauncher.launch(zipName())
                    pathError = null
                }
                Spacer(Modifier.height(defaultPadding))
                ExportPasswordCard(password.value, passwordError) {
                    password.value = it
                    passwordError = null
                }
                Spacer(Modifier.height(defaultPadding))
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    var hasErrors = false
                    if (location.value == null) {
                        pathError = "Path has not been set"
                        hasErrors = true
                    }
                    if (password.value.length < 1) { //todo make 12 again
                        passwordError = "Password length is too short (must be > 12, was ${password.value.length})"
                        hasErrors = true
                    }
                    if (!hasErrors) {
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) -> isExporting.value = true
                            else -> requestLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                }) {
                    Text("Begin export", modifier = Modifier.padding(defaultPadding))
                }
            },
            onBackClick = { navController.navigateUp() }
        )
    }

    @Composable
    private fun ExportExecuteScreen(location: Uri, password: String) {
        // metrics for exporting
        val progressExportNotes = remember { mutableStateOf(0f) }
        val progressZipNotes = remember { mutableStateOf(0f) }
        val progressCopyZip = remember { mutableStateOf(0f) }

        val detailsDescription = remember { mutableStateOf("") }

        ImportExportScreenSettings(
            progressContent = {
                NestedCircularProgressIndicator(progresses = listOf(progressExportNotes.value, progressZipNotes.value, progressCopyZip.value))
            },
            subContent = {
                DetailsCard(detailsDescription = detailsDescription.value)
            },
            onBackClick = {
                // TODO: Sure you want to go back? -> delete export file & go back.
                navigateToStop(navController, isExport = true) {
                    navController.navigateUp()
                }
            }
        )

        val context = LocalContext.current
        val cacheDir = context.cacheDir

        val contentResolver = LocalContext.current.contentResolver

        LaunchedEffect(true) {

        val exportResult = export(
            cacheDir, contentResolver, location, password,
            progressExportNotes, progressZipNotes, progressCopyZip,
            detailsDescription
        )
        if (exportResult.type == ResultType.FAILED) {
            scaffoldState.snackbarHostState.showSnackbar(
                message = exportResult.message ?: "Error during exporting"
            )
            // TODO: Do something here?
            }
        }
    }

    /**
     * Exports all data to a parquet file.
     * @return error string on error, null otherwise
     */
    // TODO: return better type. I had something for this.
    private suspend fun export(
        cacheDir: File, contentResolver: ContentResolver,
        location: Uri, password: String,
        progressExportNotes: MutableState<Float>,
        progressZipNotes: MutableState<Float>,
        progressCopyZip: MutableState<Float>,
        detailsDescription: MutableState<String>
    ): Result {
        val tmpNotesFile = File.createTempFile("notes", ".pq", cacheDir)
        val tmpZipFile = File.createTempFile("companion", ".zip", cacheDir)

        try {
            val exportJob = doExport(data = noteViewModel.getAll(), outputFile = tmpNotesFile) { progress, item ->
                progressExportNotes.value = progress
                detailsDescription.value = item?.name?.let { "Processing note '${it}'" } ?: "Processing notes"
            } ?: throw IllegalStateException("No notes to process")

            Timber.e("starting export job")
            exportJob.start()
            Timber.e("joining export job")
            exportJob.join()
            Timber.e("launching zip job")

            val zipFilePath = tmpZipFile.path
            tmpZipFile.delete() // Otherwise zip library thinks our empty tmp file is a zip and crashes.
            val zippingJob = doZip(input = tmpNotesFile, password = password.toCharArray(), destination = zipFilePath) { progress ->
                progressZipNotes.value = progress
                detailsDescription.value = "Archiving notes..."
            }
            val zippingState = zippingJob.await()
            if (zippingState.state != Export.FinishState.SUCCESS) {
                return Result(ResultType.FAILED, zippingState.error)
            } else {
                val movingJob = FileUtil.copyStream(
                    size = tmpZipFile.length(),
                    inStream = tmpZipFile.inputStream(),
                    outStream = contentResolver.openOutputStream(location, "w")!!
                ) { progress ->
                    progressCopyZip.value = progress
                    detailsDescription.value = "Moving archive"
                }
                Timber.e("launching move job")
                movingJob.start()
                movingJob.join()
                detailsDescription.value = "Done"
                Timber.e("All jobs completed - success")
            }
            return Result.DEFAULT_SUCCESS
        } finally {
            Timber.e("Cleaning up")
            tmpNotesFile.delete()
            tmpZipFile.delete()
        }
    }

    companion object {
        const val navigationStart: String = "${NoteState.noteDestination}/export"

        fun navigateToExport(navController: NavController) =
            navController.navigate(navigationStart)

        private fun zipName() = "companion-${Instant.now()}"

        /**
         * Handles Parquet exporting.
         * @param data Data to export.
         * @param outputFile output file location.
         * @param onProgress progress lambda.
         * Progress ranges from 0f to 1f.
         * Secondary parameter is the most recently processed item.
         * @return executable job.
         */
        private suspend fun <T: Exportable> doExport(
            data: List<T>,
            outputFile: File,
            onProgress: (Float, T?) -> Unit,
        ): Job? {
            if (data.isEmpty()) {
                onProgress(1f, null)
                return null
            }
            val types: List<Type> = data.first().values().map { item -> Exports.parquet.transform(item.value, item.name) }
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
            destination: String,
            onProgress: (Float) -> Unit
        ): Deferred<Export.ZippingState> {
            //TODO: When also writing categories: write lock?
            return Export.zip(
                file = input,
                password = password,
                pollTimeMS = 80,
                destination = destination,
                onProgress = onProgress
            )
        }

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
