package org.python.companion.ui.security

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.navigation.*
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import org.python.companion.support.UiUtil.createRoute
import org.python.companion.support.UiUtil.navigateForResult
import org.python.companion.support.UiUtil.setNavigationResult
import org.python.companion.viewmodels.NoteViewModel
import org.python.companion.viewmodels.SecurityViewModel
import org.python.datacomm.ResultType
import org.python.security.CompactSecurityTypeArray
import org.python.security.SecurityActor
import org.python.security.SecurityType
import org.python.security.SecurityTypes


class SecurityState(
    private val activity: FragmentActivity,
    private val navController: NavHostController,
    private val securityViewModel: SecurityViewModel,
    private val noteViewModel: NoteViewModel,
    private val securityBioState: SecurityBioState,
    private val securityPassState: SecurityPassState,
    private val scaffoldState: ScaffoldState
) {

    fun load(activity: FragmentActivity) = securityViewModel.securityActor.load(activity)

    @OptIn(ExperimentalComposeUiApi::class)
    fun NavGraphBuilder.securityGraph() {
        with(securityBioState) { securityBioGraph() }
        with(securityPassState) { securityPassGraph() }

        navigation(startDestination = navigationStart, route = "sec") {
            dialog(
                route = "$navigationStart?allowedMethods={allowedMethods}&key={key}",
                arguments = listOf(
                    navArgument("allowedMethods") { defaultValue = CompactSecurityTypeArray.default; type = NavType.IntType },
                    navArgument("key") { defaultValue = "result"; type = NavType.StringType }
                ),
                dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
            ) { entry ->
                val allowedMethods = CompactSecurityTypeArray(entry.arguments?.getInt("allowedMethods"))
                val key = entry.arguments?.getString("key") ?: "result"
                val allowedValues = allowedMethods.allowed()
                when {
                    allowedValues.isEmpty() -> throw RuntimeException("Cannot pick security type with 0 allowed methods.")
                    allowedValues.size == 1 -> {
                        navController.setNavigationResult(result = allowedValues.iterator().next(), key = key)
                        navController.navigateUp()
                    }
                    else -> SecurityDialogPick(
                        onNegativeClick = { navController.navigateUp() },
                        onPositiveClick = { type ->
                            navController.setNavigationResult(result = type, key = key)
                            navController.navigateUp()
                        },
                        allowedMethods = allowedValues
                    )
                }
            }

            dialog(route = "$navigationStart/setupOptions", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                SecurityDialogSetup(
                    onNegativeClick = { navController.navigateUp() },
                    onLoginClick = {
                        navController.setNavigationResult(result = false, key = "$navigationStart/setupOptions")
                        navController.navigateUp()
                    },
                    loginMethods = securityViewModel.securityActor.setupMethods().map { securityViewModel.securityActor.methodName(it) }
                )
            }

            dialog(route = "$navigationStart/resetOptions", dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
                SecurityDialogReset(
                    onNegativeClick = { navController.navigateUp() },
                    onDestroyClick = {
                        navController.setNavigationResult(result = true, key = "$navigationStart/resetOptions")
                        navController.navigateUp()
                    },
                    onLoginClick = {
                        navController.setNavigationResult(result = false, key = "$navigationStart/resetOptions")
                        navController.navigateUp()
                    },
                    loginMethods = securityViewModel.securityActor.setupMethods().map { securityViewModel.securityActor.methodName(it) }
                )
            }
        }
    }

    companion object {
        const val navigationStart = "securitydialog"

        fun navigateToSecurityPick(
            navController: NavController,
            allowedMethods: Collection<@SecurityType Int> = SecurityTypes.toList(),
            onPicked: (@SecurityType Int) -> Unit,
            key: String = "result"
        ) {
            navController.navigateForResult(
                route = createRoute(navigationStart,
                    optionals = mapOf(
                        "allowedMethods" to CompactSecurityTypeArray.create(allowedMethods).toString(),
                        "key" to key
                    )
                ),
                key = key,
                onResult = onPicked
            )
        }

        fun navigateToSetupOptions(navController: NavController, onLoginClick: () -> Unit) {
            navController.navigateForResult<Boolean>(
                route = "$navigationStart/setupOptions",
                key = "$navigationStart/setupOptions",
                onResult = { onLoginClick() }
            )
        }

        fun navigateToResetOptions(navController: NavController, onDestroyClick: () -> Unit, onLoginClick: () -> Unit) {
            navController.navigateForResult<Boolean>(
                route = "$navigationStart/resetOptions",
                key = "$navigationStart/resetOptions",
                onResult = { destroy -> if (destroy) onDestroyClick() else onLoginClick() }
            )
        }

        fun navigateToLogin(@SecurityType securityType: Int, navController: NavController) {
            when (securityType) {
                SecurityActor.TYPE_PASS -> SecurityPassState.navigateToLogin(navController)
                SecurityActor.TYPE_BIO -> SecurityBioState.navigateToLogin(navController)
            }
        }

        fun navigateToSetup(@SecurityType securityType: Int, navController: NavController) {
            when (securityType) {
                SecurityActor.TYPE_PASS -> SecurityPassState.navigateToSetup(navController)
                SecurityActor.TYPE_BIO -> SecurityBioState.navigateToSetup(navController)
            }
        }

        fun navigateToReset(@SecurityType securityType: Int, navController: NavController) {
            when (securityType) {
                SecurityActor.TYPE_PASS -> SecurityPassState.navigateToReset(navController)
                SecurityActor.TYPE_BIO -> SecurityBioState.navigateToReset(navController)
            }
        }

        fun switchActor(securityActor: SecurityActor, type: @SecurityType Int): Boolean {
            securityActor.switchTo(type)

            val msgAvailable = securityActor.actorAvailable()
            return msgAvailable.type == ResultType.SUCCESS
        }

        @Composable
        fun rememberState(
            activity: FragmentActivity,
            navController: NavHostController = rememberNavController(),
            securityViewModel: SecurityViewModel,
            noteViewModel: NoteViewModel,
            scaffoldState: ScaffoldState
        ): SecurityState {
            val securityBioState = SecurityBioState.rememberState(
                navController = navController,
                securityViewModel = securityViewModel,
                scaffoldState = scaffoldState
            )
            val securityPassState = SecurityPassState.rememberState(
                activity = activity,
                navController = navController,
                securityViewModel = securityViewModel,
                noteViewModel = noteViewModel
            )
            return remember(navController) {
                SecurityState(
                    activity,
                    navController,
                    securityViewModel,
                    noteViewModel,
                    securityBioState,
                    securityPassState,
                    scaffoldState
                )
            }
        }
    }
}