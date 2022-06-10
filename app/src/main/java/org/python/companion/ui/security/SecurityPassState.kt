package org.python.companion.ui.security

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.python.companion.support.UiUtil
import org.python.companion.viewmodels.NoteViewModel
import org.python.companion.viewmodels.SecurityViewModel
import org.python.datacomm.ResultType
import org.python.security.SecurityActor
import org.python.security.SecurityType
import org.python.security.SecurityTypes


class SecurityPassState(
    private val activity: FragmentActivity,
    private val navController: NavHostController,
    private val securityViewModel: SecurityViewModel,
    private val noteViewModel: NoteViewModel
) {
    @OptIn(ExperimentalComposeUiApi::class)
    fun NavGraphBuilder.securityPassGraph() {
        navigation(startDestination = navigationStart, route = "sec/pass") {
            dialog(route = "$navigationStart/login", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                if (!switchActor(SecurityActor.TYPE_PASS))
                    return@dialog
                require(securityViewModel.securityActor.hasCredentials())

                val securityLevel by securityViewModel.securityActor.clearance.collectAsState()
                if (securityLevel > 0)
                    navController.navigateUp()

                SecurityPasswordDialog(
                    saltContext = activity.baseContext,
                    onNegativeClick = { navController.navigateUp() },
                    onPositiveClick = { token ->
                        securityViewModel.viewModelScope.launch {
                            val msgSec = securityViewModel.securityActor.verify(token)
                            if (msgSec.type != ResultType.SUCCESS) {
                                //TODO: Snackbar with error message given by: msgSec.message.
                                //TODO: But first, remove 'verificationResult' for something else.
                            }
                        }
                    },
                    onResetPasswordClick = { navigateToReset(navController) },
                )
            }

            dialog(route = "$navigationStart/setup", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                if (!switchActor(SecurityActor.TYPE_PASS))
                    return@dialog

                var errorMessage: String? by remember { mutableStateOf(null) }
                SecurityPassDialogSetup(
                    onNegativeClick = { navController.navigateUp() },
                    onPositiveClick = { token ->
                        securityViewModel.viewModelScope.launch {
                            val msgSet = securityViewModel.securityActor.setCredentials(null, token)
                            if (msgSet.type == ResultType.SUCCESS) {
                                securityViewModel.securityActor.verify(token) // This line logs user in after setup.
                                navController.navigateUp()
                            } else {
                                errorMessage = msgSet.message ?: "There was a problem setting up a new password."
                            }
                        }
                    },
                    title = "Setup password",
                    errorMessage = errorMessage
                )
            }

            dialog(route = "$navigationStart/reset") {
                if (!switchActor(SecurityActor.TYPE_PASS))
                    return@dialog

                val securityLevel by securityViewModel.securityActor.clearance.collectAsState()

                if (securityLevel > 0) {
                    SecurityPassDialogSetup( // TODO: Review logic in here!
                        onNegativeClick = { navController.navigateUp() },
                        onPositiveClick = { token ->
                            securityViewModel.viewModelScope.launch {
                                val msgSet = securityViewModel.securityActor.setCredentials( null, token)
                                if (msgSet.type == ResultType.SUCCESS) {
                                    securityViewModel.securityActor.verify(token)
                                    navController.popBackStack() // TODO: Why no navigateUp?
                                } else {
                                    //TODO: Snackbar with error message given by: msgSec.message.
                                    //TODO: But first, remove 'verificationResult' for something else.
                                }
                            }
                        },
                        title = "Reset password"
                    )
                } else {
                    SecurityPassDialogReset( // TODO: Review logic in here!
                        onNegativeClick = { navController.navigateUp() },
                        onPickOtherMethodClick = {
                            SecurityState.navigateToSecurityPick(navController, SecurityTypes.filter { it != SecurityActor.TYPE_PASS }, onPicked = {})
                        },
                        onDestructiveResetPasswordClick = {
                            //TODO: Need an "are you sure?" for destructive operation.
                            securityViewModel.viewModelScope.launch {
                                noteViewModel.deleteAllSecure()
                                navController.navigateUp()
                            }
                        }
                    )
                }

                SecurityBioDialogReset(
                    onNegativeClick = { navController.navigateUp() },
                    onPositiveClick = { navigateToSetupOrResetAction(navController) }
                )
            }
        }
    }

    @Composable
    private fun switchActor(type: @SecurityType Int): Boolean {
        if (securityViewModel.securityActor.type != type)
            securityViewModel.securityActor.switchTo(type)

        val msgAvailable = securityViewModel.securityActor.actorAvailable()
        if (msgAvailable.type != ResultType.SUCCESS) {
            UiUtil.SimpleText("Security method is unavailable: ${msgAvailable.message}")
            return false
        }
        return true
    }

    companion object {
        private const val navigationStart = "${SecurityState.navigationStart}/pass"

        fun navigateToLogin(navController: NavController) =
            navController.navigate("$navigationStart/login") {
                launchSingleTop = true
            }
        fun navigateToSetup(navController: NavController) =
            navController.navigate("$navigationStart/setup") {
                launchSingleTop = true
            }
        fun navigateToReset(navController: NavController) =
            navController.navigate("$navigationStart/reset") {
                launchSingleTop = true
            }

        private fun navigateToSetupOrResetAction(navController: NavController) =
            navController.navigate("$navigationStart/setupOrResetAction") {
                launchSingleTop = true
            }

        @Composable
        fun rememberState(
            activity: FragmentActivity,
            navController: NavHostController = rememberNavController(),
            securityViewModel: SecurityViewModel,
            noteViewModel: NoteViewModel
        ) = remember(navController) { SecurityPassState(activity, navController, securityViewModel, noteViewModel) }
    }
}