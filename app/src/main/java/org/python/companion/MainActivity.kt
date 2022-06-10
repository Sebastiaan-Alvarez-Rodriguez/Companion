package org.python.companion

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.python.companion.support.UiUtil
import org.python.companion.ui.note.NoteState
import org.python.companion.ui.note.category.NoteCategoryState
import org.python.companion.ui.security.SecurityState
import org.python.companion.ui.splash.SplashBuilder
import org.python.companion.ui.theme.CompanionTheme
import org.python.companion.viewmodels.NoteCategoryViewModel
import org.python.companion.viewmodels.NoteViewModel
import org.python.companion.viewmodels.SecurityViewModel


class MainActivity : FragmentActivity() {
    private val noteViewModel by viewModels<NoteViewModel>()
    private val noteCategoryViewModel by viewModels<NoteCategoryViewModel>()

    private val securityViewModel by viewModels<SecurityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompanionTheme {
                val navController = rememberNavController()
                val scaffoldState = rememberScaffoldState()

                val utilState = UiUtil.UIUtilState.rememberState(navController = navController)

                val securityState = SecurityState.rememberState(
                    activity = this,
                    navController = navController,
                    securityViewModel = securityViewModel,
                    noteViewModel = noteViewModel,
                    scaffoldState = scaffoldState
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

                Scaffold(scaffoldState = scaffoldState) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash_screen",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("splash_screen") {
                            val splashScreenFunc = remember {
                                SplashBuilder(navController = navController, destination = NoteState.noteDestination).build {
                                    securityState.load(this@MainActivity)
                                    noteState.load()
                                    noteCategoryState.load()
                                }
                            }
                            splashScreenFunc()
                        }
                        with(utilState) { utilGraph() }
                        with(noteState) { noteGraph() }
                        with(noteCategoryState) { categoryGraph() }
                        with(securityState) { securityGraph() }
                    }
                }
            }
        }
    }
}