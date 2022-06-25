package org.python.exim

import blue.strategic.parquet.Dehydrator
import blue.strategic.parquet.ParquetWriter
import kotlinx.coroutines.*
import org.apache.parquet.schema.*
import timber.log.Timber
import java.io.File

data class ExportInfo(
    val value: Any?,
    val name: String
)
interface Exportable {
    fun values(): Array<ExportInfo>
}

interface Progress {
    fun onProgress(amount: Long)
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
                    is ByteArray -> Types.required(PrimitiveType.PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY)
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
                valueWriter!!.write(it.name, it.value)
            }
        }

        return withContext(Dispatchers.IO) {
            this.launch(start = CoroutineStart.LAZY) {
                kotlin.runCatching {
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
    }
}