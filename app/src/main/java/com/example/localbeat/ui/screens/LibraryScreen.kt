package com.example.localbeat.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.localbeat.data.model.Song
import com.example.localbeat.data.library.*
import com.example.localbeat.player.AudioPlayer
import com.example.localbeat.ui.components.SongArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LibraryScreen(route: String, vm: LibraryViewModel, player: AudioPlayer, navigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val index by vm.index.collectAsState()
    val user by vm.user.collectAsState()
    val sort by vm.sort.collectAsState()
    val reverse by vm.descending.collectAsState()
    val error by vm.error.collectAsState()
    val playback by player.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var addSong by remember { mutableStateOf<Song?>(null) }
    var editing by remember { mutableStateOf<LocalPlaylist?>(null) }
    var create by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf<LocalPlaylist?>(null) }
    val byId = remember(index) { index.songs.associateBy { it.id } }
    val favoriteSongs = remember(index, user.favorites) { index.songs.filter { it.id in user.favorites } }
    val recent = remember(index, user.stats) { user.stats.sortedByDescending { it.lastPlayed }.take(100).mapNotNull { byId[it.songId] } }
    val most = remember(index, user.stats) { user.stats.sortedByDescending { it.playCount }.mapNotNull { byId[it.songId] } }
    val added = remember(index) { index.songs.sortedByDescending { it.dateAdded }.take(100) }
    val sorted by produceState(emptyList<Song>(), index, sort, reverse) {
        value = withContext(Dispatchers.Default) { sortSongs(index.songs, sort, reverse) }
    }
    val results by produceState(LibraryIndex(), query, index) {
        value = withContext(Dispatchers.Default) {
            val q = query.trim()
            if (q.isBlank()) LibraryIndex() else LibraryIndex(
                index.songs.filter { it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true) },
                index.albums.filter { it.name.contains(q, true) || it.artist.contains(q, true) },
                index.artists.filter { it.name.contains(q, true) })
        }
    }
    val album = index.albums.find { route == "album:${it.id}" }
    val artist = index.artists.find { route == "artist:${it.id}" }
    val playlist = user.playlists.find { route == "playlist:${it.id}" }
    val tracks = when {
        album != null -> album.songs
        artist != null -> artist.songs
        playlist != null -> user.entries.filter { it.playlistId == playlist.id }.mapNotNull { byId[it.songId] }
        route == "Favorites" -> favoriteSongs
        route == "Recently Played" -> recent
        route == "Recently Added" -> added
        route == "Most Played" -> most
        route.startsWith("album:") || route.startsWith("artist:") || route.startsWith("playlist:") -> emptyList()
        else -> sorted
    }
    fun play(list: List<Song>, song: Song? = list.firstOrNull(), shuffle: Boolean = false) {
        if (song != null) player.playQueue(list, song, shuffle)
    }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            listOf("Home", "Songs", "Albums", "Artists", "Playlists", "Favorites", "Search").forEach {
                TextButton(onClick = { navigate(it) }) { Text(it) }
            }
        }
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            TextButton(onClick = vm::retry) { Text("Retry saved data") }
        }
        if (route == "Search") {
            OutlinedTextField(query, { query = it }, label = { Text("Search local music and playlists") }, modifier = Modifier.fillMaxWidth().padding(8.dp), singleLine = true)
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(album?.name ?: artist?.name ?: playlist?.name ?: if (route == "Home") "LocalBeat" else if (route.contains(":")) "Item no longer available" else route,
                style = MaterialTheme.typography.headlineMedium) }
            if (route == "Home") {
                item { HomeScreen(
                    listOf("Recently Played" to recent, "Recently Added" to added, "Favorites" to favoriteSongs, "Most Played" to most),
                    playback.connected, navigate, { list, song -> play(list, song) }) }
            } else if (route == "Albums") {
                if (index.albums.isEmpty()) item { Text("No albums found.") }
                items(index.albums, key = { it.id }) { AlbumRow(it) { navigate("album:${it.id}") } }
            } else if (route == "Artists") {
                if (index.artists.isEmpty()) item { Text("No artists found.") }
                items(index.artists, key = { it.id }) { a ->
                    TextButton(onClick = { navigate("artist:${a.id}") }) { Text("${a.name} · ${a.songs.size} songs · ${a.albums.size} albums") }
                }
            } else if (route == "Playlists") {
                item { Button(onClick = { create = true; name = "" }) { Text("Create playlist") } }
                if (user.playlists.isEmpty()) item { Text("Create your first playlist.") }
                items(user.playlists, key = { it.id }) { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { navigate("playlist:${p.id}") }, modifier = Modifier.weight(1f)) {
                            Text("${p.name} · ${user.entries.count { it.playlistId == p.id && it.songId in byId }} songs")
                        }
                        TextButton(onClick = { editing = p; name = p.name }) { Text("Rename") }
                        TextButton(onClick = { deleting = p }) { Text("Delete") }
                    }
                }
            } else if (route == "Search") {
                if (query.isBlank()) item { Text("Search your on-device library.") }
                else {
                    item { Text("Songs", style = MaterialTheme.typography.titleLarge) }
                    items(results.songs, key = { "song:" + it.id }) { song ->
                        LibrarySongRow(song, song.id in user.favorites, playback.connected,
                            { play(results.songs, song) }, { vm.favorite(song) }, { addSong = song },
                            { player.enqueue(song, true) }, { player.enqueue(song, false) },
                            { navigate("album:${song.albumId}") }, { navigate("artist:${song.artistId}") })
                    }
                    item { Text("Albums", style = MaterialTheme.typography.titleLarge) }
                    items(results.albums, key = { "album:" + it.id }) { AlbumRow(it) { navigate("album:${it.id}") } }
                    item { Text("Artists", style = MaterialTheme.typography.titleLarge) }
                    items(results.artists, key = { "artist:" + it.id }) { a -> TextButton(onClick = { navigate("artist:${a.id}") }) { Text(a.name) } }
                    item { Text("Playlists", style = MaterialTheme.typography.titleLarge) }
                    val matching = user.playlists.filter { it.name.contains(query.trim(), true) }
                    items(matching, key = { "playlist:" + it.id }) { p -> TextButton(onClick = { navigate("playlist:${p.id}") }) { Text(p.name) } }
                    if (results.songs.isEmpty() && results.albums.isEmpty() && results.artists.isEmpty() && matching.isEmpty()) item { Text("No matches.") }
                }
            } else {
                if (album != null) item {
                    SongArtwork(album.songs.firstOrNull()?.albumArtUri, Modifier.size(180.dp))
                    Text(album.artist)
                }
                if (artist != null) {
                    item { Text("Albums", style = MaterialTheme.typography.titleLarge) }
                    items(artist.albums, key = { "album:" + it.id }) { AlbumRow(it) { navigate("album:${it.id}") } }
                }
                item {
                    Text("${tracks.size} songs · ${formatSongDuration(tracks.sumOf { it.durationMs })}")
                    Row {
                        Button(onClick = { play(tracks) }, enabled = tracks.isNotEmpty() && playback.connected) { Text("Play all") }
                        TextButton(onClick = { play(tracks, shuffle = true) }, enabled = tracks.isNotEmpty() && playback.connected) { Text("Shuffle") }
                    }
                    if (route == "Songs") {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            SongSort.entries.forEach { option ->
                                FilterChip(selected = sort == option, onClick = { vm.order(option) }, label = { Text(option.name) })
                            }
                        }
                        TextButton(onClick = { vm.order(reverse = !reverse) }) { Text(if (reverse) "Descending" else "Ascending") }
                    }
                    if (playlist != null && user.entries.any { it.playlistId == playlist.id && it.songId !in byId }) {
                        Text("Some songs are no longer on this device. Unavailable songs are skipped.")
                    }
                    if (tracks.isEmpty()) Text(if (playlist != null) "This playlist has no available songs." else "No songs here yet.")
                }
                items(tracks, key = { it.id }) { song ->
                    LibrarySongRow(song, song.id in user.favorites, playback.connected,
                        { play(tracks, song) }, { vm.favorite(song) }, { addSong = song },
                        { player.enqueue(song, true) }, { player.enqueue(song, false) },
                        { navigate("album:${song.albumId}") }, { navigate("artist:${song.artistId}") },
                        if (playlist != null) ({ vm.remove(playlist.id, song) }) else null)
                }
            }
        }
    }
    if (create || editing != null) AlertDialog(onDismissRequest = { create = false; editing = null },
        title = { Text(if (create) "Create playlist" else "Rename playlist") },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Name") }) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
            if (create) vm.create(name) else editing?.let { vm.rename(it.id, name) }
            create = false; editing = null
        }) { Text("Save") } }, dismissButton = { TextButton(onClick = { create = false; editing = null }) { Text("Cancel") } })
    deleting?.let { p -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete ${p.name}?") },
        text = { Text("The songs remain on your device.") },
        confirmButton = { TextButton(onClick = { vm.delete(p.id); deleting = null }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }) }
    addSong?.let { song -> AlertDialog(onDismissRequest = { addSong = null }, title = { Text("Add to playlist") },
        text = { Column {
            if (user.playlists.isEmpty()) Text("Create a playlist in the Playlists section first.")
            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                items(user.playlists) { p -> TextButton(onClick = { vm.add(p.id, song); addSong = null }) { Text(p.name) } }
            }
        } }, confirmButton = { TextButton(onClick = { addSong = null }) { Text("Close") } }) }
}

