package com.example.localbeat.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localbeat.data.library.LocalPlaylist
import com.example.localbeat.data.model.Song
import com.example.localbeat.ui.components.*
import com.example.localbeat.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class SearchResults(val music: LibraryIndex = LibraryIndex(), val playlists: List<LocalPlaylist> = emptyList())

@Composable
fun SearchScreen(index: LibraryIndex, playlists: List<LocalPlaylist>, playlistTracks: Map<Long, List<Song>>,
    navigate: (String) -> Unit, songRow: @Composable (Song, List<Song>) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val results by produceState(SearchResults(), query, index, playlists) {
        value = withContext(Dispatchers.Default) {
            val q = query.trim()
            if (q.isEmpty()) SearchResults() else SearchResults(LibraryIndex(
                index.songs.filter { it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true) },
                index.albums.filter { it.name.contains(q, true) || it.artist.contains(q, true) },
                index.artists.filter { it.name.contains(q, true) }),
                playlists.filter { it.name.contains(q, true) })
        }
    }
    val music = results.music
    Column(Modifier.fillMaxSize().padding(horizontal = BeatDimens.Gutter)) {
        Text("Search", Modifier.padding(top = 20.dp, bottom = 6.dp), style = MaterialTheme.typography.headlineLarge)
        Text("Discover what's already yours.", color = BeatColors.Muted, style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp),
            singleLine = true, shape = MaterialTheme.shapes.medium,
            placeholder = { Text("Songs, artists, albums…") },
            leadingIcon = { Icon(BeatIcons.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(BeatIcons.Close, "Clear search") } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = BeatColors.Surface,
                focusedContainerColor = BeatColors.Surface, unfocusedBorderColor = BeatColors.Border))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (query.isBlank()) {
                item { SectionHeader("Browse your music", Modifier.padding(vertical = 12.dp)) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrowseTile("Albums", BeatIcons.Album, Modifier.weight(1f)) { navigate("Albums") }
                    BrowseTile("Artists", BeatIcons.Artist, Modifier.weight(1f)) { navigate("Artists") }
                } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrowseTile("Favorites", BeatIcons.Heart, Modifier.weight(1f)) { navigate("Favorites") }
                    BrowseTile("Playlists", BeatIcons.Playlist, Modifier.weight(1f)) { navigate("Playlists") }
                } }
                item { Text("Search stays on this device. Always offline.", Modifier.padding(top = 24.dp),
                    color = BeatColors.Muted, style = MaterialTheme.typography.bodySmall) }
            } else {
                if (music.songs.isNotEmpty()) {
                    item { SectionHeader("Songs · ${music.songs.size}", Modifier.padding(top = 8.dp)) }
                    items(music.songs, key = { "song:${it.id}" }) { song -> songRow(song, music.songs) }
                }
                if (music.albums.isNotEmpty()) {
                    item { SectionHeader("Albums · ${music.albums.size}", Modifier.padding(top = 20.dp)) }
                    items(music.albums, key = { "album:${it.id}" }) { album ->
                        ListItem(headlineContent = { Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text("${album.artist} · ${trackCount(album.songs.size)}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = { SongArtwork(album.songs.firstOrNull()?.albumArtUri, Modifier.size(56.dp)) },
                            modifier = Modifier.clickable { focus.clearFocus(); navigate("album:${album.id}") },
                            colors = ListItemDefaults.colors(containerColor = BeatColors.Background))
                    }
                }
                if (music.artists.isNotEmpty()) {
                    item { SectionHeader("Artists · ${music.artists.size}", Modifier.padding(top = 20.dp)) }
                    items(music.artists, key = { "artist:${it.id}" }) { artist -> ArtistRow(artist) { focus.clearFocus(); navigate("artist:${artist.id}") } }
                }
                if (results.playlists.isNotEmpty()) {
                    item { SectionHeader("Playlists · ${results.playlists.size}", Modifier.padding(top = 20.dp)) }
                    items(results.playlists, key = { "playlist:${it.id}" }) { playlist ->
                        ListItem(headlineContent = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(trackCount(playlistTracks[playlist.id].orEmpty().size)) },
                            leadingContent = { PlaylistCover(playlistTracks[playlist.id].orEmpty(), Modifier.size(56.dp)) },
                            modifier = Modifier.clickable { focus.clearFocus(); navigate("playlist:${playlist.id}") },
                            colors = ListItemDefaults.colors(containerColor = BeatColors.Background))
                    }
                }
                if (music.songs.isEmpty() && music.albums.isEmpty() && music.artists.isEmpty() && results.playlists.isEmpty())
                    item { EmptyState("No matches yet", "Try another title, artist, album, or playlist name.", BeatIcons.Search) }
            }
        }
    }
}
