package org.python.security.util

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
}