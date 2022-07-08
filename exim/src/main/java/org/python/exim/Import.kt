package org.python.exim

import blue.strategic.parquet.Hydrator
import blue.strategic.parquet.HydratorSupplier
import blue.strategic.parquet.ParquetReader
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.progress.ProgressMonitor
import org.apache.parquet.schema.MessageType
import org.python.exim.EximUtil.pollForZipFunc
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.streams.asSequence

interface Importable {
    val amountValues: Int
    fun <T> fromValues(values: Array<Any?>): T
}

// List of supported import functionality
sealed class Imports {
    data class parquet(val schema: MessageType) : Imports()
}

object Import {
    /** Amount of rows to push to read and push into storage at once */
    private const val ROWS_PER_TABLE_PUSH = 1000

    suspend fun <T: Importable> import(type: Imports, source: File, cls: Class<T>, onProgress: (T, Long) -> Unit): List<T> {
        return when (type) {
            is Imports.parquet -> readFromParquet(type.schema, source, cls, onProgress)
        }
    }

    private suspend fun <T: Importable> readFromParquet(schema: MessageType, file: File, cls: Class<T>, onProgress: (T, Long) -> Unit): List<T> {
        val elementsPerRow = cls.getDeclaredConstructor().newInstance().amountValues
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
        withContext(Dispatchers.IO) {
            return@withContext launch {
                val dataStream = ParquetReader.streamContent(file, HydratorSupplier.constantly(hydrator))

                dataStream.use { dataStream ->
                    var count = 0L
                    val dataSeq = dataStream.asSequence()
                    while (true) {
                        val batch = dataSeq.take(ROWS_PER_TABLE_PUSH * elementsPerRow).toList()
                        if (batch.isEmpty())
                            break
                        count += batch.size
                        //TODO: do something with batch
                        onProgress(batch.get(0), count)
                    }
                }
            }
        }
        return Collections.emptyList()
    }


    suspend fun unzip(
        input: File,
        password: CharArray,
        destination: String,
        pollTimeMS: Long,
        onProgress: (Float) -> Unit
    ): Deferred<EximUtil.ZippingState> = withContext(Dispatchers.IO) {
        return@withContext pollForZipFunc(func = { unzip(input, password, destination) }, pollTimeMS = pollTimeMS, onProgress = onProgress)
    }
    
    private fun unzip(input: File, password: CharArray, destination: String): ProgressMonitor {
        val zipParameters = ZipParameters()
        zipParameters.isEncryptFiles = true
        zipParameters.encryptionMethod = EncryptionMethod.AES
        zipParameters.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256

        val zipFile = ZipFile(input, password)
        try {
            zipFile.isRunInThread = true
            val progressMonitor = zipFile.progressMonitor
            zipFile.extractAll(destination)
            zipFile.close()
            return progressMonitor
        } catch (e: Exception) {
            zipFile.close()
            Timber.e("Got error during zipping process: ", e)
            throw e
        }
    }
}