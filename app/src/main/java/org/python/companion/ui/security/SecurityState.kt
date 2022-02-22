package org.python.companion.ui.security

import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.python.backend.security.SecurityActor
import org.python.backend.security.VerificationMessage
import org.python.companion.support.LoadState
import org.python.companion.viewmodels.SecurityViewModel


class SecurityState(
    private val activity: FragmentActivity,
    private val navController: NavHostController,
    private val securityViewModel: SecurityViewModel
) {
    fun NavGraphBuilder.securityGraph() {
        navigation(startDestination = navigationStart, route = "sec") {
            dialog(navigationStart) {
                SecurityPickDialogContent(
                    onNegativeClick = { navController.navigateUp() },
                    onPositiveClick = { type ->
                        securityViewModel.securityActor.switchTo(activity, type)
                        when (type) {
                            SecurityActor.TYPE_BIO -> navigateToBio(navController)
                            SecurityActor.TYPE_PASS -> navigateToPass(navController)
                            else -> throw RuntimeException("How did we get here?!")
                        }
                    }
                )
            }

            dialog(route = "$navigationStart/pass") {
                //TODO: Locking/synchronizing
                // to ensure security type & availability are guaranteed on the same object.
                // Maybe use fancy lambda function that takes lock, executes function, releases lock?
                // https://stackoverflow.com/questions/64116377
                // https://developer.android.com/jetpack/compose/side-effects

                if (securityViewModel.securityActor.type != SecurityActor.TYPE_PASS)
                    securityViewModel.securityActor.switchTo(activity, SecurityActor.TYPE_PASS)

                val msgAvailable = securityViewModel.securityActor.actorAvailable()
                if (msgAvailable.type != VerificationMessage.SEC_CORRECT) {
//                    TODO "Generic warning containing 'bla bla method is unavailable: msg body"
                    return@dialog
                }

                var state by remember { mutableStateOf(LoadState.STATE_READY) }
                var stateMessage by remember { mutableStateOf<String?>(null) }

                if (securityViewModel.securityActor.hasCredentials()) {
                    SecurityPasswordDialogContent(
                        onNegativeClick = { navController.navigateUp() },
                        onPositiveClick = { token ->
                            state = LoadState.STATE_LOADING
                            securityViewModel.viewModelScope.launch {
                                val msgSec = securityViewModel.securityActor.verify(token)
                                if (msgSec.type == VerificationMessage.SEC_CORRECT) {
                                    state = LoadState.STATE_OK
                                    navController.navigateUp()
                                } else {
                                    state = LoadState.STATE_FAILED
                                    stateMessage = msgSec.body?.userMessage
                                }
                            }
                        }
                    )
                } else {
                    SecurityPasswordSetupDialogContent(
                        onNegativeClick = { navController.navigateUp() },
                        onPositiveClick = { token ->
                            state = LoadState.STATE_LOADING
                            securityViewModel.viewModelScope.launch {
                                val msgSet = securityViewModel.securityActor.setCredentials(null, token)
                                if (msgSet.type == VerificationMessage.SEC_CORRECT) {
                                    state = LoadState.STATE_OK
                                    navController.navigateUp()
                                } else {
                                    state = LoadState.STATE_FAILED
                                    stateMessage = msgSet.body?.userMessage
                                }
                            }
                        },
                        state = state,
                        stateMessage = stateMessage
                    )
                }
            }

            dialog(route = "$navigationStart/bio") {
                // TODO: If no credentials set, do setup, otherwise login
            }
        }
    }

    companion object {
        private val navigationStart = "securitydialog"
        fun navigateToSecurityPick(navController: NavController) = navController.navigate(navigationStart)
        private fun navigateToBio(navController: NavController) = navController.navigate("$navigationStart/bio")
        private fun navigateToPass(navController: NavController) = navController.navigate("$navigationStart/pass")

        @Composable
        fun rememberState(
            activity: FragmentActivity,
            navController: NavHostController = rememberNavController(),
            securityViewModel: SecurityViewModel
        ) = remember(navController) { SecurityState(activity, navController, securityViewModel) }
    }
}