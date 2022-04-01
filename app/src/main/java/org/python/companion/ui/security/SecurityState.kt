package org.python.companion.ui.security

import android.content.Intent
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import androidx.navigation.*
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.python.backend.security.*
import org.python.companion.support.LoadingState
import org.python.companion.support.UiUtil
import org.python.companion.support.UiUtil.createRoute
import org.python.companion.viewmodels.NoteViewModel
import org.python.companion.viewmodels.SecurityViewModel


class SecurityState(
    private val activity: FragmentActivity,
    private val navController: NavHostController,
    private val securityViewModel: SecurityViewModel,
    private val noteViewModel: NoteViewModel
) {
    @OptIn(ExperimentalComposeUiApi::class)
    fun NavGraphBuilder.securityGraph() {
        navigation(startDestination = navigationStart, route = "sec") {
            dialog(
                route = "$navigationStart?allowedMethods={allowedMethods}",
                arguments = listOf(navArgument("allowedMethods") { defaultValue = CompactSecurityTypeArray.default; type = NavType.IntType }),
                dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
            ) { entry ->
                val allowedMethods = CompactSecurityTypeArray(entry.arguments?.getInt("allowedMethods"))
                val moveToMethod: (@SecurityType Int) -> Unit = { type ->
                    when (type) {
                        SecurityActor.TYPE_BIO -> navigateToBio(navController)
                        SecurityActor.TYPE_PASS -> navigateToPass(navController)
                        else -> throw RuntimeException("How did we get here?!")
                    }
                }
                val authenticated by securityViewModel.securityActor.authenticated.collectAsState()
                when {
                    authenticated -> navController.navigateUp()
                    allowedMethods.allowed().isEmpty() -> throw RuntimeException("Cannot pick security type with 0 allowed methods.")
                    allowedMethods.allowed().size == 1 -> {
                        navController.popBackStack()
                        moveToMethod(allowedMethods.allowed().first())
                    }
                    else -> SecurityPickDialogContent(
                        onNegativeClick = { navController.navigateUp() },
                        onPositiveClick = { type -> moveToMethod(type) },
                        allowedMethods = allowedMethods.allowed()
                    )
                }
            }


            dialog(route = "$navigationStart/pass", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                //TODO: Locking/synchronizing
                // to ensure security type & availability are guaranteed on the same object.
                // Maybe use fancy lambda function that takes lock, executes function, releases lock?
                // https://stackoverflow.com/questions/64116377
                // https://developer.android.com/jetpack/compose/side-effects

                if (!switchActor(SecurityActor.TYPE_PASS))
                    return@dialog
                navController.popBackStack()
                if (securityViewModel.securityActor.hasCredentials()) {
                    navigateToPassAuth(navController)
                } else {
                    navigateToPassSetup(navController)
                }
            }

            dialog(route = "$navigationStart/pass/auth", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                val stateMiniState = UiUtil.StateMiniState.rememberState(LoadingState.READY)
                val authenticated by securityViewModel.securityActor.authenticated.collectAsState()
                if (authenticated) {
                    navController.navigateUp()
                } else {
                    SecurityPasswordDialogContent(
                        onNegativeClick = { navController.navigateUp() },
                        onPositiveClick = { token ->
                            stateMiniState.state.value = LoadingState.LOADING
                            securityViewModel.viewModelScope.launch {
                                val msgSec = securityViewModel.securityActor.verify(token)
                                if (msgSec.type == VerificationMessage.SEC_CORRECT) {
                                    stateMiniState.state.value = LoadingState.OK
                                } else {
                                    stateMiniState.state.value = LoadingState.FAILED
                                    stateMiniState.stateMessage.value = msgSec.body?.userMessage
                                }
                            }
                        },
                        onResetPasswordClick = { navigateToPassReset(navController) },
                        state = stateMiniState
                    )
                }
            }

            dialog(route = "$navigationStart/pass/reset", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                switchActor(type = SecurityActor.TYPE_PASS)
                val hasAuthenticated by securityViewModel.securityActor.authenticated.collectAsState()

                if (hasAuthenticated) {
                    val stateMiniState = UiUtil.StateMiniState.rememberState(LoadingState.READY)
                    SecurityPasswordSetupDialogContent(
                        onNegativeClick = { navController.navigateUp() },
                        onPositiveClick = { token ->
                            stateMiniState.state.value = LoadingState.LOADING
                            securityViewModel.viewModelScope.launch {
                                val msgSet = securityViewModel.securityActor.setCredentials(null, token)
                                if (msgSet.type == VerificationMessage.SEC_CORRECT) {
                                    securityViewModel.securityActor.verify(token)
                                    stateMiniState.state.value = LoadingState.OK
                                    navController.popBackStack()
                                } else {
                                    stateMiniState.state.value = LoadingState.FAILED
                                    stateMiniState.stateMessage.value = msgSet.body?.userMessage
                                }
                            }
                        },
                        title = "Reset password",
                        state = stateMiniState
                    )
                } else {
                    SecurityPasswordResetDialogContent(
                        onNegativeClick = { navController.navigateUp() },
                        onPickOtherMethodClick = { navigateToSecurityPick(navController, SecurityTypes.filter { it != SecurityActor.TYPE_PASS }) },
                        onDestructiveResetPasswordClick = {
                            //TODO: Need an "are you sure?" for destructive operation.
                            securityViewModel.viewModelScope.launch {
                                noteViewModel.deleteAllSecure()
                                navController.navigateUp()
                            }
                        }
                    )
                }
            }

            dialog(route = "$navigationStart/pass/setup", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                switchActor(type = SecurityActor.TYPE_PASS)
                val stateMiniState = UiUtil.StateMiniState.rememberState(LoadingState.READY)
                SecurityPasswordSetupDialogContent(
                    onNegativeClick = { navController.navigateUp() },
                    onPositiveClick = { token ->
                        stateMiniState.state.value = LoadingState.LOADING
                        securityViewModel.viewModelScope.launch {
                            val msgSet = securityViewModel.securityActor.setCredentials(null, token)
                            if (msgSet.type == VerificationMessage.SEC_CORRECT) {
                                securityViewModel.securityActor.verify(token)
                                stateMiniState.state.value = LoadingState.OK
                                navController.navigateUp()
                            } else {
                                stateMiniState.state.value = LoadingState.FAILED
                                stateMiniState.stateMessage.value = msgSet.body?.userMessage
                            }
                        }
                    },
                    title = "Setup password",
                    state = stateMiniState
                )
            }

            dialog(route = "$navigationStart/bio", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                if (!switchActor(SecurityActor.TYPE_BIO))
                    return@dialog

                var state by remember { mutableStateOf(LoadingState.READY) }
                var stateMessage by remember { mutableStateOf<String?>(null) }

                val authenticated by securityViewModel.securityActor.authenticated.collectAsState()
                when {
                    authenticated -> navController.navigateUp()
                    securityViewModel.securityActor.hasCredentials() -> {
                        SecurityBioDialogContent(
                            onNegativeClick = { navController.navigateUp() },
                            onPositiveClick = {
                                state = LoadingState.LOADING
                                securityViewModel.viewModelScope.launch {
                                    val msgSec = securityViewModel.securityActor.verify(null)
                                    if (msgSec.type == VerificationMessage.SEC_CORRECT) {
                                        state = LoadingState.OK
                                    } else {
                                        state = LoadingState.FAILED
                                        stateMessage = msgSec.body?.userMessage
                                    }
                                }
                            },
                            state = state,
                            stateMessage = stateMessage
                        )
                    }
                    else -> {
                        val enrollIntent =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                                    putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                Intent(Settings.ACTION_FINGERPRINT_ENROLL)
                            }
                        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { navController.navigateUp() }
                        launcher.launch(enrollIntent)
                    }
                }
            }
        }
    }

    @Composable
    private fun switchActor(type: @SecurityType Int): Boolean {
        if (securityViewModel.securityActor.type != type)
            securityViewModel.securityActor.switchTo(activity, type)

        val msgAvailable = securityViewModel.securityActor.actorAvailable()
        if (msgAvailable.type != VerificationMessage.SEC_CORRECT) {
            UiUtil.SimpleText("Security method is unavailable. ${msgAvailable.body?.userMessage}")
            return false
        }
        return true
    }

    companion object {
        private const val navigationStart = "securitydialog"


        fun navigateToSecurityPick(navController: NavController, allowedMethods: Collection<@SecurityType Int> = SecurityTypes.toList()) {
            navController.navigate(
                createRoute(navigationStart,
                    optionals = mapOf(
                        "allowedMethods" to CompactSecurityTypeArray.create(allowedMethods).toString()
                    )
                )
            ) {
                launchSingleTop = true
            }
        }
        private fun navigateToBio(navController: NavController) =
            navController.navigate("$navigationStart/bio") {
                launchSingleTop = true
            }
        private fun navigateToPass(navController: NavController) =
            navController.navigate("$navigationStart/pass") {
                launchSingleTop = true
            }
        private fun navigateToPassReset(navController: NavController) = navController.navigate("$navigationStart/pass/reset") {
            launchSingleTop = true
        }
        private fun navigateToPassSetup(navController: NavController) = navController.navigate("$navigationStart/pass/setup") {
            launchSingleTop = true
        }
        private fun navigateToPassAuth(navController: NavController) = navController.navigate("$navigationStart/pass/auth") {
            launchSingleTop = true
        }

        @Composable
        fun rememberState(
            activity: FragmentActivity,
            navController: NavHostController = rememberNavController(),
            securityViewModel: SecurityViewModel,
            noteViewModel: NoteViewModel
        ) = remember(navController) { SecurityState(activity, navController, securityViewModel, noteViewModel) }
    }
}