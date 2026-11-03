package com.example.localbeat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.localbeat.player.AudioPlayer
import com.example.localbeat.player.PlaybackState
import com.example.localbeat.ui.components.*
import com.example.localbeat.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(state: PlaybackState, player: AudioPlayer, onBackClick: () -> Unit,
    modifier: Modifier = Modifier, isFavorite: Boolean = false, onFavorite: () -> Unit = {}) {
    var showQueue by rememberSaveable { mutableStateOf(false) }
    val song = state.currentSong
    var dragging by remember(song?.id) { mutableStateOf<Float?>(null) }
    var pendingSeek by remember(song?.id) { mutableStateOf<Long?>(null) }
    LaunchedEffect(pendingSeek) { if (pendingSeek != null) { delay(700); pendingSeek = null } }
    val controls: @Composable () -> Unit = {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val playSize = (maxWidth - 192.dp).coerceIn(48.dp, 76.dp)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(song?.title ?: "Nothing playing", style = MaterialTheme.typography.headlineMedium,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(song?.artist ?: "Choose a song from your library", color = BeatColors.Muted,
                        style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                FavoriteButton(isFavorite, onFavorite, song != null)
            }
            state.error?.let { Text(it, color = BeatColors.Highlight, style = MaterialTheme.typography.bodySmall) }
            Column {
                val position = dragging ?: pendingSeek?.toFloat() ?: state.currentPosition.toFloat()
                Slider(value = position.coerceIn(0f, state.duration.coerceAtLeast(1).toFloat()),
                    onValueChange = { dragging = it },
                    onValueChangeFinished = { dragging?.let { pendingSeek = it.toLong(); player.seek(it.toLong()) }; dragging = null },
                    valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(),
                    enabled = state.duration > 0 && state.connected,
                    modifier = Modifier.testTag("playback_seek").semantics { stateDescription = formatSongDuration(position.toLong()) + " of " + formatSongDuration(state.duration) })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatSongDuration(position.toLong()), color = BeatColors.Muted, style = MaterialTheme.typography.bodySmall)
                    Text(formatSongDuration(state.duration), color = BeatColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconToggleButton(checked = state.shuffleEnabled, onCheckedChange = { player.shuffle() }, enabled = state.connected,
                    modifier = Modifier.semantics { stateDescription = if (state.shuffleEnabled) "Shuffle on" else "Shuffle off" }) {
                    Icon(BeatIcons.Shuffle, "Shuffle", tint = if (state.shuffleEnabled) BeatColors.Highlight else BeatColors.Muted)
                }
                IconButton(onClick = player::previous, enabled = song != null && state.connected) { Icon(BeatIcons.Previous, "Previous song", Modifier.size(30.dp)) }
                PrimaryPlayButton(state.isPlaying, song != null && state.connected, playSize, player::toggle)
                IconButton(onClick = player::next, enabled = song != null && state.connected) { Icon(BeatIcons.Next, "Next song", Modifier.size(30.dp)) }
                IconButton(onClick = player::repeat, enabled = state.connected,
                    modifier = Modifier.semantics { stateDescription = when (state.repeatMode) {
                        Player.REPEAT_MODE_ALL -> "Repeat all"; Player.REPEAT_MODE_ONE -> "Repeat one"; else -> "Repeat off" } }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(BeatIcons.Repeat, "Change repeat mode", tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) BeatColors.Muted else BeatColors.Highlight)
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) Text("1", style = MaterialTheme.typography.labelSmall, color = BeatColors.Highlight)
                    }
                }
            }
            Surface(shape = MaterialTheme.shapes.medium, color = BeatColors.Surface.copy(alpha = .65f)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(BeatIcons.Album, null, tint = BeatColors.Muted)
                    Column(Modifier.weight(1f)) {
                        Text("From your device", style = MaterialTheme.typography.labelSmall, color = BeatColors.Muted)
                        Text(song?.album?.ifBlank { "Local library" } ?: "Local library", style = MaterialTheme.typography.titleSmall,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { showQueue = true }) { Icon(BeatIcons.Queue, "Open playback queue") }
                }
            }
        }
    }
    }
    BoxWithConstraints(modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(BeatColors.Wine, BeatColors.Elevated, BeatColors.Background))).safeDrawingPadding()) {
        val wide = maxWidth >= 720.dp
        val portraitArtworkSize = (maxHeight * .38f).coerceIn(176.dp, 360.dp)
        Column(Modifier.align(Alignment.TopCenter).widthIn(max = 1000.dp).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) { Icon(BeatIcons.Down, "Collapse Now Playing") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall)
                    Text("LocalBeat", color = BeatColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { showQueue = true }) { Icon(BeatIcons.Queue, "Playback queue") }
            }
            if (wide) Row(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
                SongArtwork(song?.albumArtUri, Modifier.weight(1f).aspectRatio(1f))
                Box(Modifier.weight(1f)) { controls() }
            } else Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp).padding(top = 14.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
                SongArtwork(song?.albumArtUri, Modifier.widthIn(max = portraitArtworkSize)
                    .fillMaxWidth().aspectRatio(1f))
                Box(Modifier.widthIn(max = 440.dp).fillMaxWidth()) { controls() }
            }
        }
    }
    if (showQueue) ModalBottomSheet(onDismissRequest = { showQueue = false }, containerColor = BeatColors.Elevated) {
        Column(Modifier.fillMaxWidth().padding(horizontal = BeatDimens.Gutter).padding(bottom = 24.dp)) {
            SectionHeader("Playback queue", action = "Done", onAction = { showQueue = false })
            Text("${trackCount(state.queue.size)} · tap a song to play", style = MaterialTheme.typography.bodySmall, color = BeatColors.Muted)
            LazyColumn(Modifier.heightIn(max = 500.dp)) {
                itemsIndexed(state.queue, key = { index, item -> "$index:${item.id}" }) { index, item ->
                    SongRow(item, { player.select(index); showQueue = false }, enabled = state.connected,
                        active = item.id == state.currentSong?.id)
                }
            }
        }
    }
}
