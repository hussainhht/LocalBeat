package com.example.localbeat.ui.screens

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localbeat.data.model.Song
import com.example.localbeat.ui.components.*
import com.example.localbeat.ui.theme.*
import java.util.Locale

/** Permission, scan and recovery states share the same visual language as the library. */
@Composable
fun SongsScreen(modifier: Modifier = Modifier, songsViewModel: SongsViewModel = viewModel(),
    onSongClick: (List<Song>, Song) -> Unit = { _, _ -> }, playbackReady: Boolean = true) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val state by songsViewModel.uiState.collectAsStateWithLifecycle()
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    var showRationale by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permanentlyDenied = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, MusicPermission.name)
        songsViewModel.refresh()
    }
    DisposableEffect(activity, songsViewModel) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) songsViewModel.refresh() }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }
    fun requestAccess() {
        when {
            MusicPermission.isGranted(context) -> songsViewModel.refresh()
            permanentlyDenied -> context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)))
            activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, MusicPermission.name) -> showRationale = true
            else -> permissionLauncher.launch(MusicPermission.name)
        }
    }
    if (showRationale) AlertDialog(onDismissRequest = { showRationale = false },
        icon = { Icon(BeatIcons.Music, null) }, title = { Text("Bring your music in") },
        text = { Text("LocalBeat needs audio access to show music stored on your device. Your library stays local.") },
        confirmButton = { Button(onClick = { showRationale = false; permissionLauncher.launch(MusicPermission.name) }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = { showRationale = false }) { Text("Cancel") } })
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(BeatColors.Wine, BeatColors.Background)))
                .padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("LocalBeat", style = MaterialTheme.typography.titleLarge)
                SongArtwork(null, Modifier.size(124.dp).align(Alignment.CenterHorizontally))
                Text("Your music,\nyour way.", style = MaterialTheme.typography.displaySmall)
                Text("An entire world of sound. Right here on your device.", color = BeatColors.Muted)
            }
        }
        when {
            !state.hasPermission -> item {
                EmptyState("Let's find your music",
                    state.error ?: if (permanentlyDenied) "Enable audio access in settings to browse your library."
                    else "Allow audio access to discover your songs, albums and artists.",
                    BeatIcons.Library, if (permanentlyDenied) "Open settings" else "Allow audio access", ::requestAccess)
            }
            state.isLoading -> item { LoadingState("Finding your music…") }
            state.error != null -> item { Box(Modifier.padding(20.dp)) { ErrorState(state.error!!, songsViewModel::refresh) } }
            state.songs.isEmpty() -> item { EmptyState("A little quiet here", "Music saved on your device will appear in your library.") }
            else -> items(state.songs, key = { it.id }) { song ->
                SongRow(song, { onSongClick(state.songs, song) }, Modifier.padding(horizontal = 20.dp), enabled = playbackReady)
            }
        }
    }
}

@Composable
fun SongListItem(song: Song, modifier: Modifier = Modifier, onClick: () -> Unit = {}, enabled: Boolean = true) {
    SongRow(song, onClick, modifier, enabled)
}

internal fun formatSongDuration(durationMs: Long): String {
    val seconds = durationMs.coerceAtLeast(0) / 1000
    return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
}

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
