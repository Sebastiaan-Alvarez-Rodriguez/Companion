package org.python.backend.security

import androidx.annotation.IntDef
import java.lang.IllegalArgumentException

const val TYPE_PASS = 0
const val TYPE_BIO = 1

@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@IntDef(TYPE_PASS, TYPE_BIO)
annotation class SecurityType

object SecurityProvider {
    fun getActor(@SecurityType type: Int): SecurityActor {
        return when(type) {
            TYPE_PASS -> PassActor()
            TYPE_BIO -> TODO("I must be implemented")
            else -> throw IllegalArgumentException("Illegal security type '$type' specified.")
        }
    }
}