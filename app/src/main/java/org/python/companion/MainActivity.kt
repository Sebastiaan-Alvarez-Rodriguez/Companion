package org.python.companion

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import org.python.backend.data.datatype.Anniversary
import org.python.companion.support.UiUtil
import org.python.companion.ui.anniversary.AnniversaryBody
import org.python.companion.ui.cactus.CactusBody
import org.python.companion.ui.components.CompanionScreen
import org.python.companion.ui.components.CompanionTabRow
import org.python.companion.ui.note.NoteState
import org.python.companion.ui.note.category.NoteCategoryState
import org.python.companion.ui.security.SecurityState
import org.python.companion.ui.splash.SplashBuilder
import org.python.companion.ui.theme.CompanionTheme
import org.python.companion.viewmodels.AnniversaryViewModel
import org.python.companion.viewmodels.NoteCategoryViewModel
import org.python.companion.viewmodels.NoteViewModel
import org.python.companion.viewmodels.SecurityViewModel
import timber.log.Timber


class MainActivity : FragmentActivity() {
    private val noteViewModel by viewModels<NoteViewModel>()
    private val noteCategoryViewModel by viewModels<NoteCategoryViewModel>()

    private val anniversaryViewModel by viewModels<AnniversaryViewModel>()
    private val securityViewModel by viewModels<SecurityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompanionTheme {
                val allScreens = CompanionScreen.values().toList()
                val navController = rememberNavController()
                val scaffoldState = rememberScaffoldState()

                val backstackEntry = navController.currentBackStackEntryAsState()
                val selectedTabScreen = CompanionScreen.fromRoute(backstackEntry.value?.destination?.route)

                val utilState = UiUtil.UIUtilState.rememberState(navController = navController)

                val securityState = SecurityState.rememberState(
                    activity = this,
                    navController = navController,
                    securityViewModel = securityViewModel,
                    noteViewModel = noteViewModel
                )
                val noteState = NoteState.rememberState(
                    navController = navController,
                    noteViewModel = noteViewModel,
                    scaffoldState = scaffoldState
                )
                val noteCategoryState = NoteCategoryState.rememberState(
                    navController = navController,
                    noteCategoryViewModel = noteCategoryViewModel,
                    scaffoldState = scaffoldState
                )
                val cactusState = CactusState.rememberState(navController = navController)
                val anniversaryState = AnniversaryState.rememberState(
                    navController = navController,
                    anniversaryViewModel = anniversaryViewModel
                )

                Scaffold(
                    topBar = {
                        CompanionTabRow(
                            allScreens = allScreens,
                            onTabSelected = { screen -> navController.navigate(screen.name) { launchSingleTop = true } },
                            currentScreen = selectedTabScreen
                        )
                    },
                    scaffoldState = scaffoldState
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash_screen",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("splash_screen") {
                            val splashScreenFunc = remember {
                                SplashBuilder(navController = navController, destination = CactusState.cactusDestination).build {
                                    noteState.load()
                                    noteCategoryState.load()
                                    anniversaryState.load()
                                }
                            }
                            splashScreenFunc()
                        }
                        with(utilState) { utilGraph() }
                        with(cactusState) { cactusGraph() }
                        with(noteState) { noteGraph() }
                        with(noteCategoryState) { categoryGraph() }
                        with(anniversaryState) { anniversaryGraph() }
                        with(securityState) { securityGraph() }
                    }
                }
            }
        }
    }
}

class CactusState(private val navController: NavHostController) {
    fun NavGraphBuilder.cactusGraph() {
        navigation(startDestination = cactusDestination, route = "cactus") {
            composable(cactusDestination) { // Overview
                CactusBody(onCactusClick = { })
            }
        }
    }

    companion object {
        val cactusDestination = CompanionScreen.Cactus.name
        @Composable
        fun rememberState(navController: NavHostController = rememberNavController()) = remember(navController) {
            CactusState(navController)
        }
    }
}

class AnniversaryState(private val navController: NavHostController, private val anniversaryViewModel: AnniversaryViewModel) {
    fun load() {
        anniversaryViewModel.load()
    }

    fun NavGraphBuilder.anniversaryGraph() {
        navigation(startDestination = anniversaryDestination, route = "anniversary") {
            composable(anniversaryDestination) { // Overview
                val anniversaries by anniversaryViewModel.anniversaries.collectAsState()

                AnniversaryBody(anniversaryList = anniversaries,
                    onNewClick = { navigateToCreateAnniversary(navController) },
                    onAnniversaryClick = {anniversary -> navigateToSingleAnniversary(navController = navController, anniversary = anniversary) },
                    onFavoriteClick = {anniversary -> })
            }
            composable(
                route = "$anniversaryDestination/create",
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = "companion://$anniversaryDestination/create"
                    }
                )
            ) {
                Timber.d("Creating a new anniversary")
                // TODO: Implement anniversary creation
            }
        }
    }

    private fun navigateToCreateAnniversary(navController: NavHostController) = navController.navigate("${CompanionScreen.Anniversary.name}/create")
    private fun navigateToSingleAnniversary(navController: NavHostController, anniversary: Anniversary) = navigateToSingleAnniversary(navController, anniversary.name)
    private fun navigateToSingleAnniversary(navController: NavHostController, anniversary: String) = navController.navigate("${CompanionScreen.Anniversary.name}/${anniversary}")


    companion object {
        val anniversaryDestination = CompanionScreen.Anniversary.name

        @Composable
        fun rememberState(navController: NavHostController = rememberNavController(), anniversaryViewModel: AnniversaryViewModel)
        = remember(navController) { AnniversaryState(navController, anniversaryViewModel) }
    }
}