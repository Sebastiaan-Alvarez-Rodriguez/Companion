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
import org.python.companion.support.LoadState
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
                route = "$navigationStart?returnTarget={target}&allowedMethods={allowedMethods}",
                arguments = listOf(
                    navArgument("target") { nullable = true; defaultValue = null; type = NavType.StringType },
                    navArgument("allowedMethods") { defaultValue = CompactSecurityTypeArray.default; type = NavType.IntType }
                ),
                dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
            ) { entry ->
                val target = entry.arguments?.getString("target", null)
                val allowedMethods = CompactSecurityTypeArray(entry.arguments?.getInt("allowedMethods"))
                val moveToMethod: (@SecurityType Int, Boolean) -> Unit = { type, popCurrent ->
                    if (popCurrent)
                        navController.popBackStack()
                    when (type) {
                        SecurityActor.TYPE_BIO -> navigateToBio(navController, returnTarget = target)
                        SecurityActor.TYPE_PASS -> navigateToPass(navController, returnTarget = target)
                        else -> throw RuntimeException("How did we get here?!")
                    }
                }
                when (allowedMethods.allowed().size) {
                    0 -> throw RuntimeException("Cannot pick security type with 0 allowed methods.")
                    1 -> moveToMethod(allowedMethods.allowed().first(), true)
                    else -> SecurityPickDialogContent(
                        onNegativeClick = { navController.navigateUp() },
                        onPositiveClick = { type -> moveToMethod(type, false) },
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

                if (securityViewModel.securityActor.hasCredentials()) { // TODO: Pop this frame from the stack
                    navigateToPassAuth(navController)
                } else {
                    navigateToPassSetup(navController)
                }
            }

            dialog(route = "$navigationStart/pass/auth?returnTarget={target}", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { entry ->
                val stateMiniState = UiUtil.StateMiniState.rememberState(LoadState.STATE_READY)
                SecurityPasswordDialogContent(
                    onNegativeClick = { navController.navigateUp() },
                    onPositiveClick = { token ->
                        stateMiniState.state.value = LoadState.STATE_LOADING
                        securityViewModel.viewModelScope.launch {
                            val msgSec = securityViewModel.securityActor.verify(token)
                            if (msgSec.type == VerificationMessage.SEC_CORRECT) {
                                stateMiniState.state.value = LoadState.STATE_OK
                                val target = entry.arguments?.getString("target", navigationStart) ?: navigationStart
                                navController.popBackStack(route = target, inclusive = target == navigationStart)
                            } else {
                                stateMiniState.state.value = LoadState.STATE_FAILED
                                stateMiniState.stateMessage.value = msgSec.body?.userMessage
                            }
                        }
                    },
                    onResetPasswordClick = { navigateToPassReset(navController) },
                    state = stateMiniState
                )
            }

            dialog(route = "$navigationStart/pass/reset", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                val hasAuthenticated by securityViewModel.securityActor.authenticated.collectAsState()

                if (hasAuthenticated) {
                    navigateToPassSetup(navController, title = "Reset password")
                } else {
                    SecurityPasswordResetDialogContent(
                        onNegativeClick = { navController.navigateUp() },
                        onPickOtherMethodClick = {
                            navigateToSecurityPick(
                                navController,
                                returnTarget = "$navigationStart/pass/reset",
                                allowedMethods = SecurityTypes.filter { it != SecurityActor.TYPE_PASS }
                            )
                            // To get result from a dialog destination: https://stackoverflow.com/questions/50754523/
                        },
                        onDestructiveResetPasswordClick = {
                            //TODO: Need an "are you sure?" for destructive operation.
                            securityViewModel.viewModelScope.launch {
                                noteViewModel.deleteAllSecure()
                            }
                        }
                    )
                }
            }

            dialog(route = "$navigationStart/pass/setup?title={title}", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { entry ->
                val title = entry.arguments?.getString("title", "Setup password") ?: "Setup password"
                val stateMiniState = UiUtil.StateMiniState.rememberState(LoadState.STATE_READY)
                SecurityPasswordSetupDialogContent(
                    onNegativeClick = { navController.navigateUp() },
                    onPositiveClick = { token ->
                        stateMiniState.state.value = LoadState.STATE_LOADING
                        securityViewModel.viewModelScope.launch {
                            val msgSet = securityViewModel.securityActor.setCredentials(null, token)
                            if (msgSet.type == VerificationMessage.SEC_CORRECT) {
                                securityViewModel.securityActor.verify(token)
                                stateMiniState.state.value = LoadState.STATE_OK
                                navController.popBackStack(route = navigationStart, inclusive = true)
                            } else {
                                stateMiniState.state.value = LoadState.STATE_FAILED
                                stateMiniState.stateMessage.value = msgSet.body?.userMessage
                            }
                        }
                    },
                    title = title,
                    state = stateMiniState
                )
            }

            dialog(route = "$navigationStart/bio?returnTarget={target}", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) { entry ->
                if (!switchActor(SecurityActor.TYPE_BIO))
                    return@dialog

                var state by remember { mutableStateOf(LoadState.STATE_READY) }
                var stateMessage by remember { mutableStateOf<String?>(null) }

                if (securityViewModel.securityActor.hasCredentials()) {
                    SecurityBioDialogContent(
                        onNegativeClick = { navController.navigateUp() },
                        onPositiveClick = {
                            state = LoadState.STATE_LOADING
                            securityViewModel.viewModelScope.launch {
                                val msgSec = securityViewModel.securityActor.verify(null)
                                if (msgSec.type == VerificationMessage.SEC_CORRECT) {
                                    state = LoadState.STATE_OK
                                    val target = entry.arguments?.getString("target", navigationStart) ?: navigationStart
                                    navController.popBackStack(route = target, inclusive = target == navigationStart)
                                } else {
                                    state = LoadState.STATE_FAILED
                                    stateMessage = msgSec.body?.userMessage
                                }
                            }
                        },
                        state = state,
                        stateMessage = stateMessage
                    )
                } else {
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

        fun navigateToSecurityPick(
            navController: NavController,
            returnTarget: String? = null,
            allowedMethods: Collection<@SecurityType Int> = SecurityTypes.toList()
        ) {
            navController.navigate(
                createRoute(navigationStart,
                    optionals = mapOf(
                        "target" to returnTarget,
                        "allowedMethods" to CompactSecurityTypeArray.create(allowedMethods).toString()
                    )
                )
            ) {
                launchSingleTop = true
            }
        }
        private fun navigateToBio(navController: NavController, returnTarget: String? = null) =
            navController.navigate("$navigationStart/bio${if (returnTarget != null) "?target=$returnTarget" else ""}") {
                launchSingleTop = true
            }
        private fun navigateToPass(navController: NavController, returnTarget: String? = null) =
            navController.navigate("$navigationStart/pass${if (returnTarget != null) "?target=$returnTarget" else ""}") {
                launchSingleTop = true
            }
        private fun navigateToPassReset(navController: NavController) = navController.navigate("$navigationStart/pass/reset") {
            launchSingleTop = true
        }
        private fun navigateToPassSetup(navController: NavController, title: String? = null) = navController.navigate("$navigationStart/pass/setup${if (title != null) "?title=$title" else ""}") {
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