package org.python.backend.util

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object CoroutineUtil {
    interface Callback<T> {
        fun onResult(result: T)
    }

    suspend fun <T> awaitCallback(block: (Callback<T>) -> Unit) : T =
        suspendCoroutine { cont ->
            block(object : Callback<T> {
                override fun onResult(result: T) = cont.resume(result)
            })
        }

    suspend fun <T> awaitSuspendingCallback(block: suspend (Callback<T>) -> Unit) : T {
        return suspendCoroutine { cont: Continuation<T> ->
            suspend {
                block(object : Callback<T> {
                    override fun onResult(result: T) = cont.resume(result)
                })
            }
        }
    }
}