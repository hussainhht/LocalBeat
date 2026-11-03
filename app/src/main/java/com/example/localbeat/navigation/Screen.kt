package com.example.localbeat.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.localbeat.ui.components.BeatIcons

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("Home", "Home", BeatIcons.Home)
    data object Search : Screen("Search", "Search", BeatIcons.Search)
    data object Library : Screen("Library", "Library", BeatIcons.Library)
    data object Downloads : Screen("Downloads", "Downloads", BeatIcons.Download)
}

val bottomNavScreens = listOf(Screen.Home, Screen.Search, Screen.Library, Screen.Downloads)
