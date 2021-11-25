package org.python.companion

import android.app.Application
import org.python.companion.logging.ReleaseTree
import timber.log.Timber

class CompanionApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
          Timber.plant(Timber.DebugTree())
        } else {
          Timber.plant(ReleaseTree());
        }
    }
}