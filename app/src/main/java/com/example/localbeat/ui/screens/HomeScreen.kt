package com.example.localbeat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localbeat.data.model.Song
import com.example.localbeat.ui.components.SongArtwork

@Composable
fun HomeScreen(sections: List<Pair<String, List<Song>>>, ready: Boolean,
    navigate: (String) -> Unit, play: (List<Song>, Song) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        sections.forEach { (title, songs) ->
            Column {
                TextButton(onClick = { navigate(title) }) { Text(title, style = MaterialTheme.typography.titleLarge) }
                if (songs.isEmpty()) Text(if (title == "Favorites") "Favorite songs to keep them here." else "Your music will appear here as you build your library.")
                else LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(songs.take(10), key = { it.id }) { song ->
                        Card(onClick = { play(songs, song) }, enabled = ready, modifier = Modifier.width(160.dp)) {
                            SongArtwork(song.albumArtUri, Modifier.size(120.dp))
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Text("Quick access", style = MaterialTheme.typography.titleLarge)
        listOf("Songs", "Albums", "Artists", "Playlists").forEach { title ->
            TextButton(onClick = { navigate(title) }) { Text(title) }
        }
    }
}
