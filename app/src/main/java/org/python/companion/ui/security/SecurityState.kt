package org.python.companion.ui.security

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
) {


    @OptIn(ExperimentalComposeUiApi::class)
    fun NavGraphBuilder.securityGraph() {
        with(securityBioState) { securityBioGraph() }
        with(securityPassState) { securityPassGraph() }

        navigation(startDestination = navigationStart, route = "sec") {
            dialog(
                route = "$navigationStart?allowedMethods={allowedMethods}",
                arguments = listOf(navArgument("allowedMethods") { defaultValue = CompactSecurityTypeArray.default; type = NavType.IntType }),
                dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
            ) { entry ->
                val allowedMethods = CompactSecurityTypeArray(entry.arguments?.getInt("allowedMethods"))
                when {
                    allowedMethods.allowed().isEmpty() -> throw RuntimeException("Cannot pick security type with 0 allowed methods.")
                    allowedMethods.allowed().size == 1 -> {
                        navController.setNavigationResult(result = allowedMethods.allowed().iterator().next())
                        navController.navigateUp()
                    }
                    else -> SecurityPickDialogContent(
                        onNegativeClick = { navController.navigateUp() },
                        onPositiveClick = { type ->
                            navController.setNavigationResult(result = type)
                            navController.navigateUp()
                        },
                        allowedMethods = allowedMethods.allowed()
                    )
                }
            }
        }
    }

    companion object {
        const val navigationStart = "securitydialog"


        fun navigateToSecurityPick(navController: NavController, allowedMethods: Collection<@SecurityType Int> = SecurityTypes.toList(), onPicked: (@SecurityType Int) -> Unit) {
            navController.navigateForResult(
                route = createRoute(navigationStart,
                    optionals = mapOf(
                        "allowedMethods" to CompactSecurityTypeArray.create(allowedMethods).toString()
                    )
                ),
                onResult = onPicked
            )
        }

        fun navigateToLogin(@SecurityType securityType: Int, navController: NavController) {
            when (securityType) {
                SecurityActor.TYPE_PASS -> TODO()
                SecurityActor.TYPE_BIO -> SecurityBioState.navigateToLogin(navController)
            }
        }

        fun navigateToSetup(@SecurityType securityType: Int, navController: NavController) {
            when (securityType) {
                SecurityActor.TYPE_PASS -> TODO()
                    SecurityActor.TYPE_BIO -> SecurityBioState.navigateToSetup(navController)
            }
        }

        fun navigateToReset(@SecurityType securityType: Int, navController: NavController) {
            when (securityType) {
                SecurityActor.TYPE_PASS -> TODO()
                    SecurityActor.TYPE_BIO -> SecurityBioState.navigateToReset(navController)
            }
        }

        @Composable
        fun rememberState(
            activity: FragmentActivity,
            navController: NavHostController = rememberNavController(),
            securityViewModel: SecurityViewModel,
            noteViewModel: NoteViewModel
        ): SecurityState {
            val securityBioState = SecurityBioState.rememberState(activity = activity, securityViewModel = securityViewModel)
            val securityPassState = SecurityPassState.rememberState(activity = activity, securityViewModel = securityViewModel)
            return remember(navController) {
                SecurityState(
                    activity,
                    navController,
                    securityViewModel,
                    noteViewModel,
                    securityBioState,
                    securityPassState
                )
            }
        }
    }
}