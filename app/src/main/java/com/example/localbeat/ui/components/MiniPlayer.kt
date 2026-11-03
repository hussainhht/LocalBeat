package com.example.localbeat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localbeat.player.PlaybackState

@Composable
fun MiniPlayer(state: PlaybackState, onPlayPauseClick: () -> Unit, onClick: () -> Unit) {
    val song = state.currentSong ?: return
    Surface(tonalElevation = 4.dp) {
        Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SongArtwork(song.albumArtUri, Modifier.size(48.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                TextButton(onClick = onPlayPauseClick, enabled = state.connected) {
                    Text(if (state.isPlaying) "Pause" else "Play")
                }
            }
            LinearProgressIndicator(
                progress = { if (state.duration > 0) (state.currentPosition.toFloat() / state.duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
