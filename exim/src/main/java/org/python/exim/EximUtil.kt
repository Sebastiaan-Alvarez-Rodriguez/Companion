package org.python.exim

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.progress.ProgressMonitor

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

    suspend fun pollForZipFunc(func: suspend () -> ProgressMonitor, pollTimeMS: Long = 100, onProgress: (Float) -> Unit): Deferred<ZippingState> {
        return coroutineScope {
            async {
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
    }

    fun verifyZip(location: String): Boolean {
        val zipFile = ZipFile(location)
        return zipFile.isValidZipFile
    }
}