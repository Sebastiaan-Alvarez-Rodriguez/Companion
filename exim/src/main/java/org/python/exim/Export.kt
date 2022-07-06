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


data class ExportInfo(val value: Any?, val name: String)

interface Exportable {
    fun values(): Array<ExportInfo>
}

// List of supported export functionality
sealed class Exports {
    data class parquet(val schema: MessageType) : Exports() {
        companion object {
            /** Helper function to transform from primitive type to parquet schema type */
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
                Timber.e(it.toString())
                valueWriter!!.write(it.name, it.value)
            }
        }

        return withContext(Dispatchers.IO) {
            return@withContext launch {
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
                    parquetWriter.close()
                } catch (e: Exception) {
                    parquetWriter.close()
                    Timber.e(e)
                    throw e
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
        destination: String,
        pollTimeMS: Long,
        onProgress: (Float) -> Unit
    ): Deferred<ZippingState> = withContext(Dispatchers.IO) {
        async {
            val progressMonitor = zip(file, password, destination)
            while (progressMonitor.result == ProgressMonitor.Result.WORK_IN_PROGRESS || progressMonitor.result == null) {
                delay(pollTimeMS)
                onProgress(progressMonitor.workCompleted.toFloat() / progressMonitor.totalWork)
                Timber.e("Export zip tick: ${progressMonitor.workCompleted.toFloat() / progressMonitor.totalWork}")
            }
            Timber.e("Export zip tick completed: ${progressMonitor.workCompleted.toFloat() / progressMonitor.totalWork}")

            return@async when (progressMonitor.result) {
                ProgressMonitor.Result.SUCCESS -> ZippingState(FinishState.SUCCESS)
                ProgressMonitor.Result.ERROR -> throw progressMonitor.exception //ZippingState(FinishState.ERROR, progressMonitor.exception.message)
                ProgressMonitor.Result.CANCELLED -> ZippingState(FinishState.CANCELLED, "Zipping was cancelled")
                else -> throw IllegalStateException("Unexpected zipping progress '${progressMonitor.result}'")
            }
        }
    }

    private fun zip(file: File, password: CharArray, destination: String): ProgressMonitor {
        val zipParameters = ZipParameters()
        zipParameters.isEncryptFiles = true
        zipParameters.encryptionMethod = EncryptionMethod.AES
        zipParameters.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
//        zipParameters.fileNameInZip = file.name TODO: Needed?

        val zipFile = ZipFile(destination.toString(), password)
        try {
            zipFile.isRunInThread = true
            val progressMonitor = zipFile.progressMonitor

            zipFile.addFile(file, zipParameters)
            zipFile.close()
            return progressMonitor
        } catch (e: Exception) {
            zipFile.close()
            Timber.e("Got error during zipping process: ", e)
            throw e
        }
    }
}