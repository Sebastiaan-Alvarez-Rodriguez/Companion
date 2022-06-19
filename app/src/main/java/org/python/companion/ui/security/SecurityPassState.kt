package org.python.companion.ui.security

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
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
                Login()
            }

            dialog(route = "$navigationStart/setup", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                Setup()
            }

            dialog(route = "$navigationStart/reset", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                Reset()
            }
        }
    }

    @Composable
    fun Login(allowResetCalls: Boolean = true) {
        if (!SecurityState.switchActor(securityViewModel.securityActor, SecurityActor.TYPE_PASS))
            return
        require(securityViewModel.securityActor.canLogin())

        val clearance by securityViewModel.securityActor.clearance.collectAsState()
        if (clearance > 0)
            navController.navigateUp()

        var errorMessage: String? by remember { mutableStateOf(null) }

        SecurityPasswordDialog(
            saltContext = LocalContext.current,
            onNegativeClick = { navController.navigateUp() },
            onPositiveClick = { token ->
                securityViewModel.viewModelScope.launch {
                    val msg = securityViewModel.securityActor.verify(token)
                    if (msg.type != ResultType.SUCCESS) {
                        errorMessage = msg.message ?: "There was a problem setting up a new password."
                    }
                }
            },
            onResetPasswordClick = if (allowResetCalls) {
                { SecurityState.navigateToReset(SecurityActor.TYPE_PASS, navController) }
            } else
                null,
            errorMessage = errorMessage
        )
    }

    @Composable
    fun Setup() {
        if (!SecurityState.switchActor(securityViewModel.securityActor, SecurityActor.TYPE_PASS))
            return
        require(securityViewModel.securityActor.canSetup())
        DoSetCredentialsSetup(securityViewModel, navController)
    }

    @Composable
    fun Reset() {
        if (!SecurityState.switchActor(securityViewModel.securityActor, SecurityActor.TYPE_PASS))
            return
        require(securityViewModel.securityActor.canReset())
        DoSetCredentialsReset(securityViewModel, navController)
    }

    @Composable
    private fun switchActor(type: @SecurityType Int): Boolean {
        if (securityViewModel.securityActor.type() != type)
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

        @Composable
        fun DoSetCredentialsSetup(securityViewModel: SecurityViewModel, navController: NavHostController) =
            DoSetCredentials(securityViewModel, navController, "Setup password")
        @Composable
        fun DoSetCredentialsReset(securityViewModel: SecurityViewModel, navController: NavHostController) =
            DoSetCredentials(securityViewModel, navController, "Reset password")
        @Composable
        fun DoSetCredentials(securityViewModel: SecurityViewModel, navController: NavHostController, title: String) {
            var errorMessage: String? by remember { mutableStateOf(null) }
            SecurityPassDialogSetup(
                onNegativeClick = { navController.navigateUp() },
                onPositiveClick = { token ->
                    securityViewModel.viewModelScope.launch {
                        val msg = securityViewModel.securityActor.setCredentials(null, token)
                        if (msg.type == ResultType.SUCCESS) {
                            securityViewModel.securityActor.verify(token) // This line logs user in after setup.
                            navController.navigateUp()
                        } else {
                            errorMessage = msg.message ?: "There was a problem setting up a new password."
                        }
                    }
                },
                title = title,
                errorMessage = errorMessage
            )
        }

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