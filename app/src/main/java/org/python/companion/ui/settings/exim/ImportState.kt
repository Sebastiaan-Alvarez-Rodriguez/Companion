package org.python.companion.ui.settings.exim

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
import org.python.backend.data.datatype.Note
import org.python.backend.data.datatype.NoteCategory
import org.python.companion.R
import org.python.companion.support.FileUtil
import org.python.companion.support.PermissionUtil
import org.python.companion.ui.security.SecurityState
import org.python.companion.ui.settings.SettingsState
import org.python.companion.ui.settings.exim.Shared.NOTECATEGORYFILE_NAME
import org.python.companion.ui.settings.exim.Shared.NOTEFILE_NAME
import org.python.companion.viewmodels.NoteCategoryViewModel
import org.python.companion.viewmodels.NoteViewModel
import org.python.datacomm.Result
import org.python.datacomm.ResultType
import org.python.exim.EximUtil
import org.python.exim.Import
import org.python.exim.Importable
import org.python.exim.Imports
import timber.log.Timber
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.isRegularFile

class ImportState(
    private val navController: NavHostController,
    private val noteViewModel: NoteViewModel,
    private val noteCategoryViewModel: NoteCategoryViewModel,
    private val scaffoldState: ScaffoldState
) {
    fun NavGraphBuilder.importGraph() {
        navigation(startDestination = navigationStart, route = "import") {
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

        val requestLauncher = PermissionUtil.requestExternalStoragePermission(navController, onGranted = { fileLauncher.launch(arrayOf("application/zip")) })

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
        // metrics for importing
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
                    Shared.navigateToStop(navController, isExport = false) {
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
                    message = importResult!!.message ?: "Error during importing",
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

    companion object {
        const val navigationStart: String = "${SettingsState.navigationStart}/import"

        fun navigateToImport(navController: NavController) = navController.navigate(navigationStart)

        /**
         * Imports all data from 2 parquet files: one for notes, the other for note categories.
         * @return result containing error message on error, success otherwise
         */
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
                val result = doCopy(contentResolver, location, tmpZipFile.outputStream()) { progress ->
                    progressCopyZip.value = progress
                    detailsDescription.value = "Moving archive"
                }.pipe {
                    doUnzip(tmpZipFile, password.toCharArray(), tmpZipExtractDir) { progress ->
                        progressExtractZip.value = progress
                        detailsDescription.value = "Extracting archive..."
                    }
                }.pipe {
                    doImport(tmpZipExtractDir,
                        onProgressNotes = { progress, item ->
                            progressImportNotes.value = progress
                            detailsDescription.value = item?.name?.let { "Processing note '${it}'" } ?: "Processing notes"
                        },
                        onProgressCategories = { progress, item ->
                            progressImportNoteCategories.value = progress
                            detailsDescription.value = item?.name?.let { "Processing category '${it}'" } ?: "Processing categories"
                        }
                    )
                }

                detailsDescription.value = if (result.type == ResultType.SUCCESS) "Done" else "Failure"
                Timber.e("All jobs completed - ${if(result.type == ResultType.SUCCESS) "success" else "failed"}: ${result.message}")
                return result
            } finally {
                Timber.e("Cleaning up")
                tmpZipFile.delete()
                FileUtil.deleteDirectory(tmpZipExtractDir)
            }
        }

        private suspend fun doCopy(contentResolver: ContentResolver, location: Uri, output: OutputStream, onProgress: (Float) -> Unit): Result {
            Timber.e("launching copy job")
            val zipSize = FileUtil.determineSize(contentResolver.openAssetFile(location, "r", null)!!)
            val copyJob = FileUtil.copyStream(
                size = zipSize,
                inStream = contentResolver.openInputStream(location)!!,
                outStream = output,
                onProgress = onProgress
            )

            copyJob.start()
            Timber.e("joining copy job")
            copyJob.join()
            return Result.DEFAULT_SUCCESS
        }

        private suspend fun doUnzip(
            zipFile: File,
            password: CharArray,
            outputLocation: Path,
            onProgress: (Float) -> Unit
        ): Result {
            // Check if picked file is a correct zip
            if (!EximUtil.verifyZip(zipFile))
                return Result(ResultType.FAILED, "Object is not a valid zip file")

            Timber.e("launching unzip job")
            val zippingJob = doUnzip(input = zipFile, password = password,
                destination = outputLocation.toString(),//tmpZipExtractDir.toString()
                onProgress = onProgress
            )

            zippingJob.start()
            Timber.e("joining unzip job")
            zippingJob.join()
            val zippingState = zippingJob.await()
            if (zippingState.state != EximUtil.FinishState.SUCCESS)
                return Result(ResultType.FAILED, zippingState.error)
            return Result.DEFAULT_SUCCESS
        }

        private suspend fun doImport(
            outputLocation: Path,
            onProgressNotes: (Float, Note?) -> Unit,
            onProgressCategories: (Float, NoteCategory?) -> Unit
        ): Result {
            val notePath = outputLocation / NOTEFILE_NAME
            if (!notePath.isRegularFile())
                return Result(ResultType.FAILED, "Could not find extracted notes file")
            val categoriesPath = outputLocation / NOTECATEGORYFILE_NAME
            if (!categoriesPath.isRegularFile())
                return Result(ResultType.FAILED, "Could not find extracted note categories file")

            Timber.e("launching import jobs")
            val importNotesJob = doImport( // TODO: Do something with data, and apply mergeStrategy
                input = notePath.toFile(),
                batchSize = 100,
                cls = Note::class.java,
                onProgress = onProgressNotes
            )
            val importCategoriesJob = doImport( // TODO: Do something with data, and apply mergeStrategy
                input = categoriesPath.toFile(),
                batchSize = 100,
                cls = NoteCategory::class.java,
                onProgress = onProgressCategories
            )

            importNotesJob.start()
            importCategoriesJob.start()
            Timber.e("joining import jobs")
            importNotesJob.join()
            importCategoriesJob.join()
            return Result.DEFAULT_SUCCESS
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

        private suspend fun doUnzip(input: File, password: CharArray, destination: String, onProgress: (Float) -> Unit): Deferred<EximUtil.ZippingState> =
            Import.unzip(input = input, password = password, destination = destination, pollTimeMS = 100, onProgress = onProgress)

        @Composable
        fun rememberState(
            navController: NavHostController = rememberNavController(),
            noteViewModel: NoteViewModel,
            noteCategoryViewModel: NoteCategoryViewModel,
            scaffoldState: ScaffoldState
        ) = remember(navController) { ImportState(navController, noteViewModel, noteCategoryViewModel, scaffoldState) }
    }
}
