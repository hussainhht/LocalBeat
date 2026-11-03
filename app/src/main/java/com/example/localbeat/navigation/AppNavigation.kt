package com.example.localbeat.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localbeat.player.AudioPlayer
import com.example.localbeat.ui.components.MiniPlayer
import com.example.localbeat.ui.screens.*

@Composable
fun AppNavigation(modifier: Modifier = Modifier, player: AudioPlayer = viewModel(),
    songs: SongsViewModel = viewModel(), library: LibraryViewModel = viewModel(),
    shared: String? = null, sharedVideo: Boolean = false) {
    var stack by rememberSaveable { mutableStateOf(listOf("Home")) }
    val route = stack.last()
    val context = androidx.compose.ui.platform.LocalContext.current
    val downloads = remember { com.example.localbeat.data.repository.DownloadRepositoryImpl.get(context) }
    LaunchedEffect(shared) { if (shared != null) stack = listOf("Downloads") }
    LaunchedEffect(downloads) { downloads.libraryRevision.collect { songs.refresh() } }
    fun navigate(target: String) { if (target != route) stack = stack + target }
    fun back() { if (stack.size > 1) stack = stack.dropLast(1) }
    BackHandler(stack.size > 1) { back() }
    val scanning by songs.uiState.collectAsStateWithLifecycle()
    val playback by player.state.collectAsStateWithLifecycle()
    val user by library.user.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) songs.refresh() }
        owner.lifecycle.addObserver(observer)
        songs.refresh()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(scanning.songs) { library.load(scanning.songs) }
    Scaffold(modifier.fillMaxSize(), bottomBar = {
        if (route != "Player") Column {
            MiniPlayer(playback, player::toggle, { navigate("Player") })
            NavigationBar {
                listOf("Home", "Songs", "Search", "Favorites").forEach { destination ->
                    NavigationBarItem(selected = route == destination, onClick = { stack = listOf(destination) },
                        icon = { Text(destination.take(1)) }, label = { Text(destination) })
                }
            }
        }
    }) { padding ->
        Column(Modifier.padding(padding)) {
            if (route != "Player") Row {
                if (stack.size > 1) TextButton(onClick = { back() }) { Text("Back") }
                TextButton(onClick = songs::refresh) { Text("Refresh library") }
                TextButton(onClick = { navigate("Downloads") }) { Text("Downloads") }
            }
            when {
                route == "Downloads" -> DownloadsScreen(shared = shared, sharedVideo = sharedVideo)
                route == "Player" -> PlayerScreen(playback, player, { back() },
                    isFavorite = playback.currentSong?.id in user.favorites,
                    onFavorite = { playback.currentSong?.let(library::favorite) })
                !scanning.hasPermission || scanning.isLoading || scanning.error != null ->
                    SongsScreen(songsViewModel = songs, onSongClick = player::play, playbackReady = playback.connected)
                else -> LibraryScreen(route, library, player, ::navigate)
            }
        }
    }
}
