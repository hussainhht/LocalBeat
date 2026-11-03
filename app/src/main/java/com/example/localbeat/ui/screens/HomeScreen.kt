package com.example.localbeat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localbeat.data.library.LocalPlaylist
import com.example.localbeat.data.model.Song
import com.example.localbeat.ui.components.*
import com.example.localbeat.ui.theme.*
import java.util.Calendar

@Composable
fun HomeScreen(sections: List<Pair<String, List<Song>>>, ready: Boolean,
    navigate: (String) -> Unit, play: (List<Song>, Song) -> Unit,
    playlists: List<Pair<LocalPlaylist, List<Song>>> = emptyList(), songCount: Int = 0) {
    val favorites = sections.firstOrNull { it.first == "Favorites" }?.second.orEmpty()
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    LazyColumn(contentPadding = PaddingValues(bottom = BeatDimens.SectionGap),
        verticalArrangement = Arrangement.spacedBy(BeatDimens.SectionGap)) {
        item {
            Column(Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(BeatColors.Wine.copy(alpha = .55f), BeatColors.Background)))
                .padding(horizontal = BeatDimens.Gutter).padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SongArtwork(null, Modifier.size(36.dp))
                    Text("LocalBeat", Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { navigate("Search") }) { Icon(BeatIcons.Search, "Search your music") }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("$greeting.", style = MaterialTheme.typography.headlineLarge)
                    Text("Your music. Your rhythm.", color = BeatColors.Muted)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All music" to "Songs", "Favorites" to "Favorites", "Playlists" to "Playlists", "Recently added" to "Recently Added")) { (label, route) ->
                        FilterChip(selected = route == "Songs", onClick = { navigate(route) }, label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BeatColors.Crimson,
                                selectedLabelColor = BeatColors.Ink, containerColor = BeatColors.Surface),
                            border = null)
                    }
                }
            }
        }
        item {
            Surface(onClick = { navigate("Favorites") }, modifier = Modifier.padding(horizontal = BeatDimens.Gutter).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp), color = BeatColors.Surface, contentColor = BeatColors.Text) {
                Column(Modifier.background(Brush.linearGradient(listOf(BeatColors.Wine, BeatColors.Surface)))
                    .padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("MADE FOR YOUR LIBRARY", style = MaterialTheme.typography.labelSmall, color = BeatColors.Highlight)
                            Text("Always on repeat.", style = MaterialTheme.typography.headlineSmall)
                        }
                        PlaylistCover(favorites, Modifier.size(if (LocalDensity.current.fontScale > 1.2f) 56.dp else 84.dp), favorite = true)
                    }
                    Text(if (favorites.isEmpty()) "Save the songs you love. Find them here."
                        else "${favorites.size} ${if (favorites.size == 1) "favorite" else "favorites"}, all yours.",
                        style = MaterialTheme.typography.bodyMedium, color = BeatColors.Muted)
                    Text("Your favorites  →", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (songCount == 0) item {
            EmptyState("Your next favorite is already yours", "Add music to this device or import audio to start your library.",
                action = "Import audio", onAction = { navigate("Downloads") })
        } else {
            sections.forEach { (title, songs) ->
                if (songs.isNotEmpty()) item(key = "music:$title") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(title, Modifier.padding(horizontal = BeatDimens.Gutter), "See all") { navigate(title) }
                        LazyRow(contentPadding = PaddingValues(horizontal = BeatDimens.Gutter - 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(songs.take(12), key = { it.id }) { song -> MusicCard(song, { play(songs, song) }, ready) }
                        }
                    }
                }
            }
            if (sections.firstOrNull { it.first == "Recently Played" }?.second.isNullOrEmpty()) item {
                Surface(Modifier.padding(horizontal = BeatDimens.Gutter), shape = MaterialTheme.shapes.medium, color = BeatColors.Elevated) {
                    Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(BeatIcons.Clock, null, tint = BeatColors.Highlight)
                        Column { Text("Make yourself at home", style = MaterialTheme.typography.titleMedium)
                            Text("Your listening history will appear as you play.", color = BeatColors.Muted, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("Your playlists", Modifier.padding(horizontal = BeatDimens.Gutter), "View all") { navigate("Playlists") }
                if (playlists.isEmpty()) Surface(onClick = { navigate("Playlists") },
                    modifier = Modifier.padding(horizontal = BeatDimens.Gutter).fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium, color = BeatColors.Elevated) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(BeatIcons.Plus, null, tint = BeatColors.Highlight)
                        Column { Text("A mix for every mood", style = MaterialTheme.typography.titleMedium)
                            Text("Create your first playlist", color = BeatColors.Muted, style = MaterialTheme.typography.bodySmall) }
                    }
                } else LazyRow(contentPadding = PaddingValues(horizontal = BeatDimens.Gutter),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(playlists.take(10), key = { it.first.id }) { (playlist, tracks) ->
                        PlaylistCard(playlist.name, tracks, { navigate("playlist:${playlist.id}") }, Modifier.width(156.dp))
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = BeatDimens.Gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Quick access")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrowseTile("Albums", BeatIcons.Album, Modifier.weight(1f)) { navigate("Albums") }
                    BrowseTile("Artists", BeatIcons.Artist, Modifier.weight(1f)) { navigate("Artists") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrowseTile("All songs", BeatIcons.Music, Modifier.weight(1f)) { navigate("Songs") }
                    BrowseTile("Audio imports", BeatIcons.Download, Modifier.weight(1f)) { navigate("Downloads") }
                }
                Text("${trackCount(songCount)}. No connection needed.", Modifier.padding(top = 12.dp),
                    color = BeatColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun BrowseTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clip(MaterialTheme.shapes.medium).background(BeatColors.Surface).clickable(onClick = onClick)
        .padding(18.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Icon(icon, null, tint = BeatColors.Highlight, modifier = Modifier.size(28.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