@Composable
private fun AlbumRow(album: AlbumGroup, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SongArtwork(album.songs.firstOrNull()?.albumArtUri, Modifier.size(56.dp))
            Column(Modifier.padding(8.dp)) {
                Text(album.name)
                Text("${album.artist} · ${album.songs.size} songs", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun LibrarySongRow(song: Song, favorite: Boolean, ready: Boolean, play: () -> Unit, toggleFavorite: () -> Unit,
    addPlaylist: () -> Unit, playNext: () -> Unit, enqueue: () -> Unit, album: () -> Unit, artist: () -> Unit,
    remove: (() -> Unit)? = null) {
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SongListItem(song, Modifier.weight(1f), play, ready)
        TextButton(onClick = toggleFavorite) { Text(if (favorite) "♥" else "♡") }
        Box {
            TextButton(onClick = { menu = true }) { Text("⋮") }
            DropdownMenu(menu, onDismissRequest = { menu = false }) {
                listOf("Play" to play, "Play next" to playNext, "Add to queue" to enqueue,
                    "Add to playlist" to addPlaylist, (if (favorite) "Unfavorite" else "Favorite") to toggleFavorite,
                    "Go to album" to album, "Go to artist" to artist).forEach { (label, action) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { menu = false; action() },
                        enabled = ready || label !in listOf("Play", "Play next", "Add to queue"))
                }
                if (remove != null) DropdownMenuItem(text = { Text("Remove from playlist") }, onClick = { menu = false; remove() })
            }
        }
    }
}
