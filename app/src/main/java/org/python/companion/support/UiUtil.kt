package org.python.companion.support

import androidx.annotation.IntDef
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.python.companion.R

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
    fun SimpleProblem(message: String, prefix: String = "Error", icon: ImageVector = Icons.Outlined.ErrorOutline) {
        val defaultPadding = dimensionResource(id = R.dimen.padding_default)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Error",
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(defaultPadding))
                Text("[$prefix]: $message")
            }
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

    @Composable
    fun SimpleText(text: String) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text)
        }
    }

    fun createRoute(base: String, args: Collection<String>? = null, optionals: Map<String, String?>? = null): String {
        return base + when (args.isNullOrEmpty()) {
            true -> ""
            false -> "/"+args.joinToString(separator="/")
        } + when(optionals.isNullOrEmpty()) {
            true -> ""
            false -> "?"+optionals.filterValues { v -> v != null }.map { (k, v) -> "$k=$v" }.joinToString(separator = "&")
        }
    }


    fun NavController.getNavigationResult(key: String = "result") {
        currentBackStackEntry?.savedStateHandle?.getLiveData<String>(key)
    }
    fun NavController.setNavigationResult(result: String, key: String = "result") {
        previousBackStackEntry?.savedStateHandle?.set(key, result)
    }

    fun effect(scope: CoroutineScope, block: suspend () -> Unit) =scope.launch(Dispatchers.IO) { block() }

    fun <T> Flow<T>.stateInViewModel(scope: CoroutineScope, initialValue : T): StateFlow<T> =
        stateIn(scope = scope, started = SharingStarted.Lazily, initialValue = initialValue)

    open class OpenableMiniState(val open: MutableState<Boolean>) {
        open fun open() {
            open.value = true
        }

        open fun close() {
            open.value = false
        }

        companion object {
            @Composable
            fun rememberState(open: Boolean = false) = remember(open) {
                OpenableMiniState(open = mutableStateOf(open))
            }
        }
    }

    open class StateMiniState(
        val state: MutableState<@LoadingState Int>,
        val stateMessage: MutableState<String?>
    ) {
        companion object {
            @Composable
            fun rememberState(state: @LoadingState Int, stateMessage: String? = null) =
                remember(state) { StateMiniState(mutableStateOf(state), mutableStateOf(stateMessage)) }
        }
    }
    fun navigateReplaceStartRoute(navController: NavController, newHomeRoute: String) = with (navController) {
        popBackStack(graph.startDestinationId, true)
        graph.setStartDestination(newHomeRoute)
        navigate(newHomeRoute)
    }

    fun navigateOutOfGraph(navController: NavController) = with (navController) {
        popBackStack(graph.startDestinationId, true)
        navigateUp()
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