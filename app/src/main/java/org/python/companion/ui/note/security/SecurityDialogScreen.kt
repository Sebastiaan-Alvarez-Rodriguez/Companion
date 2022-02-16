package org.python.companion.ui.note.security

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation

// TODO: Make dialogs transition through navigation


class SecurityState(private val navController: NavHostController,) {
    fun NavGraphBuilder.securityGraph() {
        navigation(startDestination = "securityDialog", route = "sec") {
            composable("pick") {

            }
        }
    }
}