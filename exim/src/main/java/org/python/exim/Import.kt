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
import java.util.stream.Collectors
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
    suspend fun <T: Importable<T>> import(type: Imports, source: File, batchSize: Int, cls: Class<T>, onProgress: (T, Long) -> Unit): Job {
        return when (type) {
            is Imports.parquet -> readFromParquet(source, batchSize, cls, onProgress)
        }
    }

    private suspend fun <T: Importable<T>> readFromParquet(file: File, batchSize: Int, cls: Class<T>, onProgress: (T, Long) -> Unit): Job {
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
                ParquetReader.streamContent(file, HydratorSupplier.constantly(hydrator)).use { dataStream ->
                    var count = 0L
                    for (batch in dataStream.asSequence().chunked(batchSize)) {
                        Timber.e("Took batch (${batch.size}, max=$batchSize)")
                        count += batch.size
                        batch.forEach { item ->
                            onProgress(item, count)
                            Timber.e("    $item")
                            //TODO: Store items one by one, or rather as a batch?
                        }
                    }
                }
            }
        }
    }

    suspend fun unzip(
        input: File,
        inZipName: String,
        password: CharArray,
        destination: String,
        pollTimeMS: Long,
        onProgress: (Float) -> Unit
    ): Deferred<EximUtil.ZippingState> = withContext(Dispatchers.IO) {
        return@withContext pollForZipFunc(
            func = { unzip(input, inZipName, password, destination) },
            pollTimeMS = pollTimeMS,
            onProgress = onProgress
        )
    }
    
    private fun unzip(input: File, inZipName: String, password: CharArray, destination: String): ProgressMonitor {
        val zipFile = ZipFile(input, password)
        val progressMonitor = zipFile.progressMonitor
        if (!zipFile.isValidZipFile)
            throw RuntimeException("Zip is not valid")
        zipFile.isRunInThread = true
        zipFile.extractFile(inZipName, destination)
        return progressMonitor
    }
}