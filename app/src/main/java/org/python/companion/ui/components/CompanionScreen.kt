package org.python.companion.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector

enum class CompanionScreen (
    val icon: ImageVector,
) {
    Note(
        icon = Icons.Filled.PieChart,
    ),
    Cactus(
        icon = Icons.Filled.AttachMoney,
    ),
    Settings(
        icon = Icons.Filled.MoneyOff,
    );

    companion object {
        fun fromRoute(route: String?): CompanionScreen =
            when (route?.substringBefore("/")) {
                Note.name -> Note
                Cactus.name -> Cactus
                Settings.name -> Settings
                null -> Note
                else -> throw IllegalArgumentException("Route $route is not recognized.")
            }
    }
}