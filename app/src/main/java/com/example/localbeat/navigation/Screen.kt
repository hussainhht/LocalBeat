package com.example.localbeat.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Destinations in the application.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home,
    )

    data object Songs : Screen(
        route = "songs",
        title = "Songs",
        icon = Icons.AutoMirrored.Filled.List,
    )

    data object Downloads : Screen(
        route = "downloads",
        title = "Downloads",
        icon = Icons.Default.KeyboardArrowDown,
    )

    data object Player : Screen(
        route = "player",
        title = "Now Playing",
    )
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Songs,
    Screen.Downloads,
)
