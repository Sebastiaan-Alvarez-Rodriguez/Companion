package org.python.companion.support

import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

object Util {
    fun effect(scope: CoroutineScope, block: suspend () -> Unit) {
        scope.launch(Dispatchers.IO) { block() }
    }

    fun <T> Flow<T>.stateInViewModel(scope: CoroutineScope, initialValue : T): StateFlow<T> =
        stateIn(scope = scope, started = SharingStarted.Lazily, initialValue = initialValue)

    abstract class DialogMiniState(public val open: Boolean) {}

    fun navigateReplaceStartRoute(navController: NavController, newHomeRoute: String) {
        with (navController) {
            popBackStack(graph.startDestinationId, true)
            graph.setStartDestination(newHomeRoute)
            navigate(newHomeRoute)
        }
    }

    /**
     * Navigate to a new destination, simultaneously removing the current entry from the backstack.
     * @param navController
     * @param newRoute New route path to follow.
     */
    fun navigatePop(navController: NavController, newRoute: String) {
        with (navController) {
            popBackStack()
            navigate(newRoute)
        }
    }
}