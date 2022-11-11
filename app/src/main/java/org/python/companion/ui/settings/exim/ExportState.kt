package org.python.companion.ui.settings.exim

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
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
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteCategory
import org.python.companion.R
import org.python.companion.support.FileUtil
import org.python.companion.support.PermissionUtil
import org.python.companion.support.UiUtil
import org.python.companion.ui.note.NoteState
import org.python.companion.ui.security.SecurityState
import org.python.companion.ui.settings.exim.Shared.NOTECATEGORYFILE_NAME
import org.python.companion.ui.settings.exim.Shared.NOTEFILE_NAME
import org.python.companion.viewmodels.NoteCategoryViewModel
import org.python.companion.viewmodels.NoteViewModel
import org.python.datacomm.Result
import org.python.datacomm.ResultType
import org.python.exim.EximUtil
import org.python.exim.Export
import org.python.exim.Exportable
import org.python.exim.Exports
import timber.log.Timber
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.deleteExisting

class ExportState(
    private val navController: NavHostController,
    private val noteViewModel: NoteViewModel,
    private val noteCategoryViewModel: NoteCategoryViewModel,
    private val scaffoldState: ScaffoldState
) {
    fun NavGraphBuilder.exportGraph() {
        navigation(startDestination = navigationStart, route = "exim") {
            composable(route = navigationStart) {
                val hasSecureNotes by noteViewModel.hasSecureNotes.collectAsState()
                val isAuthorized by noteViewModel.securityActor.clearance.collectAsState()

                val isExporting = rememberSaveable { mutableStateOf(false) }

                // settings for exporting
                val location = rememberSaveable { mutableStateOf<Uri?>(null) }
                val password = rememberSaveable { mutableStateOf("") }

                if (hasSecureNotes && isAuthorized <= 0) {
                    ImportExportScreenSettings(
                        progressContent = { NestedCircularProgressIndicator(progresses = listOf(1f, 1f, 1f, 1f)) },
                        subContent = {
                            val defaultPadding = dimensionResource(id = R.dimen.padding_default)
                            UiUtil.SimpleActionSingular(
                                title = "Login",
                                message = "It seems there are some secured notes.\nLogin first to export them.",
                                buttonText = "Login",
                                modifier = Modifier.fillMaxSize().padding(defaultPadding)
                            ) {
                                SecurityState.navigateToSecurityPick(navController, onPicked = { type -> SecurityState.navigateToLogin(type, navController = navController)})
                            }
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                } else if (!isExporting.value) {
                    ExportSettingsScreen(location, password, isExporting)
                } else {
                    ExportExecuteScreen(location.value!!, password.value)
                }
            }
        }
    }

    @Composable
    private fun ExportSettingsScreen(
        location: MutableState<Uri?>,
        password: MutableState<String>,
        isExporting: MutableState<Boolean>
    ) {
        // error indicators
        var pathError by rememberSaveable { mutableStateOf<String?>(null) }
        var passwordError by rememberSaveable { mutableStateOf<String?>(null) }

        val defaultPadding = dimensionResource(id = R.dimen.padding_default)

        val context = LocalContext.current

        // launcher to make user select file
        val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) {
            location.value = it
        }

        val requestLauncher = PermissionUtil.requestExternalStoragePermission(navController, onGranted = { fileLauncher.launch(zipName()) })

        ImportExportScreenSettings(
            progressContent = { NestedCircularProgressIndicator(progresses = listOf(1f, 1f, 1f, 1f)) },
            subContent = {
                PickFileCard(
                    path = location.value?.path,
                    explanationText = "pick a path to place the backup.",
                    pathError = pathError
                ) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                        fileLauncher.launch(zipName())
                    else
                        requestLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
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
                    if (password.value.length < 12) {
                        passwordError = "Password length is too short (must be > 12, was ${password.value.length})"
                        hasErrors = true
                    }
                    if (!hasErrors) {
                        isExporting.value = true
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
        val progressExportNoteCategories = remember { mutableStateOf(0f) }
        val progressZipNotes = remember { mutableStateOf(0f) }
        val progressCopyZip = remember { mutableStateOf(0f) }

        val detailsDescription = remember { mutableStateOf("") }

        var exportResult by remember { mutableStateOf<Result?>(null) }

        val context = LocalContext.current
        val cacheDir = context.cacheDir

        val contentResolver = LocalContext.current.contentResolver

        ImportExportScreenSettings(
            progressContent = {
                FinishingNestedCircularProgressIndicator(
                    result = exportResult,
                    progresses = listOf(progressExportNotes.value, progressExportNoteCategories.value, progressZipNotes.value, progressCopyZip.value)
                )
            },
            subContent = {
                DetailsCard(detailsDescription = detailsDescription.value)
            },
            onBackClick = {
                if (exportResult == null) {
                    Shared.navigateToStop(navController, isExport = true) {
                        cacheDir.walkBottomUp().onLeave { it.delete() }
                        navController.navigateUp()
                    }
                } else {
                    cacheDir.walkBottomUp().onLeave { it.delete() }
                    navController.navigateUp()
                }
            }
        )

        var exportTrigger by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(exportTrigger) {
            exportResult = null
            exportResult = export(
                noteViewModel, noteCategoryViewModel, cacheDir, contentResolver, location, password,
                progressExportNotes, progressExportNoteCategories, progressZipNotes, progressCopyZip,
                detailsDescription
            )
            if (exportResult!!.type == ResultType.FAILED) {
                val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                    message = exportResult!!.message ?: "Error during exporting",
                    duration = SnackbarDuration.Indefinite,
                    actionLabel = "Retry"
                )
                when (snackbarResult) {
                    SnackbarResult.ActionPerformed -> exportTrigger = !exportTrigger // trigger retry
                    else -> {}
                }
            }
        }
    }

    companion object {
        const val navigationStart: String = "${NoteState.noteDestination}/exim"

        private const val ZIP_EXTENSION = "zip"

        fun navigateToExport(navController: NavController) = navController.navigate(navigationStart)

        private fun zipName() = "companion-${Instant.now()}"

        /**
         * Exports all data to a parquet file.
         * @return result containing error message on error, success otherwise
         */
        private suspend fun export(
            noteViewModel: NoteViewModel,
            noteCategoryViewModel: NoteCategoryViewModel,
            cacheDir: File, contentResolver: ContentResolver,
            location: Uri, password: String,
            progressExportNotes: MutableState<Float>,
            progressZipNotes: MutableState<Float>,
            progressExportNoteCategories: MutableState<Float>,
            progressCopyZip: MutableState<Float>,
            detailsDescription: MutableState<String>
        ): Result {
            val tmpDir = Files.createTempDirectory("companion")
            val tmpNotesFile = File(tmpDir.toString(), NOTEFILE_NAME)
            val tmpNoteCategoriesFile = File(tmpDir.toString(), NOTECATEGORYFILE_NAME)

            val tmpZipFile = File.createTempFile("companion", ".$ZIP_EXTENSION", cacheDir)
            try {
                val result = doExport(
                    noteViewModel, noteCategoryViewModel, tmpNotesFile, tmpNoteCategoriesFile,
                    onProgressNotes = { progress, item ->
                        progressExportNotes.value = progress
                        detailsDescription.value = item?.name?.let { "Processing note '${it}'" } ?: "Processing notes"
                    },
                    onProgressNoteCategories = { progress, item ->
                        progressExportNoteCategories.value = progress
                        detailsDescription.value = item?.name?.let { "Processing category '${it}'" } ?: "Processing categories"
                    }
                ).pipe {
                    doZip(
                        files = listOf(tmpNotesFile, tmpNoteCategoriesFile),
                        destination = tmpZipFile.toPath(),
                        password = password.toCharArray(),
                    ) { progress ->
                        progressZipNotes.value = progress
                        detailsDescription.value = "Creating archive..."
                    }
                }.pipe {
                    doCopy(
                        input = tmpZipFile,
                        outputStream = contentResolver.openOutputStream(location, "wt")!!
                    ) { progress ->
                        progressCopyZip.value = progress
                        detailsDescription.value = "Moving zip"
                    }
                }

                detailsDescription.value = if (result.type == ResultType.SUCCESS) "Done" else "Failure"
                Timber.e("All jobs completed - ${if(result.type == ResultType.SUCCESS) "success" else "failed"}: ${result.message}")
                return result
            } finally {
                Timber.e("Cleaning up")
                tmpNotesFile.delete()
                tmpNoteCategoriesFile.delete()
                tmpZipFile.delete()
                FileUtil.deleteDirectory(tmpDir)
            }
        }

        private suspend fun doExport(
            noteViewModel: NoteViewModel,
            noteCategoryViewModel: NoteCategoryViewModel,
            noteOutputFile: File,
            noteCategoryOutputFile: File,
            onProgressNotes: (Float, Note?) -> Unit,
            onProgressNoteCategories: (Float, NoteCategory?) -> Unit
        ): Result {
            val exportNotesJob = doExportParquet(
                data = noteViewModel.getAll(),
                outputFile = noteOutputFile,
                onProgress = onProgressNotes
            ) ?: return Result(ResultType.FAILED, "No notes to process")

            val exportNoteCategoriesJob = doExportParquet(
                data = noteCategoryViewModel.getAll(),
                outputFile = noteCategoryOutputFile,
                onProgress = onProgressNoteCategories
            ) ?: return Result(ResultType.FAILED, "No categories to process")

            Timber.e("starting export jobs")
            exportNotesJob.start()
            exportNoteCategoriesJob.start()
            Timber.e("joining export jobs")
            exportNotesJob.join()
            exportNoteCategoriesJob.join()
            return Result.DEFAULT_SUCCESS
        }

        private suspend fun doZip(files: List<File>, destination: Path, password: CharArray, onProgress: (Float) -> Unit): Result {
            Timber.e("launching zip job")
            destination.deleteExisting() // Otherwise zip library thinks our empty tmp file is a zip and crashes.
            val zippingJob = doZip(
                inputs = files,
                password = password,
                destination = destination,
                onProgress = onProgress
            )
            zippingJob.start()
            Timber.e("joining zip job")
            zippingJob.join()
            val zippingState = zippingJob.await()
            if (zippingState.state != EximUtil.FinishState.SUCCESS)
                return Result(ResultType.FAILED, zippingState.error)
            if (!EximUtil.verifyZip(destination))
                return Result(ResultType.FAILED, "Internal error: Created an incorrect zip.")
            return Result.DEFAULT_SUCCESS
        }

        private suspend fun doCopy(input: File, outputStream: OutputStream, onProgress: (Float) -> Unit): Result {
            Timber.e("launching copy job")
            val copyJob = FileUtil.copyStream(
                size = input.length(),
                inStream = input.inputStream(),
                outStream = outputStream,
                onProgress = onProgress
            )
            copyJob.start()
            Timber.e("joining copy job")
            copyJob.join()
            return Result.DEFAULT_SUCCESS
        }

        /**
         * Handles Parquet exporting.
         * @param data Data to export.
         * @param outputFile output file location.
         * @param onProgress progress lambda.
         * Progress ranges from 0f to 1f. Secondary parameter is the most recently processed item.
         * @return executable job.
         */
        private suspend fun <T: Exportable> doExportParquet(
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
            inputs: List<File>,
            password: CharArray,
            destination: Path,
            onProgress: (Float) -> Unit
        ): Deferred<EximUtil.ZippingState> {
            return Export.zip(inputs = inputs, password = password, pollTimeMS = 100, destination = destination, onProgress = onProgress)
        }

        @Composable
        fun rememberState(
            navController: NavHostController = rememberNavController(),
            noteViewModel: NoteViewModel,
            noteCategoryViewModel: NoteCategoryViewModel,
            scaffoldState: ScaffoldState
        ) = remember(navController) { ExportState(navController, noteViewModel, noteCategoryViewModel, scaffoldState) }
    }
}
