package org.python.companion.ui.security

import android.content.Intent
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.DialogProperties
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
    private val navController: NavHostController,
    private val securityViewModel: SecurityViewModel,
    private val scaffoldState: ScaffoldState
) {
    @OptIn(ExperimentalComposeUiApi::class)
    fun NavGraphBuilder.securityBioGraph() {
        navigation(startDestination = navigationStart, route = "sec/bio") {
            dialog(route = "$navigationStart/login") {
                Login()
            }

            dialog(route = "$navigationStart/setup", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                Setup()
            }

            dialog(route = "$navigationStart/reset", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                if (!switchActor(SecurityActor.TYPE_BIO))
                    return@dialog
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { navController.navigateUp() }

                SecurityBioDialogReset(
                    onNegativeClick = { navController.navigateUp() },
                    onPositiveClick = { launcher.launch(createSecuritySettingsIntent()) }
                )
            }
        }
    }

    @Composable
    fun Login() {
        if (!SecurityState.switchActor(securityViewModel.securityActor, SecurityActor.TYPE_BIO))
            return
        require(securityViewModel.securityActor.canLogin())

        val securityLevel by securityViewModel.securityActor.clearance.collectAsState()
        if (securityLevel > 0)
            navController.navigateUp()

        LaunchedEffect(true) {
            val msgSec = securityViewModel.securityActor.verify(null)
            if (msgSec.type != ResultType.SUCCESS) {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = msgSec.message ?: "There was a login problem.",
                    duration = SnackbarDuration.Short
                )
                navController.navigateUp()
            }
        }
    }

    @Composable
    fun Setup() {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { navController.navigateUp() }

        if (!SecurityState.switchActor(securityViewModel.securityActor, SecurityActor.TYPE_BIO))
            return
        SecurityBioDialogSetup(
            onNegativeClick = { navController.navigateUp() },
            onPositiveClick = { launcher.launch(createSecuritySettingsIntent()) }
        )
    }

    @Composable
    fun Reset() {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { navController.navigateUp() }

        if (!SecurityState.switchActor(securityViewModel.securityActor, SecurityActor.TYPE_BIO))
            return
        SecurityBioDialogReset(
            onNegativeClick = { navController.navigateUp() },
            onPositiveClick = { launcher.launch(createSecuritySettingsIntent()) }
        )
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
        private const val navigationStart = "${SecurityState.navigationStart}/bio"

        fun createBiometricSettingsCreationIntent() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                }
            } else {
                @Suppress("DEPRECATION")
                Intent(Settings.ACTION_FINGERPRINT_ENROLL)
            }
        fun createSecuritySettingsIntent() = Intent(Settings.ACTION_SECURITY_SETTINGS)

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

        @Composable
        fun rememberState(
            navController: NavHostController = rememberNavController(),
            securityViewModel: SecurityViewModel,
            scaffoldState: ScaffoldState
        ) = remember(navController) { SecurityBioState(navController, securityViewModel, scaffoldState) }
    }
}