package org.python.companion.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import org.python.companion.CompanionApplication

class SecurityViewModel(application: Application) : AndroidViewModel(application) {
    val securityActor = (application as CompanionApplication).securityActor
}