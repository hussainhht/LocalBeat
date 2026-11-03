package com.example.localbeat.ui.screens

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localbeat.data.model.Song
import java.util.Locale

@Composable
fun SongsScreen(
    modifier: Modifier = Modifier,
    songsViewModel: SongsViewModel = viewModel(),
    onSongClick: (List<Song>, Song) -> Unit = { _, _ -> },
    playbackReady: Boolean = true,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val state by songsViewModel.uiState.collectAsStateWithLifecycle()
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    var showRationale by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permanentlyDenied = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, MusicPermission.name)
        songsViewModel.refresh()
    }

    DisposableEffect(activity, songsViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) songsViewModel.refresh()
        }
        activity?.lifecycle?.addObserver(observer)
        songsViewModel.refresh()
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Allow music access") },
            text = { Text("LocalBeat needs audio access to show the music stored on your device.") },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    permissionLauncher.launch(MusicPermission.name)
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Local Songs",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        when {
            !state.hasPermission -> {
                Text(state.error ?: if (permanentlyDenied) {
                    "Audio access is disabled. Allow music access in app settings to see your songs."
                } else {
                    "Allow audio access to discover music stored on your device."
                })
                Button(onClick = {
                    when {
                        MusicPermission.isGranted(context) -> songsViewModel.refresh()
                        permanentlyDenied -> context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)),
                        )
                        activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                            activity, MusicPermission.name,
                        ) -> showRationale = true
                        else -> permissionLauncher.launch(MusicPermission.name)
                    }
                }) { Text(if (permanentlyDenied) "Open settings" else "Allow audio access") }
            }
            state.isLoading -> {
                CircularProgressIndicator()
                Text("Scanning music library…")
            }
            state.error != null -> {
                Text(state.error!!)
                Button(onClick = songsViewModel::refresh) { Text("Retry") }
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${state.songs.size} tracks available")
                    TextButton(onClick = songsViewModel::refresh) { Text("Refresh") }
                }
                if (state.songs.isEmpty()) {
                    Text("No songs found")
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.songs, key = { it.contentUri.toString() }) { song ->
                            SongListItem(song = song, onClick = { onSongClick(state.songs, song) }, enabled = playbackReady)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongListItem(song: Song, modifier: Modifier = Modifier, onClick: () -> Unit = {}, enabled: Boolean = true) {
    Card(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(formatSongDuration(song.durationMs), style = MaterialTheme.typography.bodyMedium)
        }
    }
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
