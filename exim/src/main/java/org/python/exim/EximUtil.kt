package org.python.exim

import kotlinx.coroutines.*
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object EximUtil {
    data class FieldInfo(val value: Any?, val name: String)

    enum class MergeStrategy {
        DELETE_ALL_BEFORE,
        SKIP_ON_CONFLICT,
        OVERRIDE_ON_CONFLICT
    }

    data class ZippingState(val state: FinishState, val error: String? = null)

    enum class FinishState {
        SUCCESS,
        ERROR,
        CANCELLED
    }

    suspend fun pollForZipFunc(
        func: suspend () -> ProgressMonitor,
        pollTimeMS: Long = 100,
        onProgress: (Float) -> Unit
    ): Deferred<ZippingState> = withContext(Dispatchers.IO) {
        return@withContext async {
            val progressMonitor = func()
            while (progressMonitor.result == ProgressMonitor.Result.WORK_IN_PROGRESS || progressMonitor.result == null) {
                delay(pollTimeMS)
                onProgress(progressMonitor.workCompleted.toFloat() / progressMonitor.totalWork)
            }

            return@async when (progressMonitor.result) {
                ProgressMonitor.Result.SUCCESS -> ZippingState(FinishState.SUCCESS)
                ProgressMonitor.Result.ERROR -> ZippingState(
                    FinishState.ERROR,
                    progressMonitor.exception.message
                )
                ProgressMonitor.Result.CANCELLED -> ZippingState(
                    FinishState.CANCELLED,
                    "Zipping was cancelled"
                )
                else -> throw IllegalStateException("Unexpected zipping progress '${progressMonitor.result}'")
            }
        }
    }

    fun verifyZip(location: String): Boolean {
        val zipFile = ZipFile(location)
        return zipFile.isValidZipFile
    }
    fun verifyZip(file: File): Boolean = verifyZip(file.path)

    fun verifyZip(inputStream: InputStream): Boolean {
        val tmpFile = File.createTempFile("companion-verifyzip", ".zip")
        copyStream(inputStream, tmpFile.outputStream())
        val returnValue = verifyZip(tmpFile)
        tmpFile.delete()
        inputStream.close()
        return returnValue
    }

    private fun copyStream(
        inStream: InputStream,
        outStream: OutputStream,
    ) {
        val bufferSize = 4096
        val bytes = ByteArray(bufferSize)
        var count: Int
        try {
            do {
                count = inStream.read(bytes)
                if (count > 0) {
                    outStream.write(bytes, 0, count)
                }
            } while (count > 0)
        } finally {
            outStream.flush()
            outStream.close()
            inStream.close()
        }
    }
}