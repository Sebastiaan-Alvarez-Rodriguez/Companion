package org.python.backend.security

import androidx.annotation.IntDef
import java.lang.IllegalArgumentException

const val TYPE_PASS = 0
const val TYPE_BIO = 1

@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@IntDef(TYPE_PASS, TYPE_BIO)
annotation class SecurityType

class SecurityProvider {
//    init {
//        var preferences: SharedPreferences? = context.getSharedPreferences(
//            context.getString(R.string.pref_security),
//            Context.MODE_PRIVATE
//        )
//    }

    companion object {
        fun getActor(@SecurityType type: Int): SecurityActor {
            return when(type) {
                TYPE_PASS -> PassSecurityActor
                TYPE_BIO -> BioSecurityActor
                else -> throw IllegalArgumentException("Illegal security type '$type' specified.")
            }
        }
    }
}