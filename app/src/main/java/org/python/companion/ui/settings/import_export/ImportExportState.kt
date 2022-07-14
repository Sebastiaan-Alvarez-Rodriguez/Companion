package org.python.companion.ui.settings.import_export

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
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
import org.python.companion.support.UiUtil
import org.python.companion.ui.note.NoteState
import org.python.companion.ui.security.SecurityState
import org.python.companion.ui.theme.DarkColorPalette
import org.python.companion.viewmodels.NoteCategoryViewModel
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
    private val noteCategoryViewModel: NoteCategoryViewModel,
    private val scaffoldState: ScaffoldState
) {
    fun NavGraphBuilder.importExportGraph() {
        navigation(startDestination = navigationStart, route = "exim") {
            composable(route = navigationStart) {
                val hasSecureNotes by noteViewModel.hasSecureNotes.collectAsState()
                val isAuthorized by noteViewModel.securityActor.clearance.collectAsState()

                if (hasSecureNotes && isAuthorized <= 0) {
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

        val requestLauncher = requestExternalStoragePermission(onGranted = { fileLauncher.launch(zipName()) })

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
                    navigateToStop(navController, isExport = true) {
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

        val requestLauncher = requestExternalStoragePermission(onGranted = { fileLauncher.launch(arrayOf("application/zip")) })

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
                    else {
                        requestLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
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
        // TODO: Export categories as well
        // metrics for exporting
        val progressCopyZip = remember { mutableStateOf(0f) }
        val progressExtractZip = remember { mutableStateOf(0f) }
        val progressImportNotes = remember { mutableStateOf(0f) }
        val progressImportNoteCategories = remember { mutableStateOf(0f) }

        val detailsDescription = remember { mutableStateOf("") }

        var importResult by remember { mutableStateOf<Result?>(null) }

        val context = LocalContext.current
        val cacheDir = context.cacheDir

        val contentResolver = LocalContext.current.contentResolver

        ImportExportScreenSettings(
            progressContent = {
                FinishingNestedCircularProgressIndicator(
                    result = importResult,
                    progresses = listOf(progressCopyZip.value, progressExtractZip.value, progressImportNotes.value)
                )
            },
            subContent = {
                DetailsCard(detailsDescription = detailsDescription.value)
            },
            onBackClick = {
                if (importResult == null) {
                    navigateToStop(navController, isExport = true) {
                        cacheDir.walkBottomUp().onLeave { it.delete() }
                        navController.navigateUp()
                    }
                } else {
                    cacheDir.walkBottomUp().onLeave { it.delete() }
                    navController.navigateUp()
                }
            }
        )

        var importTrigger by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(importTrigger) {
            importResult = import(
                noteViewModel, noteCategoryViewModel, cacheDir, contentResolver, location, password, mergeStrategy,
                progressCopyZip, progressExtractZip, progressImportNotes, progressImportNoteCategories,
                detailsDescription
            )
            if (importResult!!.type == ResultType.FAILED) {
                val snackbarResult = scaffoldState.snackbarHostState.showSnackbar(
                    message = importResult!!.message ?: "Error during exporting",
                    duration = SnackbarDuration.Indefinite,
                    actionLabel = "Retry"
                )
                when (snackbarResult) {
                    SnackbarResult.ActionPerformed -> importTrigger = !importTrigger // trigger retry
                    else -> {}
                }
            }
        }
    }

    @Composable
    private fun FinishingNestedCircularProgressIndicator(result: Result?, progresses: List<Float>) {
        NestedCircularProgressIndicator(progresses = progresses) {
            result?.let {
                if (it.type == ResultType.SUCCESS) {
                    Icon(
                        imageVector = Icons.Rounded.Done,
                        contentDescription = "Success",
                        modifier = Modifier.size(32.dp).align(Alignment.Center), tint = DarkColorPalette.primary
                    )
                } else if (it.type == ResultType.FAILED) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Failure",
                        modifier = Modifier.size(32.dp).align(Alignment.Center), tint = Color.Red
                    )
                }
            }
        }
    }
    @Composable
    private inline fun requestExternalStoragePermission(crossinline onGranted: () -> Unit): ManagedActivityResultLauncher<String, Boolean> {
        return rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                onGranted()
            } else {
                // TODO: Only ask for permission as per https://developer.android.com/training/permissions/requesting
                UiUtil.UIUtilState.navigateToSingular(
                    navController = navController,
                    title = "Permission needed, really",
                    message = "We need file access permission to import data from a file. We solely access files for importing/exporting.",
                    onClick = {}
                )
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
                val exportNotesJob = doExport(data = noteViewModel.getAll(), outputFile = tmpNotesFile) { progress, item ->
                    progressExportNotes.value = progress
                    detailsDescription.value = item?.name?.let { "Processing note '${it}'" } ?: "Processing notes"
                } ?: throw IllegalStateException("No notes to process")

                val exportNoteCategoriesJob = doExport(data = noteCategoryViewModel.getAll(), outputFile = tmpNoteCategoriesFile) { progress, item ->
                    progressExportNoteCategories.value = progress
                    detailsDescription.value = item?.name?.let { "Processing category '${it}'" } ?: "Processing categories"
                } ?: throw IllegalStateException("No categories to process")

                Timber.e("starting export jobs")
                exportNotesJob.start()
                exportNoteCategoriesJob.start()
                Timber.e("joining export jobs")
                exportNotesJob.join()
                exportNoteCategoriesJob.join()

                Timber.e("launching zip job")
                val zipFilePath = tmpZipFile.path
                tmpZipFile.delete() // Otherwise zip library thinks our empty tmp file is a zip and crashes.
                val zippingJob = doZip(
                    inputs = listOf(tmpNotesFile, tmpNoteCategoriesFile),
                    password = password.toCharArray(),
                    destination = zipFilePath
                ) { progress ->
                    progressZipNotes.value = progress
                    detailsDescription.value = "Archiving notes..."
                }
                zippingJob.start()
                Timber.e("joining zip job")
                zippingJob.join()
                val zippingState = zippingJob.await()
                if (zippingState.state != EximUtil.FinishState.SUCCESS)
                    return Result(ResultType.FAILED, zippingState.error)
                if (!EximUtil.verifyZip(zipFilePath))
                    return Result(ResultType.FAILED, "Internal error: Created an incorrect zip.")

                // TODO: Remove old content before launching job (solves 11KB zip bug)
                Timber.e("launching copy job")
                val copyJob = FileUtil.copyStream(
                    size = tmpZipFile.length(),
                    inStream = tmpZipFile.inputStream(),
                    outStream = contentResolver.openOutputStream(location, "w")!!
                ) { progress ->
                    progressCopyZip.value = progress
                    detailsDescription.value = "Moving zip"
                }
                copyJob.start()
                Timber.e("joining copy job")
                copyJob.join()
                detailsDescription.value = "Done"
                Timber.e("All jobs completed - success")

                return Result.DEFAULT_SUCCESS
            } finally {
                Timber.e("Cleaning up")
                tmpNotesFile.delete()
                tmpNoteCategoriesFile.delete()
                tmpZipFile.delete()
                tmpDir.toFile().deleteRecursively()
            }
        }

        private suspend fun import(
            noteViewModel: NoteViewModel,
            noteCategoryViewModel: NoteCategoryViewModel,
            cacheDir: File, contentResolver: ContentResolver,
            location: Uri, password: String, mergeStrategy: EximUtil.MergeStrategy,
            progressCopyZip: MutableState<Float>,
            progressExtractZip: MutableState<Float>,
            progressImportNotes: MutableState<Float>,
            progressImportNoteCategories: MutableState<Float>,
            detailsDescription: MutableState<String>
        ): Result {
            val tmpZipFile = File.createTempFile("companion", ".zip", cacheDir)
            val tmpZipExtractDir = Files.createTempDirectory("companion")

            try {
                Timber.e("launching copy job")
                val zipSize = FileUtil.determineSize(contentResolver.openAssetFile(location, "r", null)!!)
                val copyJob = FileUtil.copyStream(
                    size = zipSize,
                    inStream = contentResolver.openInputStream(location)!!,
                    outStream = tmpZipFile.outputStream()
                ) { progress ->
                    progressCopyZip.value = progress
                    detailsDescription.value = "Moving archive"
                }
                copyJob.start()
                Timber.e("joining copy job")
                copyJob.join()

                // Check if picked file is a correct zip
                if (!EximUtil.verifyZip(tmpZipFile))
                    return Result(ResultType.FAILED, "Message is not a valid zip file")

                Timber.e("launching unzip job")
                val zippingJob = doUnzip(input = tmpZipFile, password = password.toCharArray(), destination = tmpZipExtractDir.toString()) { progress ->
                    progressExtractZip.value = progress
                    detailsDescription.value = "Extracting archive..."
                }
                zippingJob.start()
                Timber.e("joining unzip job")
                zippingJob.join()
                val zippingState = zippingJob.await()
                if (zippingState.state != EximUtil.FinishState.SUCCESS)
                    return Result(ResultType.FAILED, zippingState.error)

                val extractedNotesFile = tmpZipExtractDir.resolve(NOTEFILE_NAME)
                if (!extractedNotesFile.isRegularFile())
                    throw FileNotFoundException("Could not find extracted notes file")
                val extractedNoteCategoriesFile = tmpZipExtractDir.resolve(NOTECATEGORYFILE_NAME)
                if (!extractedNoteCategoriesFile.isRegularFile())
                    throw FileNotFoundException("Could not find extracted note categories file")

                Timber.e("launching import jobs")
                val importNotesJob = doImport( // TODO: Do something with data, and apply mergeStrategy
                    input = extractedNotesFile.toFile(),
                    batchSize = 100,
                    cls = Note::class.java
                ) { progress, item ->
                    progressImportNotes.value = progress
                    detailsDescription.value = item?.name?.let { "Processing note '${it}'" } ?: "Processing notes"
                }
                val importCategoriesJob = doImport( // TODO: Do something with data, and apply mergeStrategy
                    input = extractedNoteCategoriesFile.toFile(),
                    batchSize = 100,
                    cls = NoteCategory::class.java
                ) { progress, item ->
                    progressImportNoteCategories.value = progress
                    detailsDescription.value = item?.name?.let { "Processing category '${it}'" } ?: "Processing categories"
                }

                importNotesJob.start()
                importCategoriesJob.start()
                Timber.e("joining import jobs")
                importNotesJob.join()
                importCategoriesJob.join()

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

        private suspend fun doZip(inputs: List<File>, password: CharArray, destination: String, onProgress: (Float) -> Unit): Deferred<EximUtil.ZippingState> {
            //TODO: When also writing categories: write lock?
            return Export.zip(inputs = inputs, password = password, pollTimeMS = 100, destination = destination, onProgress = onProgress)
        }

        private suspend fun doUnzip(input: File, password: CharArray, destination: String, onProgress: (Float) -> Unit): Deferred<EximUtil.ZippingState> =
            Import.unzip(input = input, password = password, destination = destination, pollTimeMS = 100, onProgress = onProgress)

        private fun navigateToStop(navController: NavHostController, isExport: Boolean = true, onStopClick: () -> Unit) =
            UiUtil.UIUtilState.navigateToBinary(
                navController = navController,
                title = "${if (isExport) "Exporting" else "Importing"} unfinished",
                message = "Are you sure you want to go back? ${if (isExport) "Export" else "Import"} process will be cancelled.",
                positiveText = "Stop ${if (isExport) "exporting" else "importing"}",
                onOptionClick = { if (it) onStopClick() }
            )

        @Composable
        fun rememberState(
            navController: NavHostController = rememberNavController(),
            noteViewModel: NoteViewModel,
            noteCategoryViewModel: NoteCategoryViewModel,
            scaffoldState: ScaffoldState) =
            remember(navController) { ImportExportState(navController, noteViewModel, noteCategoryViewModel, scaffoldState) }
    }
}
