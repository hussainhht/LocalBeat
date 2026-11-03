package com.example.localbeat.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localbeat.player.PlaybackState
import com.example.localbeat.ui.theme.*

@Composable
fun MiniPlayer(state: PlaybackState, onPlayPauseClick: () -> Unit, onClick: () -> Unit,
    onNext: () -> Unit = {}) {
    val song = state.currentSong ?: return
    Surface(Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(),
        shape = RoundedCornerShape(18.dp), color = BeatColors.Card) {
        Column(Modifier.semantics { contentDescription = "Open Now Playing" }
            .clickable(role = Role.Button, onClickLabel = "Open Now Playing", onClick = onClick)) {
            Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                SongArtwork(song.albumArtUri, Modifier.size(44.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, color = BeatColors.Muted, style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onPlayPauseClick, enabled = state.connected) {
                    Crossfade(state.isPlaying, label = "Mini player play pause") { playing ->
                        Icon(if (playing) BeatIcons.Pause else BeatIcons.Play, if (playing) "Pause" else "Play")
                    }
                }
                IconButton(onClick = onNext, enabled = state.connected && state.queue.size > 1) {
                    Icon(BeatIcons.Next, "Next song", Modifier.size(22.dp))
                }
            }
            // Compact progress has no synthetic animation or invented percentage.
            LinearProgressIndicator(
                progress = { if (state.duration > 0) (state.currentPosition.toFloat() / state.duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth().height(2.dp).clip(RoundedCornerShape(2.dp)),
                color = BeatColors.Highlight, trackColor = BeatColors.Border,
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(5.dp))
        }
    }
}
