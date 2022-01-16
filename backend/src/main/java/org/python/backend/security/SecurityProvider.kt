package org.python.backend.security

import androidx.fragment.app.FragmentActivity

object SecurityProvider {
    fun getActor(@SecurityType type: Int, activity: FragmentActivity): SecurityActor {
        return when(type) {
            SecurityActor.TYPE_PASS -> PassActor(activity)
            SecurityActor.TYPE_BIO -> BioActor(activity)
            else -> throw IllegalArgumentException("Illegal security type '$type' specified.")
        }
    }
}