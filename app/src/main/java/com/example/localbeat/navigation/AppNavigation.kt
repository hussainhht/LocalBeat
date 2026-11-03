package com.example.localbeat.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.localbeat.data.repository.DownloadRepository
import com.example.localbeat.data.repository.DownloadRepositoryImpl
import com.example.localbeat.player.AudioPlayer
import com.example.localbeat.player.AudioPlayerImpl
import com.example.localbeat.ui.components.BottomNavBar
import com.example.localbeat.ui.components.MiniPlayer
import com.example.localbeat.ui.screens.DownloadsScreen
import com.example.localbeat.ui.screens.HomeScreen
import com.example.localbeat.ui.screens.PlayerScreen
import com.example.localbeat.ui.screens.SongsScreen
import kotlinx.coroutines.launch

/**
 * Root navigation component that manages bottom navigation,
 * screen switching, and the existing download and player placeholders.
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    downloadRepository: DownloadRepository = remember { DownloadRepositoryImpl() },
    player: AudioPlayer = remember { AudioPlayerImpl() },
) {
    var currentScreen by rememberSaveable { mutableStateOf<String>(Screen.Home.route) }
    var previousScreen by rememberSaveable { mutableStateOf<String>(Screen.Home.route) }

    val coroutineScope = rememberCoroutineScope()

    val downloads by downloadRepository.getDownloads().collectAsState(initial = emptyList())
    val currentSong by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()

    fun navigateTo(screen: Screen) {
        if (currentScreen != screen.route) {
            previousScreen = currentScreen
            currentScreen = screen.route
        }
    }

    BackHandler(enabled = currentScreen == Screen.Player.route) {
        currentScreen = previousScreen
    }

    val activeScreen = when (currentScreen) {
        Screen.Songs.route -> Screen.Songs
        Screen.Downloads.route -> Screen.Downloads
        Screen.Player.route -> Screen.Player
        else -> Screen.Home
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (activeScreen != Screen.Player) {
                Column {
                    if (currentSong != null) {
                        MiniPlayer(
                            song = currentSong,
                            isPlaying = isPlaying,
                            onPlayPauseClick = {
                                if (isPlaying) player.pause() else player.resume()
                            },
                            onClick = { navigateTo(Screen.Player) },
                        )
                    }
                    BottomNavBar(
                        currentScreen = activeScreen,
                        onScreenSelected = { screen -> navigateTo(screen) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (activeScreen) {
            Screen.Home -> HomeScreen(
                onNavigateToSongs = { navigateTo(Screen.Songs) },
                onNavigateToDownloads = { navigateTo(Screen.Downloads) },
                onNavigateToPlayer = { navigateTo(Screen.Player) },
                modifier = Modifier.padding(innerPadding),
            )
            Screen.Songs -> SongsScreen(
                modifier = Modifier.padding(innerPadding),
            )
            Screen.Downloads -> DownloadsScreen(
                downloads = downloads,
                onStartDownload = { url ->
                    coroutineScope.launch {
                        downloadRepository.startDownload(url = url, title = "")
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
            Screen.Player -> PlayerScreen(
                song = currentSong,
                isPlaying = isPlaying,
                onPlayPauseClick = {
                    if (isPlaying) player.pause() else player.resume()
                },
                onBackClick = { currentScreen = previousScreen },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
