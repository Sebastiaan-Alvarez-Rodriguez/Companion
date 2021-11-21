package org.python.companion

import org.python.companion.logging.ReleaseTree
import timber.log.Timber

class Application : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
          Timber.plant(Timber.DebugTree())
        } else {
          Timber.plant(ReleaseTree());
        }
    }
}