package com.example.localbeat.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localbeat.data.repository.DownloadRepositoryImpl
import com.example.localbeat.player.AudioPlayer
import com.example.localbeat.ui.components.*
import com.example.localbeat.ui.screens.*
import com.example.localbeat.ui.theme.*

@Composable
fun AppNavigation(modifier: Modifier = Modifier, player: AudioPlayer = viewModel(),
    songs: SongsViewModel = viewModel(), library: LibraryViewModel = viewModel(),
    shared: String? = null, sharedVideo: Boolean = false) {
    var selected by rememberSaveable { mutableStateOf("Home") }
    var homeStack by rememberSaveable { mutableStateOf(listOf("Home")) }
    var searchStack by rememberSaveable { mutableStateOf(listOf("Search")) }
    var libraryStack by rememberSaveable { mutableStateOf(listOf("Songs")) }
    var downloadStack by rememberSaveable { mutableStateOf(listOf("Downloads")) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var consumedShare by rememberSaveable { mutableStateOf<String?>(null) }
    val holder = rememberSaveableStateHolder()
    val stack = when (selected) { "Search" -> searchStack; "Library" -> libraryStack; "Downloads" -> downloadStack; else -> homeStack }
    val route = stack.last()
    fun setStack(value: List<String>) {
        when (selected) { "Search" -> searchStack = value; "Library" -> libraryStack = value; "Downloads" -> downloadStack = value; else -> homeStack = value }
    }
    fun navigate(target: String) {
        when (target) {
            "Player" -> showPlayer = true
            "Home", "Search", "Downloads" -> selected = target
            "Songs", "Albums", "Artists", "Playlists", "Favorites" -> { selected = "Library"; libraryStack = listOf(target) }
            else -> if (target != route) {
                if (selected == "Home") { selected = "Library"; libraryStack = listOf("Songs", target) }
                else setStack(stack + target)
            }
        }
    }
    fun back() {
        when { showPlayer -> showPlayer = false; stack.size > 1 -> setStack(stack.dropLast(1)); selected != "Home" -> selected = "Home" }
    }
    BackHandler(showPlayer || stack.size > 1 || selected != "Home") { back() }
    val context = LocalContext.current
    val downloads = remember { DownloadRepositoryImpl.get(context) }
    LaunchedEffect(shared, sharedVideo) {
        if (shared != null && shared != consumedShare) {
            consumedShare = shared; selected = "Downloads"; showPlayer = false
        }
    }
    LaunchedEffect(downloads) { downloads.libraryRevision.collect { songs.refresh() } }
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
    LaunchedEffect(scanning.songs) { if (!scanning.isLoading) library.load(scanning.songs) }
    if (showPlayer) {
        PlayerScreen(playback, player, { showPlayer = false },
            isFavorite = playback.currentSong?.id in user.favorites,
            onFavorite = { playback.currentSong?.let(library::favorite) })
    } else {
        val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        Scaffold(modifier.fillMaxSize(), containerColor = BeatColors.Background,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!keyboardVisible) Column {
                    AnimatedVisibility(playback.currentSong != null,
                        enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        MiniPlayer(playback, player::toggle, { showPlayer = true }, player::next)
                    }
                    BottomNavBar(bottomNavScreens.first { it.route == selected }, { destination ->
                        if (selected == destination.route) {
                            setStack(listOf(if (selected == "Library") "Songs" else selected))
                        } else selected = destination.route
                    })
                }
            }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
                Crossfade(selected to route, animationSpec = tween(BeatDimens.MotionMillis), label = "Destination",
                    modifier = Modifier.widthIn(max = BeatDimens.ContentWidth).fillMaxSize()) { (tab, page) ->
                    holder.SaveableStateProvider("$tab/$page") {
                        when {
                            page == "Downloads" -> DownloadsScreen(shared = shared, sharedVideo = sharedVideo)
                            !scanning.hasPermission || (scanning.isLoading && scanning.songs.isEmpty()) || scanning.error != null ->
                                SongsScreen(songsViewModel = songs, onSongClick = player::play, playbackReady = playback.connected)
                            else -> LibraryScreen(page, library, player, ::navigate, onBack = if (stack.size > 1) ({ back() }) else null,
                                refresh = songs::refresh)
                        }
                    }
                }
            }
        }
    }
}
