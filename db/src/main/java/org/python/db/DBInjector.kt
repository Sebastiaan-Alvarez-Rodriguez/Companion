package org.python.db

import android.app.Application

open class DBInjector : Application() {

    // Database singleton
    protected val companionDatabase by lazy {
        CompanionDatabase.getInstance(this)
    }
}