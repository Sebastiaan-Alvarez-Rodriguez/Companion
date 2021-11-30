package org.python.companion.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

enum class CompanionScreen (
    val icon: ImageVector,
) {
    Note(
        icon = Icons.Filled.Notifications,
    ),
    Cactus(
        icon = Icons.Filled.ShoppingCart,
    ),
    Anniversary(
        icon = Icons.Filled.DateRange,
    );

    companion object {
        fun fromRoute(route: String?): CompanionScreen =
            when (route?.substringBefore("/")) {
                Note.name -> Note
                Cactus.name -> Cactus
                Anniversary.name -> Anniversary
                null -> Note
                else -> throw IllegalArgumentException("Route $route is not recognized.")
            }
    }
}