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

/**
 * Interface for importable data.
 * @param T type of importable data.
 * Requires `T` to have an empty constructor.
 */
interface Importable<T> {
    val amountValues: Int
    fun fromValues(values: List<Any?>): T

    companion object {
        fun <T: Importable<T>> amountValues(cls: Class<T>) =
            classToInstance(cls).amountValues

        fun <T: Importable<T>> fromValues(values: List<Any?>, cls: Class<T>): T =
            classToInstance(cls).fromValues(values)

        private fun <T: Importable<T>> classToInstance(cls: Class<T>) =
            cls.getDeclaredConstructor().newInstance()
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
        val elementsPerRow = Importable.amountValues(cls)
        val hydrator = object : Hydrator<MutableList<EximUtil.FieldInfo>, List<EximUtil.FieldInfo>> {
            override fun start(): MutableList<EximUtil.FieldInfo> = ArrayList() // TODO: compute likely arraylist size

            override fun add(
                target: MutableList<EximUtil.FieldInfo>?,
                heading: String?,
                value: Any?
            ): MutableList<EximUtil.FieldInfo> {
                target!!.add(EximUtil.FieldInfo(value, heading!!))
                return target
            }

            override fun finish(target: MutableList<EximUtil.FieldInfo>?): MutableList<EximUtil.FieldInfo> = target ?: start()
        }
        return withContext(Dispatchers.IO) {
            return@withContext launch {
                val dataStream = ParquetReader.streamContent(file, HydratorSupplier.constantly(hydrator))

                dataStream.use { stream -> //TODO: Early closing?
                    var count = 0L
                    val dataSeq = stream.collect(Collectors.toList())//.asSequence()
                    while (true) {
                        Timber.e("Taking batch (cur: $count): ${batchSize * elementsPerRow} items")
//                        val batches = dataSeq.chunked(batchSize * elementsPerRow)
//
//                        batches.forEach {
//
//                        }
                        val batch = dataSeq.take(batchSize * elementsPerRow).toList()
                        Timber.e("Took batch (${batch.size})")

                        if (batch.isEmpty())
                            break
                        count += batch.size / elementsPerRow

                        val items = (0..batch.size step elementsPerRow).asSequence().map {
                            val rowData = batch.subList(it, elementsPerRow)
                            val item: T = Importable.fromValues(rowData, cls)
                            onProgress(item, count)
                            item
                        }
                        items.forEach { Timber.e("Read item: $it") }
                        //TODO: Store items
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
//        val zipParameters = UnzipParameters()
//        zipParameters = true
//        zipParameters.encryptionMethod = EncryptionMethod.AES
//        zipParameters.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256

        val zipFile = ZipFile(input)//, password)
        val progressMonitor = zipFile.progressMonitor
        if (!zipFile.isValidZipFile)
            throw RuntimeException("Zip is not valid")
        zipFile.isRunInThread = true
        zipFile.extractFile(inZipName, destination)
        return progressMonitor
    }
}