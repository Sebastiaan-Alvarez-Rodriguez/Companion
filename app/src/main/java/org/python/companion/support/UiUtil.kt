package org.python.companion.support

import androidx.annotation.IntDef
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

object LoadState {
    const val STATE_READY = 0
    const val STATE_LOADING = 1
    const val STATE_OK = 2
    const val STATE_FAILED = 3
}
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@IntDef(LoadState.STATE_READY, LoadState.STATE_LOADING, LoadState.STATE_OK, LoadState.STATE_FAILED)
annotation class LoadingState

object UiUtil {
    @Composable
    fun SimpleLoading() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }

    @Composable
    fun SimpleOk() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Filled.CheckCircle, "Ok")
        }
    }



    fun effect(scope: CoroutineScope, block: suspend () -> Unit) {
        scope.launch(Dispatchers.IO) { block() }
    }

    fun <T> Flow<T>.stateInViewModel(scope: CoroutineScope, initialValue : T): StateFlow<T> =
        stateIn(scope = scope, started = SharingStarted.Lazily, initialValue = initialValue)

    open class DialogMiniState(val open: MutableState<Boolean>) {
        open fun open() {
            open.value = true
        }

        open fun close() {
            open.value = false
        }

        companion object {
            @Composable
            fun rememberState(open: Boolean = false) = remember(open) {
                DialogMiniState(open = mutableStateOf(open))
            }
        }
    }

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