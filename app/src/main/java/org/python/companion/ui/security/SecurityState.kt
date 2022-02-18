package org.python.companion.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import org.python.companion.viewmodels.SecurityViewModel


class SecurityState(
    private val activity: FragmentActivity,
    private val navController: NavHostController,
    private val securityViewModel: SecurityViewModel
) {
    private val navigationStart = "securitydialog"
    fun NavGraphBuilder.securityGraph() {
        navigation(startDestination = navigationStart, route = "sec") {
            dialog("pick") {
                SecurityPickDialogContent(
                    onNegativeClick = { navController.navigateUp() /* TODO: does this work on top-level? */ },
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
                if (securityViewModel.securityActor.type != SecurityActor.TYPE_PASS)
                    securityViewModel.securityActor.switchTo(activity, SecurityActor.TYPE_PASS)

                val msgAvailable = securityViewModel.securityActor.actorAvailable()
                if (msgAvailable.type != VerificationMessage.SEC_CORRECT) {
//                    TODO "Generic warning containing 'bla bla method is unavailable: msg body"
                    return@dialog
                }

                if (securityViewModel.securityActor.hasCredentials()) {
                    SecurityPasswordDialogContent(
                        onNegativeClick = { navController.navigateUp() },
                        onPositiveClick = { token ->
                            securityViewModel.viewModelScope.launch {
                                val msgSec = securityViewModel.securityActor.verify(token)
                                if (msgSec.type == VerificationMessage.SEC_CORRECT)
                                    navController.navigateUp()
                            }
                        }
                    )
                } else {
                    SecurityDialogSetupPasswordContent(
                        onNegativeClick = { navController.navigateUp() },
                        onPositiveClick = { token ->
                            securityViewModel.viewModelScope.launch {
                                val msgSet = securityViewModel.securityActor.setCredentials(null, token)
                                if (msgSet.type == VerificationMessage.SEC_CORRECT)
                                    navController.navigateUp()
                            }
                        }
                    )
                }
            }

            dialog(route = "$navigationStart/bio") {
                // TODO: If no credentials set, do setup, otherwise login
            }
        }
    }

    private fun navigateToBio(navController: NavController) = navController.navigate("$navigationStart/bio")
    private fun navigateToPass(navController: NavController) = navController.navigate("$navigationStart/pass")

    companion object {
        @Composable
        fun rememberState(
            activity: FragmentActivity,
            navController: NavHostController = rememberNavController(),
            securityViewModel: SecurityViewModel
        ) = remember(navController) { SecurityState(activity, navController, securityViewModel) }
    }
}