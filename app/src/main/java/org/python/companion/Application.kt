package org.python.companion

import org.python.backend.BackendInjector
import org.python.companion.logging.ReleaseTree
import timber.log.Timber

class CompanionApplication : BackendInjector() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
          Timber.plant(Timber.DebugTree())
        } else {
          Timber.plant(ReleaseTree());
        }
    }
}