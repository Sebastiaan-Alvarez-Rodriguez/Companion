package org.python.exim

import blue.strategic.parquet.Dehydrator
import blue.strategic.parquet.ParquetWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.parquet.schema.MessageType
import timber.log.Timber
import java.io.DataOutputStream
import java.io.File

data class ExportInfo(
    val value: Any?,
    val name: String
)
interface Exportable {
    fun values(): Array<ExportInfo>
    suspend fun export(out: DataOutputStream)
}

// List of supported export functionality
sealed class Exports {
    data class parquet(val schema: MessageType) : Exports()
}

object Export {
    suspend fun <T: Exportable> export(type: Exports, destination: File, content: List<T>) {
        when (type) {
            is Exports.parquet -> writeToParquet(type.schema, destination, content)
        }
    }

    private suspend fun <T: Exportable> writeToParquet(schema: MessageType, file: File, content: List<T>) {
        val dehydrator = Dehydrator<T> { record, valueWriter ->
            record.values().forEach {
                valueWriter!!.write(it.name, it.value)
            }
        }

        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val parquetWriter = ParquetWriter.writeFile(
                    schema,
                    file,
                    dehydrator
                )

                try {
                    content.forEach { parquetWriter.write(it) }
                } catch (e: Exception) {
                    parquetWriter.close()
                    Timber.e(e)
                }
            }
        }
    }
}