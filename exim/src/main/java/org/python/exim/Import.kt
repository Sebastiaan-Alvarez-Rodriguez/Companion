package org.python.exim

import org.apache.parquet.schema.MessageType
import java.io.File
import java.util.*

interface Importable {
    fun <T> fromValues(values: Array<Any?>): T
}

// List of supported import functionality
sealed class Imports {
    data class parquet(val schema: MessageType) : Imports()
}

object Import {
    suspend fun <T: Importable> import(type: Imports, source: File): List<T> {
        return when (type) {
            is Imports.parquet -> readFromParquet(type.schema, source)
        }
    }

    private suspend fun <T: Importable> readFromParquet(schema: MessageType, file: File): List<T> {
//        val dehydrator = Hydrator<T>
        return Collections.emptyList()
    }
}