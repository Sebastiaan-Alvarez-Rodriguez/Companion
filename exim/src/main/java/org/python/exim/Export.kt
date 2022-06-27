package org.python.exim

import blue.strategic.parquet.Dehydrator
import blue.strategic.parquet.ParquetWriter
import kotlinx.coroutines.*
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.progress.ProgressMonitor
import org.apache.parquet.schema.*
import timber.log.Timber
import java.io.File
import java.nio.file.Path
import java.time.Instant


data class ExportInfo(
    val value: Any?,
    val name: String
)
interface Exportable {
    fun values(): Array<ExportInfo>
}

// List of supported export functionality
sealed class Exports {
    data class parquet(val schema: MessageType) : Exports() {
        companion object {
            fun transform(primitiveType: Any?, name: String): Type =
                when(primitiveType) {
                    is Int -> Types.required(PrimitiveType.PrimitiveTypeName.INT32)
                    is Long -> Types.required(PrimitiveType.PrimitiveTypeName.INT64)
                    is Boolean -> Types.required(PrimitiveType.PrimitiveTypeName.BOOLEAN)
                    is Float -> Types.required(PrimitiveType.PrimitiveTypeName.FLOAT)
                    is Double -> Types.required(PrimitiveType.PrimitiveTypeName.DOUBLE)
                    is String -> Types.required(PrimitiveType.PrimitiveTypeName.BINARY).`as`(LogicalTypeAnnotation.stringType())
                    is ByteArray -> Types.required(PrimitiveType.PrimitiveTypeName.BINARY)
                    else -> throw IllegalArgumentException("No support for transforming object '$primitiveType' of type '${primitiveType?.javaClass?.name ?: "<null>"}'")
                }.named(name)
            }
    }
}

object Export {
    suspend fun <T: Exportable> export(type: Exports, destination: File, content: List<T>, onProgress: (T, Long) -> Unit): Job =
        when (type) {
            is Exports.parquet -> writeToParquet(type.schema, destination, content, onProgress)
            else -> throw IllegalArgumentException("Unknown export type '$type'")
        }

    private suspend fun <T: Exportable> writeToParquet(schema: MessageType, file: File, content: List<T>, onProgress: (T, Long) -> Unit): Job {
        val dehydrator = Dehydrator<T> { record, valueWriter ->
            record.values().forEach {
                valueWriter!!.write(it.name, it.value)
            }
        }
        Timber.e("Returning job...")

        return withContext(Dispatchers.IO) {
            return@withContext launch(start = CoroutineStart.LAZY) {
                val parquetWriter = ParquetWriter.writeFile(
                    schema,
                    file,
                    dehydrator
                )

                try {
                    var count = 0L
                    content.forEach {
                        parquetWriter.write(it)
                        count += 1L
                        onProgress(it, count)
                    }
                } catch (e: Exception) {
                    parquetWriter.close()
                    Timber.e(e)
                }
            }
        }
    }

    data class ZippingState(val state: FinishState, val error: String? = null)

    enum class FinishState {
        SUCCESS,
        ERROR,
        CANCELLED
    }
    suspend fun zip(
        file: File,
        password: CharArray,
        zipName: String? = null,
        path: Path,
        pollTimeMS: Long,
        onProgress: (Float) -> Unit
    ): Deferred<ZippingState> = withContext(Dispatchers.IO) {
        async {
            val progressMonitor = zip(file, password, path, zipName)
            while (progressMonitor.result == ProgressMonitor.Result.WORK_IN_PROGRESS) {
                delay(pollTimeMS)
                onProgress(progressMonitor.workCompleted.toFloat() / progressMonitor.totalWork)
            }

            return@async when (progressMonitor.result) {
                ProgressMonitor.Result.SUCCESS -> ZippingState(FinishState.SUCCESS)
                ProgressMonitor.Result.ERROR -> ZippingState(FinishState.ERROR, progressMonitor.exception.message)
                ProgressMonitor.Result.CANCELLED -> ZippingState(FinishState.CANCELLED, "Zipping was cancelled")
                else -> throw IllegalStateException("Unexpected zipping progress '${progressMonitor.result}'")
            }
        }
    }

    private fun zip(file: File, password: CharArray, path: Path, zipName: String? = null): ProgressMonitor {
        val zipParameters = ZipParameters()
        zipParameters.isEncryptFiles = true
        zipParameters.encryptionMethod = EncryptionMethod.AES
        zipParameters.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
//        zipParameters.fileNameInZip = file.name TODO: Needed?
        val finalName = zipName ?: "companion-${Instant.now()}"

        try {
            val zipFile = ZipFile(finalName, password)
            zipFile.isRunInThread = true
            zipFile.comment = "Companion-generated backup files"
            val progressMonitor = zipFile.progressMonitor

            zipFile.addFile(file, zipParameters)
            return progressMonitor
        } catch (e: Exception) {
            Timber.e("Got error during zipping process: ", e)
            throw e
        }
    }
}