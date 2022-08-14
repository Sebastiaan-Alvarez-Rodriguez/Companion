package org.python.exim

import blue.strategic.parquet.Hydrator
import blue.strategic.parquet.HydratorSupplier
import blue.strategic.parquet.ParquetReader
import kotlinx.coroutines.*
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.progress.ProgressMonitor
import org.python.exim.EximUtil.pollForZipFunc
import timber.log.Timber
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.streams.asSequence

/**
 * Interface for importable data.
 * @param T type of importable data.
 * Requires `T` to have an empty constructor.
 */
interface Importable<T> {
    val amountValues: Int
    fun fromValues(values: List<Any?>): T

    companion object {
        fun <T: Importable<T>> amountValues(cls: Class<T>) = amountValues(classToInstance(cls))
        fun <T: Importable<T>> amountValues(accessor: T) = accessor.amountValues

        fun <T: Importable<T>> fromValues(values: List<Any?>, cls: Class<T>): T = fromValues(values, classToInstance(cls))
        fun <T: Importable<T>> fromValues(values: List<Any?>, accessor: T): T = accessor.fromValues(values)

        fun <T: Importable<T>> classToInstance(cls: Class<T>) = cls.getDeclaredConstructor().newInstance()
    }
}

// List of supported import functionality
sealed class Imports {
    object parquet : Imports()
}

object Import {

    /** Import data from given source to given importable type, processing `batchSize` rows at a time */
    suspend fun <T: Importable<T>> import(
        type: Imports,
        source: File,
        batchSize: Int,
        cls: Class<T>,
        onStoreBatch: suspend (Long, Long, List<T>) -> Unit
    ): Job {
        return when (type) {
            is Imports.parquet -> readFromParquet(source, batchSize, cls, onStoreBatch)
        }
    }

    private suspend fun <T: Importable<T>> readFromParquet(
        file: File,
        batchSize: Int,
        cls: Class<T>,
        onStoreBatch: suspend (Long, Long, List<T>) -> Unit
    ): Job {
        val accessor = Importable.classToInstance(cls)

        val elementsPerRow = Importable.amountValues(accessor)
        val hydrator = object : Hydrator<MutableList<Any>, T> {
            override fun start(): MutableList<Any> = ArrayList(elementsPerRow)

            override fun add(target: MutableList<Any>?, heading: String?, value: Any?): MutableList<Any> {
                target!!.add(value!!)
                return target
            }

            override fun finish(target: MutableList<Any>?): T = target?.let { accessor.fromValues(it) } ?: throw IllegalStateException("Import failed: Could not finish import for null data row. Is the parquet file correct?")
        }
        return withContext(Dispatchers.IO) {
            return@withContext launch {
                val metadata = ParquetReader.readMetadata(file)
                val totalRows: Long = metadata.blocks.stream().unordered().parallel().mapToLong { blockMetaData -> blockMetaData.rowCount }.sum()
                Timber.d("Found $totalRows rows in ${metadata.blocks.size} metadata blocks.")

                if (totalRows == 0L) {
                    onStoreBatch(0, 0, Collections.emptyList())
                    return@launch
                }
                ParquetReader.streamContent(file, HydratorSupplier.constantly(hydrator)).use { dataStream ->
                    var amountProcessed: Long = 0L
                    for (batch in dataStream.asSequence().chunked(batchSize)) {
                        onStoreBatch(amountProcessed, totalRows, batch)
                        amountProcessed += batch.size
                    }
                }
            }
        }
    }

    suspend fun unzip(
        input: Path,
        password: CharArray,
        destination: Path,
        pollTimeMS: Long,
        onProgress: (Float) -> Unit
    ): Deferred<EximUtil.ZippingState> = withContext(Dispatchers.IO) {
        return@withContext pollForZipFunc(
            func = { unzip(input, password, destination) },
            pollTimeMS = pollTimeMS,
            onProgress = onProgress
        )
    }
    
    private fun unzip(input: Path, password: CharArray, destination: Path): ProgressMonitor {
        val zipFile = ZipFile(input.toFile(), password)
        val progressMonitor = zipFile.progressMonitor
        if (!zipFile.isValidZipFile)
            throw RuntimeException("Zip is not valid")
        zipFile.isRunInThread = true
        zipFile.extractAll(destination.toString())
        return progressMonitor
    }
}