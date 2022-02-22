package org.python.companion.ui.security

import android.content.Intent
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import org.python.backend.security.SecurityType
import org.python.backend.security.VerificationMessage
import org.python.companion.support.LoadState
import org.python.companion.support.UiUtil
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

                if (!switchActor(SecurityActor.TYPE_PASS))
                    return@dialog

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
                                    navController.navigateUp()
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
                        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
//                            if (it.resultCode == Activity.RESULT_OK)
                                navController.navigateUp()
                        }
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