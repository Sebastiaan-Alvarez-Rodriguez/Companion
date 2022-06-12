package org.python.companion.ui.security

import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import androidx.navigation.*
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.python.companion.support.UiUtil.createRoute
import org.python.companion.support.UiUtil.navigateForResult
import org.python.companion.support.UiUtil.setNavigationResult
import org.python.companion.ui.settings.SettingsState
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
                route = "$navigationStart?autoPick={autoPick}&allowedMethods={allowedMethods}&key={key}&disallowedReason={disallowedReason}",
                arguments = listOf(
                    navArgument("autoPick") { defaultValue = true; type = NavType.BoolType },
                    navArgument("allowedMethods") { defaultValue = CompactSecurityTypeArray.default; type = NavType.IntType },
                    navArgument("key") { defaultValue = "result"; type = NavType.StringType },
                    navArgument("disallowedReason") { defaultValue = ""; type = NavType.StringType }
                ),
                dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
            ) { entry ->
                val autoPick = entry.arguments?.getBoolean("autoPick") ?: true
                val allowedMethods = CompactSecurityTypeArray(entry.arguments?.getInt("allowedMethods"))
                val key = entry.arguments?.getString("key") ?: "result"
                val disallowedReason = entry.arguments?.getString("disallowedReason") ?: ""
                val allowedValues = allowedMethods.allowed()

                if (autoPick) {
                    if (allowedValues.size == 1) {
                        navController.setNavigationResult(result = allowedValues.iterator().next(), key = key)
                        navController.navigateUp()
                    } else {
                        SecurityDialogPick(
                            onNegativeClick = { navController.navigateUp() },
                            onPositiveClick = { type ->
                                navController.setNavigationResult(result = type, key = key)
                                navController.navigateUp()
                            },
                            allowedMethods = allowedValues
                        )
                    }
                } else {
                    SecurityDialogPick(
                        onNegativeClick = { navController.navigateUp() },
                        onPositiveClick = { type ->
                            navController.setNavigationResult(result = type, key = key)
                            navController.navigateUp()
                        },
                        allowedMethods = allowedValues,
                        showDisallowed = true,
                        disallowedReason = disallowedReason
                    )
                }
            }

            dialog(
                route = "${SettingsState.navigationStart}/setup/{securityMethod}",
                arguments = listOf(navArgument("securityMethod") {type = NavType.IntType}),
                dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
            ) { entry ->
                val method: @SecurityType Int = entry.arguments?.getInt("securityMethod") ?: SecurityActor.TYPE_UNDEFINED
                require(switchActor(noteViewModel.securityActor, method))

                val canSetup by noteViewModel.securityActor.canSetup.collectAsState(false)
                if (canSetup) {
                    SecurityDialogSetupSpecific(
                        method = method,
                        securityViewModel = securityViewModel,
                        navController = navController
                    )
                } else {
                    SecurityDialogSetupOptions(
                        onNegativeClick = { navController.navigateUp() },
                        onLoginClick = {
                            navigateToSecurityPick(
                                navController,
                                allowedMethods = SecurityTypes.filter { it != method },
                                key = "pickForSetupOtherLogin",
                                onPicked = { type -> navigateToLogin(type, navController) }
                            )
                        },
                        loginMethods = noteViewModel.securityActor.setupMethods().map { noteViewModel.securityActor.methodName(it) }
                    )
                }
            }

            dialog(
                route = "${SettingsState.navigationStart}/reset/{securityMethod}",
                arguments = listOf(navArgument("securityMethod") {type = NavType.IntType}),
                dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
            ) { entry ->
                val method: @SecurityType Int = entry.arguments?.getInt("securityMethod") ?: SecurityActor.TYPE_UNDEFINED
                require(switchActor(noteViewModel.securityActor, method))

                val canReset by noteViewModel.securityActor.canReset.collectAsState(false)
                if (canReset) {
                    SecurityDialogResetSpecific(
                        method = method,
                        securityViewModel = securityViewModel,
                        navController = navController
                    )
                } else {
                    SecurityDialogReset(
                        onNegativeClick = { navController.navigateUp() },
                        onDestroyClick = { /* TODO: Are you sure? */ noteViewModel.viewModelScope.launch { noteViewModel.deleteAllSecure() }},
                        onLoginClick = {
                            navigateToSecurityPick(
                                navController,
                                allowedMethods = SecurityTypes.filter { it != method },
                                key = "pickForResetOtherLogin",
                                onPicked = { type ->
                                    navigateToLogin(type, navController)
                                }
                            )
                        },
                        loginMethods = securityViewModel.securityActor.setupMethods().map { securityViewModel.securityActor.methodName(it) }
                    )
                }
            }
        }
    }

    companion object {
        const val navigationStart = "securitydialog"

        fun navigateToSecurityPick(
            navController: NavController,
            autoPick: Boolean = true,
            allowedMethods: Collection<@SecurityType Int> = SecurityTypes.toList(),
            disallowedReason: String = "",
            onPicked: (@SecurityType Int) -> Unit,
            key: String = "result"
        ) {
            navController.navigateForResult(
                route = createRoute(navigationStart,
                    optionals = mapOf(
                        "autoPick" to autoPick.toString(),
                        "allowedMethods" to CompactSecurityTypeArray.create(allowedMethods).toString(),
                        "disallowedReason" to disallowedReason,
                        "key" to key
                    )
                ),
                key = key,
                onResult = onPicked
            )
        }

        fun navigateToSetup(@SecurityType securityType: Int, navController: NavController) =
            navController.navigate("${SettingsState.navigationStart}/setup/$securityType")

        fun navigateToReset(@SecurityType securityType: Int, navController: NavController) =
            navController.navigate("${SettingsState.navigationStart}/reset/$securityType")

        fun navigateToLogin(@SecurityType securityType: Int, navController: NavController) {
            when (securityType) {
                SecurityActor.TYPE_PASS -> SecurityPassState.navigateToLogin(navController)
                SecurityActor.TYPE_BIO -> SecurityBioState.navigateToLogin(navController)
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