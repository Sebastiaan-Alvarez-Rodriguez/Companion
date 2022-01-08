package org.python.backend.security

import androidx.annotation.IntDef
import androidx.fragment.app.FragmentActivity

const val TYPE_PASS = 0
const val TYPE_BIO = 1

@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@IntDef(TYPE_PASS, TYPE_BIO)
annotation class SecurityType

object SecurityProvider {
    fun getActor(@SecurityType type: Int, activity: FragmentActivity): SecurityActor {
        return when(type) {
            TYPE_PASS -> PassActor(activity)
            TYPE_BIO -> BioActor(activity)
            else -> throw IllegalArgumentException("Illegal security type '$type' specified.")
        }
    }
}