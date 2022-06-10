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
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import org.python.companion.support.UiUtil
import org.python.companion.viewmodels.SecurityViewModel
import org.python.datacomm.ResultType
import org.python.security.SecurityActor
import org.python.security.SecurityType


class SecurityBioState(
    private val activity: FragmentActivity,
    private val navController: NavHostController,
    private val securityViewModel: SecurityViewModel
) {
    fun NavGraphBuilder.securityBioGraph() {
        navigation(startDestination = navigationStart, route = "sec/bio") {
            dialog(route = "$navigationStart/login") {
                if (!switchActor(SecurityActor.TYPE_BIO))
                    return@dialog
                require(securityViewModel.securityActor.hasCredentials())

                val securityLevel by securityViewModel.securityActor.clearance.collectAsState()
                if (securityLevel > 0)
                    navController.navigateUp()

                LaunchedEffect(true) {
                    val msgSec = securityViewModel.securityActor.verify(null)
                    if (msgSec.type != ResultType.SUCCESS) {
                        //TODO: Snackbar with error message given by: msgSec.message.
                        //TODO: But first, remove 'verificationResult' for something else.
                    }
                }
            }

            dialog(route = "$navigationStart/setup") {
                if (!switchActor(SecurityActor.TYPE_BIO))
                    return@dialog
                SecurityBioDialogSetup(
                    onNegativeClick = { navController.navigateUp() },
                    onPositiveClick = { navigateToSetupOrResetAction(navController) }
                )
            }

            dialog(route = "$navigationStart/reset") {
                if (!switchActor(SecurityActor.TYPE_BIO))
                    return@dialog
                SecurityBioDialogReset(
                    onNegativeClick = { navController.navigateUp() },
                    onPositiveClick = { navigateToSetupOrResetAction(navController) }
                )
            }

            dialog(route = "$navigationStart/setupOrResetAction") {
                GoToAndroidBiometricSettings()
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

    @Composable
    private fun GoToAndroidBiometricSettings() {
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

    companion object {
        private const val navigationStart = "${SecurityState.navigationStart}/bio"

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
        ) = remember(navController) { SecurityBioState(activity, navController, securityViewModel) }
    }
}