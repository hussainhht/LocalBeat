package com.example.localbeat.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.localbeat.data.model.Song
import com.example.localbeat.ui.screens.AlbumGroup
import com.example.localbeat.ui.screens.ArtistGroup
import com.example.localbeat.ui.screens.formatSongDuration
import com.example.localbeat.ui.theme.*

fun trackCount(count: Int): String = "$count " + if (count == 1) "song" else "songs"
fun albumCount(count: Int): String = "$count " + if (count == 1) "album" else "albums"

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun EmptyState(title: String, detail: String, icon: ImageVector = BeatIcons.Music,
    action: String? = null, onAction: () -> Unit = {}, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(76.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(BeatColors.Wine, BeatColors.Surface))), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(32.dp), tint = BeatColors.Highlight)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = BeatColors.Muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null) Button(onClick = onAction) { Text(action) }
    }
}

@Composable
fun LoadingState(text: String = "Loading your music…") {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
        Text(text, color = BeatColors.Muted)
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Surface(color = BeatColors.Wine, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRetry) { Text("Try again") }
        }
    }
}

@Composable
fun PrimaryPlayButton(playing: Boolean = false, enabled: Boolean = true, size: Dp = 64.dp,
    onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .94f else 1f, label = "Play press")
    FilledIconButton(onClick = onClick, enabled = enabled, interactionSource = interaction,
        modifier = Modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale },
        shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (pressed) BeatColors.Pressed else BeatColors.Crimson,
            contentColor = BeatColors.Ink)) {
        Crossfade(playing, label = "Play pause") { active ->
            Icon(if (active) BeatIcons.Pause else BeatIcons.Play, if (active) "Pause" else "Play", Modifier.size(size * .46f))
        }
    }
}

@Composable
fun FavoriteButton(favorite: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    val tint by animateColorAsState(if (favorite) BeatColors.Highlight else BeatColors.Muted, label = "Favorite color")
    val scale by animateFloatAsState(if (favorite) 1.08f else 1f, label = "Favorite feedback")
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(BeatDimens.TouchTarget)
        .semantics { stateDescription = if (favorite) "In favorites" else "Not in favorites" }) {
        Icon(if (favorite) BeatIcons.HeartFilled else BeatIcons.Heart,
            if (favorite) "Remove from favorites" else "Add to favorites",
            Modifier.size(22.dp).graphicsLayer { scaleX = scale; scaleY = scale }, tint = tint)
    }
}

@Composable
fun SongRow(song: Song, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    active: Boolean = false, favorite: Boolean? = null, onFavorite: () -> Unit = {},
    onOptions: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(if (active) BeatColors.Wine.copy(alpha = .4f) else androidx.compose.ui.graphics.Color.Transparent)
        .clickable(enabled = enabled, onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SongArtwork(song.albumArtUri, Modifier.size(BeatDimens.RowArtwork))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium, color = if (active) BeatColors.Highlight else BeatColors.Text)
            Text(song.artist + " · " + formatSongDuration(song.durationMs), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall, color = BeatColors.Muted)
        }
        if (favorite != null) FavoriteButton(favorite, onFavorite)
        if (onOptions != null) IconButton(onClick = onOptions, modifier = Modifier.size(BeatDimens.TouchTarget)) {
            Icon(BeatIcons.More, "Options for ${song.title}", tint = BeatColors.Muted)
        }
        trailing?.invoke()
    }
}

@Composable
fun MusicCard(song: Song, onClick: () -> Unit, enabled: Boolean = true) {
    Column(Modifier.width(154.dp).clip(MaterialTheme.shapes.medium).clickable(enabled = enabled, onClick = onClick)
        .padding(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SongArtwork(song.albumArtUri, Modifier.fillMaxWidth().aspectRatio(1f))
        Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.artist, color = BeatColors.Muted, style = MaterialTheme.typography.bodySmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun AlbumCard(album: AlbumGroup, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SongArtwork(album.songs.firstOrNull()?.albumArtUri, Modifier.fillMaxWidth().aspectRatio(1f))
        Text(album.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("${album.artist} · ${trackCount(album.songs.size)}", style = MaterialTheme.typography.bodySmall,
            color = BeatColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ArtistRow(artist: ArtistGroup, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SongArtwork(null, Modifier.size(64.dp), CoverKind.Artist)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(artist.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${trackCount(artist.songs.size)} · ${artist.albums.size} ${if (artist.albums.size == 1) "album" else "albums"}", color = BeatColors.Muted,
                style = MaterialTheme.typography.bodySmall)
        }
        Icon(BeatIcons.Back, null, Modifier.size(16.dp).graphicsLayer { rotationZ = 180f }, tint = BeatColors.Muted)
    }
}

@Composable
fun PlaylistCard(name: String, songs: List<Song>, onClick: () -> Unit, modifier: Modifier = Modifier,
    onOptions: (() -> Unit)? = null) {
    Column(modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PlaylistCover(songs, Modifier.fillMaxWidth().aspectRatio(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(trackCount(songs.size), color = BeatColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
            if (onOptions != null) IconButton(onClick = onOptions) { Icon(BeatIcons.More, "Options for $name", tint = BeatColors.Muted) }
        }
    }
}

data class SheetAction(val label: String, val icon: ImageVector, val enabled: Boolean = true, val action: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsSheet(title: String, subtitle: String? = null, artwork: String? = null,
    actions: List<SheetAction>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = BeatColors.Elevated) {
        Column(Modifier.fillMaxWidth().heightIn(max = 620.dp).verticalScrollCompat()
            .padding(horizontal = BeatDimens.Gutter).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SongArtwork(artwork, Modifier.size(64.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BeatColors.Muted)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp), color = BeatColors.Border)
            actions.forEach { action ->
                ListItem(headlineContent = { Text(action.label) }, leadingContent = { Icon(action.icon, null) },
                    colors = ListItemDefaults.colors(containerColor = BeatColors.Elevated),
                    modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(enabled = action.enabled) { onDismiss(); action.action() })
            }
        }
    }
}

@Composable
private fun Modifier.verticalScrollCompat(): Modifier =
    this.then(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()))
