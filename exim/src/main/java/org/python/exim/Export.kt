package org.python.exim

import blue.strategic.parquet.CompressionCodecName
import blue.strategic.parquet.Dehydrator
import blue.strategic.parquet.ParquetWriter
import kotlinx.coroutines.*
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.progress.ProgressMonitor
import org.apache.hadoop.io.compress.CompressionCodec
import org.apache.parquet.schema.*
import org.python.exim.EximUtil.pollForZipFunc
import timber.log.Timber
import java.io.File


interface Exportable {
    fun values(): Array<EximUtil.FieldInfo>
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
                    dehydrator,
                    CompressionCodecName.UNCOMPRESSED
                )

                parquetWriter.use { writer ->
                    var count = 0L
                    content.forEach {
                        writer.write(it)
                        count += 1L
                        onProgress(it, count)
                    }
                }
            }
        }
    }

    suspend fun zip(
        input: File,
        inZipName: String,
        password: CharArray,
        destination: String,
        pollTimeMS: Long,
        onProgress: (Float) -> Unit
    ): Deferred<EximUtil.ZippingState> = withContext(Dispatchers.IO) {
        pollForZipFunc(func = { zip(input, inZipName, password, destination) }, pollTimeMS = pollTimeMS, onProgress = onProgress)
    }

    private fun zip(file: File, inZipName: String, password: CharArray, destination: String): ProgressMonitor {
        val zipParameters = ZipParameters()

        zipParameters.isEncryptFiles = true
        zipParameters.encryptionMethod = EncryptionMethod.AES
        zipParameters.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
        zipParameters.fileNameInZip = inZipName

        val zipFile = ZipFile(destination, password)
        val progressMonitor = zipFile.progressMonitor
        zipFile.isRunInThread = true
        zipFile.addFile(file, zipParameters)
        return progressMonitor
    }
}