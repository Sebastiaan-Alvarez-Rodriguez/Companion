package org.python.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import org.python.companion.ui.components.CompanionScreen
import org.python.companion.ui.components.CompanionTabRow
import org.python.companion.ui.theme.CompanionTheme


// Basic UI: https://developer.android.com/jetpack/compose/tutorial
// Navigation: https://developer.android.com/codelabs/jetpack-compose-navigation?continue=https%3A%2F%2Fdeveloper.android.com%2Fcourses%2Fpathways%2Fcompose%23codelab-https%3A%2F%2Fdeveloper.android.com%2Fcodelabs%2Fjetpack-compose-navigation#0
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompanionTheme {
                val allScreens = CompanionScreen.values().toList()
                val navController = rememberNavController()
                val backstackEntry = navController.currentBackStackEntryAsState()
                val currentScreen = CompanionScreen.fromRoute(backstackEntry.value?.destination?.route)

                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Scaffold(
                        topBar = {
                            CompanionTabRow(
                                allScreens = allScreens,
                                onTabSelected = { screen -> navController.navigate(screen.name) },
                                currentScreen = currentScreen
                            )
                        }
                    ) { innerPadding ->
                        CompanionNavHost(navController, modifier = Modifier.padding(innerPadding))
                    }

                }
            }
        }
    }
}


@Composable
fun CompanionNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = CompanionScreen.Cactus.name,
        modifier = modifier
    ) {
        composable(CompanionScreen.Cactus.name) { // Overview
            OverviewBody(
                onClickSeeAllAccounts = { navController.navigate(Accounts.name) },
                onClickSeeAllBills = { navController.navigate(Bills.name) },
                onAccountClick = { name ->
                    navigateToSingleAccount(navController, name)
                },
            )
        }
        composable(CompanionScreen.Note.name) { // Accounts
            AccountsBody(accounts = UserData.accounts) { name ->
                navigateToSingleAccount(navController = navController, accountName = name)
            }
        }
        composable(CompanionScreen.Settings.name) { // Bills
            BillsBody(bills = UserData.bills)
        }
        val accountsName = Accounts.name
        composable(
            route = "$accountsName/{name}",
            arguments = listOf(
                navArgument("name") {
                    type = NavType.StringType
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "rally://$accountsName/{name}"
                }
            ),
        ) { entry ->
            val accountName = entry.arguments?.getString("name")
            val account = UserData.getAccount(accountName)
            SingleAccountBody(account = account)
        }
    }
}

private fun navigateToSingleAccount(navController: NavHostController, accountName: String) {
    navController.navigate("${Accounts.name}/$accountName")
}



@Composable
fun ContentScreen() {
    Greeting("Test")
}

@Composable
fun Greeting(name: String) {
    Column {
        Text(text = "Hello $name!")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CompanionTheme {
        Greeting("Android")
    }
}