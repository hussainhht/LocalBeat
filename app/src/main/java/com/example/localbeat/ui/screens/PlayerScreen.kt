package com.example.localbeat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.localbeat.player.AudioPlayer
import com.example.localbeat.player.PlaybackState
import com.example.localbeat.ui.components.SongArtwork

@Composable
fun PlayerScreen(state: PlaybackState, player: AudioPlayer, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    var showQueue by remember { mutableStateOf(false) }
    var dragging by remember(state.currentSong?.id) { mutableStateOf<Float?>(null) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBackClick) { Text("Back") }
            Text("Now Playing", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { showQueue = true }) { Text("Queue") }
        }
        SongArtwork(state.currentSong?.albumArtUri,
            Modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f))
        Text(state.currentSong?.title ?: "No song selected", style = MaterialTheme.typography.headlineSmall)
        Text(state.currentSong?.artist.orEmpty())
        Text(state.currentSong?.album.orEmpty(), style = MaterialTheme.typography.bodySmall)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Slider(
            value = dragging ?: state.currentPosition.toFloat().coerceIn(0f, state.duration.coerceAtLeast(1).toFloat()),
            onValueChange = { dragging = it },
            onValueChangeFinished = { dragging?.let { player.seek(it.toLong()) }; dragging = null },
            valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(),
            enabled = state.duration > 0 && state.connected,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatSongDuration(dragging?.toLong() ?: state.currentPosition))
            Text(formatSongDuration(state.duration))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = player::previous, enabled = state.currentSong != null) { Text("Previous") }
            Button(onClick = player::toggle, enabled = state.currentSong != null) {
                Text(if (state.isPlaying) "Pause" else "Play")
            }
            TextButton(onClick = player::next, enabled = state.currentSong != null) { Text("Next") }
        }
        Row {
            FilterChip(selected = state.shuffleEnabled, onClick = player::shuffle, label = { Text("Shuffle") })
            Spacer(Modifier.width(16.dp))
            TextButton(onClick = player::repeat) {
                Text("Repeat: " + when (state.repeatMode) {
                    Player.REPEAT_MODE_ALL -> "All"
                    Player.REPEAT_MODE_ONE -> "One"
                    else -> "Off"
                })
            }
        }
    }
    if (showQueue) {
        AlertDialog(onDismissRequest = { showQueue = false },
            title = { Text("Playback queue") },
            text = {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    itemsIndexed(state.queue) { index, song ->
                        TextButton(onClick = { player.select(index); showQueue = false }) {
                            Text((if (song.id == state.currentSong?.id) "▶ " else "") + song.title + " — " + song.artist)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQueue = false }) { Text("Close") } })
    }
}
