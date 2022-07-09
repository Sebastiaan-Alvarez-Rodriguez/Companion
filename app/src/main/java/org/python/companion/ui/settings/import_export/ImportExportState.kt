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
import org.python.backend.data.datatype.Note
import org.python.companion.R
import org.python.companion.support.FileUtil
import org.python.companion.support.UiUtil
import org.python.companion.ui.note.NoteState
import org.python.companion.ui.security.SecurityState
import org.python.companion.viewmodels.NoteViewModel
import org.python.datacomm.Result
import org.python.datacomm.ResultType
import org.python.exim.*
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.time.Instant
import kotlin.io.path.isRegularFile

class ImportExportState(
    private val navController: NavHostController,
    private val noteViewModel: NoteViewModel,
    private val scaffoldState: ScaffoldState
) {
    fun NavGraphBuilder.importExportGraph() {
        navigation(startDestination = navigationStart, route = "exim") {
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

            composable(route = "$navigationStart/import") {
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

                val isImporting = rememberSaveable { mutableStateOf(false) }

                // settings for importing
                val location = rememberSaveable { mutableStateOf<Uri?>(null) }
                val password = rememberSaveable { mutableStateOf("") }
                val mergeStrategy = rememberSaveable { mutableStateOf(EximUtil.MergeStrategy.DELETE_ALL_BEFORE) }

                if (!isImporting.value) {
                    ImportSettingsScreen(location, password, mergeStrategy, isImporting)
                } else {
                    ImportExecuteScreen(location.value!!, password.value, mergeStrategy.value)
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
                fileLauncher.launch(zipName())
            } else {
                // TODO Snackbar: 'need permission'
            }
        }
        ImportExportScreenSettings(
            progressContent = { NestedCircularProgressIndicator(progresses = listOf(1f, 1f, 1f)) },
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
                    if (password.value.length < 1) { //todo make 12 again
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
                noteViewModel, cacheDir, contentResolver, location, password,
                progressExportNotes, progressZipNotes, progressCopyZip,
                detailsDescription
            )
            if (exportResult.type == ResultType.FAILED) {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = exportResult.message ?: "Error during exporting"
                )
                // TODO: Do something here?
            } else {
                // TODO: Indicate up, maybe go back up
            }
        }
    }

    @Composable
    private fun ImportSettingsScreen(
        location: MutableState<Uri?>,
        password: MutableState<String>,
        mergeStrategy: MutableState<EximUtil.MergeStrategy>,
        isImporting: MutableState<Boolean>
    ) {
        // error indicators
        var pathError by rememberSaveable { mutableStateOf<String?>(null) }
        var passwordError by rememberSaveable { mutableStateOf<String?>(null) }

        val defaultPadding = dimensionResource(id = R.dimen.padding_default)

        val context = LocalContext.current

        // launcher to make user select an existing file
        val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            location.value = it
        }

        val requestLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                fileLauncher.launch(arrayOf("application/zip"))
            } else {
                // TODO Snackbar: 'need permission'
            }
        }
        ImportExportScreenSettings(
            progressContent = { NestedCircularProgressIndicator(progresses = listOf(1f, 1f, 1f)) },
            subContent = {
                PickFileCard(
                    path = location.value?.path,
                    explanationText = "Pick file to import",
                    pathError = pathError
                ) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                        fileLauncher.launch(arrayOf("application/zip"))
                    else
                        requestLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    pathError = null
                }
                Spacer(Modifier.height(defaultPadding))
                ImportPasswordCard(password.value, passwordError) {
                    password.value = it
                    passwordError = null
                }
                Spacer(Modifier.height(defaultPadding))
                ImportMergeStrategyCard(
                    mergeStrategy = mergeStrategy.value,
                    onMergeStrategyChange = { mergeStrategy.value = it }
                )
                Spacer(Modifier.height(defaultPadding))
                Button(modifier = Modifier.fillMaxWidth(), onClick = {
                    var hasErrors = false
                    if (location.value == null) {
                        pathError = "Path has not been set"
                        hasErrors = true
                    }
                    if (password.value.isEmpty()) {
                        passwordError = "Password has not been set."
                        hasErrors = true
                    }
                    if (!hasErrors) {
                        isImporting.value = true
                    }
                }) {
                    Text("Begin import", modifier = Modifier.padding(defaultPadding))
                }
            },
            onBackClick = { navController.navigateUp() }
        )
    }

    @Composable
    private fun ImportExecuteScreen(location: Uri, password: String, mergeStrategy: EximUtil.MergeStrategy) {
        // metrics for exporting
        val progressCopyZip = remember { mutableStateOf(0f) }
        val progressExtractZip = remember { mutableStateOf(0f) }
        val progressImportNotes = remember { mutableStateOf(0f) }

        val detailsDescription = remember { mutableStateOf("") }

        ImportExportScreenSettings(
            progressContent = {
                NestedCircularProgressIndicator(progresses = listOf(progressCopyZip.value, progressExtractZip.value, progressImportNotes.value))
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
            val importResult = import(
                noteViewModel, cacheDir, contentResolver, location, password, mergeStrategy,
                progressCopyZip, progressExtractZip, progressImportNotes,
                detailsDescription
            )
            if (importResult.type == ResultType.FAILED) {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = importResult.message ?: "Error during exporting"
                )
                // TODO: Do something here?
            } else {
                // TODO: Indicate up, maybe go back up
            }
        }
    }


    companion object {
        const val navigationStart: String = "${NoteState.noteDestination}/exim"

        private const val PARQUET_EXTENSION = "pq"
        private const val ZIP_EXTENSION = "zip"
        private const val NOTEFILE_NAME = "notes.pq"
        private const val NOTECATEGORYFILE_NAME = "notecategories.$PARQUET_EXTENSION"

        fun navigateToExport(navController: NavController) =
            navController.navigate(navigationStart)

        fun navigateToImport(navController: NavController) =
            navController.navigate("$navigationStart/import")

        private fun zipName() = "companion-${Instant.now()}"

        /**
         * Exports all data to a parquet file.
         * @return result containing error message on error, success otherwise
         */
        private suspend fun export(
            noteViewModel: NoteViewModel,
            cacheDir: File, contentResolver: ContentResolver,
            location: Uri, password: String,
            progressExportNotes: MutableState<Float>,
            progressZipNotes: MutableState<Float>,
            progressCopyZip: MutableState<Float>,
            detailsDescription: MutableState<String>
        ): Result {
            val tmpNotesFile = File.createTempFile("notes", ".$PARQUET_EXTENSION", cacheDir)
            val tmpZipFile = File.createTempFile("companion", ".$ZIP_EXTENSION", cacheDir)

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
                val zippingJob = doZip(input = tmpNotesFile, inZipName = NOTEFILE_NAME, password = password.toCharArray(), destination = zipFilePath) { progress ->
                    progressZipNotes.value = progress
                    detailsDescription.value = "Archiving notes..."
                }
                val zippingState = zippingJob.await()
                if (zippingState.state != EximUtil.FinishState.SUCCESS)
                    return Result(ResultType.FAILED, zippingState.error)

                val copyJob = FileUtil.copyStream(
                    size = tmpZipFile.length(),
                    inStream = tmpZipFile.inputStream(),
                    outStream = contentResolver.openOutputStream(location, "w")!!
                ) { progress ->
                    progressCopyZip.value = progress
                    detailsDescription.value = "Moving archive"
                }
                Timber.e("launching move job")
                copyJob.start()
                copyJob.join()
                detailsDescription.value = "Done"
                Timber.e("All jobs completed - success")
                return Result.DEFAULT_SUCCESS
            } finally {
                Timber.e("Cleaning up")
                tmpNotesFile.delete()
                tmpZipFile.delete()
            }
        }

        private suspend fun import(
            noteViewModel: NoteViewModel,
            cacheDir: File, contentResolver: ContentResolver,
            location: Uri, password: String, mergeStrategy: EximUtil.MergeStrategy,
            progressCopyZip: MutableState<Float>,
            progressExtractZip: MutableState<Float>,
            progressImportNotes: MutableState<Float>,
            detailsDescription: MutableState<String>
        ): Result {
            val tmpZipFile = File.createTempFile("companion", ".zip", cacheDir)
            val tmpZipExtractDir = Files.createTempDirectory("companion")

            try {
                // TODO: Check if picked file is a zip
                val copyJob = FileUtil.copyStream(
                    size = tmpZipFile.length(),
                    inStream = contentResolver.openInputStream(location)!!,
                    outStream = tmpZipFile.outputStream()
                ) { progress ->
                    progressCopyZip.value = progress
                    detailsDescription.value = "Moving archive"
                }
                Timber.e("launching copy job")
                copyJob.start()
                copyJob.join()

                Timber.e("launching zip job")
                val zippingJob = doUnzip(input = tmpZipFile, password = password.toCharArray(), destination = tmpZipExtractDir.toString()) { progress ->
                    progressExtractZip.value = progress
                    detailsDescription.value = "Extracting archive..."
                }
                val zippingState = zippingJob.await()
                if (zippingState.state != EximUtil.FinishState.SUCCESS)
                    return Result(ResultType.FAILED, zippingState.error)

                val extractedNotesFile = tmpZipExtractDir.resolve(NOTEFILE_NAME)
                if (!extractedNotesFile.isRegularFile())
                    throw FileNotFoundException("Could not find extracted notes file")

                Timber.e("launching import job")
                val importJob = doImport( // TODO: Do something with data, and apply mergeStrategy
                    input = extractedNotesFile.toFile(),
                    batchSize = 100,
                    cls = Note::class.java
                ) { progress, item ->
                    progressImportNotes.value = progress
                    detailsDescription.value = item?.name?.let { "Processing note '${it}'" } ?: "Processing notes"
                }

                importJob.start()
                Timber.e("joining import job")
                importJob.join()

                detailsDescription.value = "Done"
                Timber.e("All jobs completed - success")
                return Result.DEFAULT_SUCCESS
            } finally {
                Timber.e("Cleaning up")
                tmpZipFile.delete()
                FileUtil.deleteDirectory(tmpZipExtractDir)
            }
        }

        /**
         * Handles Parquet exporting.
         * @param data Data to export.
         * @param outputFile output file location.
         * @param onProgress progress lambda.
         * Progress ranges from 0f to 1f. Secondary parameter is the most recently processed item.
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

        /**
         * Handles Parquet importing.
         * @param input input file location.
         * @param batchSize amount of items to process at once.
         * @param onProgress progress lambda.
         * Progress ranges from 0f to 1f. Secondary parameter is the most recently processed item.
         * @return executable job.
         */
        private suspend fun <T: Importable<T>> doImport(
            input: File,
            batchSize: Int,
            cls: Class<T>,
            onProgress: (Float, T?) -> Unit
        ): Job {
            val parquetImport = Imports.parquet

            return Import.import(
                type = parquetImport,
                source = input,
                batchSize = batchSize,
                cls = cls
            ) { item: T, amountProcessed: Long ->
                //TODO: get total rows, compute progress
                onProgress(0.6f, item)
            }
        }

        private suspend fun doZip(
            input: File,
            inZipName: String,
            password: CharArray,
            destination: String,
            onProgress: (Float) -> Unit
        ): Deferred<EximUtil.ZippingState> {
            //TODO: When also writing categories: write lock?
            return Export.zip(input = input, inZipName = inZipName, password = password, pollTimeMS = 100, destination = destination, onProgress = onProgress)
        }

        private suspend fun doUnzip(
            input: File,
            password: CharArray,
            destination: String,
            onProgress: (Float) -> Unit
        ): Deferred<EximUtil.ZippingState> = Import.unzip(input = input, password = password, destination = destination, pollTimeMS = 100, onProgress = onProgress)

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
