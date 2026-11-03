package com.example.localbeat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localbeat.data.model.Song
import com.example.localbeat.player.AudioPlayer
import com.example.localbeat.ui.components.*
import com.example.localbeat.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val libraryFilters = listOf("Songs", "Albums", "Artists", "Playlists", "Favorites")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(route: String, vm: LibraryViewModel, player: AudioPlayer, navigate: (String) -> Unit,
    modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, refresh: () -> Unit = {}) {
    val index by vm.index.collectAsStateWithLifecycle()
    val user by vm.user.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val reverse by vm.descending.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val playback by player.state.collectAsStateWithLifecycle()
    var optionsSongId by rememberSaveable { mutableStateOf<String?>(null) }
    var addSongId by rememberSaveable { mutableStateOf<String?>(null) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var playlistOptions by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var create by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var addTracks by rememberSaveable { mutableStateOf(false) }
    var addQuery by rememberSaveable { mutableStateOf("") }
    val byId = remember(index) { index.songs.associateBy { it.id } }
    val favoriteSongs = remember(index, user.favorites) { index.songs.filter { it.id in user.favorites } }
    val recent = remember(index, user.stats) { user.stats.sortedByDescending { it.lastPlayed }.take(100).mapNotNull { byId[it.songId] } }
    val most = remember(index, user.stats) { user.stats.sortedByDescending { it.playCount }.mapNotNull { byId[it.songId] } }
    val added = remember(index) { index.songs.sortedByDescending { it.dateAdded } }
    val playlistTracks = remember(user.entries, index) {
        user.entries.groupBy { it.playlistId }.mapValues { (_, entries) -> entries.mapNotNull { byId[it.songId] } }
    }
    val sorted by produceState(index.songs, index, sort, reverse) {
        value = withContext(Dispatchers.Default) { sortSongs(index.songs, sort, reverse) }
    }
    val album = index.albums.find { route == "album:${it.id}" }
    val artist = index.artists.find { route == "artist:${it.id}" }
    val playlist = user.playlists.find { route == "playlist:${it.id}" }
    val detail = route.contains(":")
    val filterState = rememberLazyListState()
    LaunchedEffect(route) {
        if (route in libraryFilters) filterState.scrollToItem(libraryFilters.indexOf(route))
    }
    val tracks = when {
        album != null -> album.songs
        artist != null -> artist.songs
        playlist != null -> playlistTracks[playlist.id].orEmpty()
        route == "Favorites" -> favoriteSongs
        route == "Recently Played" -> recent
        route == "Recently Added" -> added
        route == "Most Played" -> most
        detail -> emptyList()
        else -> sorted
    }
    fun play(list: List<Song>, song: Song? = list.firstOrNull(), shuffle: Boolean = false) {
        if (song != null) player.playQueue(list, song, shuffle)
    }
    val songRow: @Composable (Song, List<Song>) -> Unit = { song, queue ->
        SongRow(song, { play(queue, song) }, enabled = playback.connected,
            active = playback.currentSong?.id == song.id,
            favorite = song.id in user.favorites, onFavorite = { vm.favorite(song) },
            onOptions = { optionsSongId = song.id })
    }
    when (route) {
        "Home" -> HomeScreen(
            listOf("Recently Played" to recent, "Favorites" to favoriteSongs, "Recently Added" to added, "Most Played" to most),
            playback.connected, navigate, { list, song -> play(list, song) },
            user.playlists.map { it to playlistTracks[it.id].orEmpty() }, index.songs.size)
        "Search" -> SearchScreen(index, user.playlists, playlistTracks, navigate, songRow)
        else -> Column(modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = BeatDimens.Gutter).padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) IconButton(onClick = onBack) { Icon(BeatIcons.Back, "Back") }
                Column(Modifier.weight(1f)) {
                    Text(if (detail) when { album != null -> "Album"; artist != null -> "Artist"; playlist != null -> "Playlist"; else -> "Collection" }
                        else if (route in libraryFilters) "Your library" else route,
                        style = if (detail) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineLarge)
                    if (!detail) Text("${trackCount(index.songs.size)} · on this device", color = BeatColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { navigate("Search") }) { Icon(BeatIcons.Search, "Search library") }
                IconButton(onClick = refresh) { Icon(BeatIcons.Refresh, "Refresh library", tint = BeatColors.Muted) }
            }
            if (route in libraryFilters) LazyRow(state = filterState, contentPadding = PaddingValues(horizontal = BeatDimens.Gutter),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(libraryFilters) { filter -> FilterChip(selected = route == filter, onClick = { navigate(filter) },
                    label = { Text(filter) }, colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BeatColors.Crimson, selectedLabelColor = BeatColors.Ink,
                        containerColor = BeatColors.Surface), border = null) }
            }
            LazyVerticalGrid(columns = GridCells.Adaptive(156.dp), modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = BeatDimens.Gutter, end = BeatDimens.Gutter, top = 16.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (error != null) item(span = { GridItemSpan(maxLineSpan) }) { ErrorState(error!!, vm::retry) }
                when (route) {
                    "Albums" -> {
                        item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader("Albums · ${index.albums.size}") }
                        if (index.albums.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyState("Albums live here", "Albums are grouped from the music on your device.", BeatIcons.Album)
                        }
                        items(index.albums, key = { it.id }) { a -> AlbumCard(a, { navigate("album:${a.id}") }) }
                    }
                    "Artists" -> {
                        item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader("Artists · ${index.artists.size}") }
                        if (index.artists.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyState("Get to know your library", "Artist collections appear when music is added.", BeatIcons.Artist)
                        }
                        items(index.artists, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { a -> ArtistRow(a) { navigate("artist:${a.id}") } }
                    }
                    "Playlists" -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Your mixes", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                                FilledTonalButton(onClick = { create = true; name = ""; addSongId = null }) {
                                    Icon(BeatIcons.Plus, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Create")
                                }
                            }
                        }
                        if (user.playlists.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyState("A soundtrack for every mood", "Collect the songs you love in a mix of your own.", BeatIcons.Playlist,
                                "Create playlist", { create = true; name = "" })
                        }
                        items(user.playlists, key = { it.id }) { p ->
                            PlaylistCard(p.name, playlistTracks[p.id].orEmpty(), { navigate("playlist:${p.id}") },
                                onOptions = { playlistOptions = p.id })
                        }
                    }
                    else -> {
                        if (detail || route == "Favorites") item(span = { GridItemSpan(maxLineSpan) }) {
                            CollectionHero(
                                title = album?.name ?: artist?.name ?: playlist?.name ?: if (route == "Favorites") "Your favorites" else "Collection unavailable",
                                subtitle = album?.artist ?: if (artist != null) albumCount(artist.albums.size) else if (route == "Favorites") "The songs that stay with you." else "Made by you",
                                songs = tracks, favorite = route == "Favorites", artist = artist != null, albumCover = album != null,
                                onOptions = if (playlist != null) ({ playlistOptions = playlist.id }) else null)
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        if (!detail && route != "Favorites") Text(route, style = MaterialTheme.typography.titleLarge)
                                        Text("${trackCount(tracks.size)} · ${formatSongDuration(tracks.sumOf { it.durationMs })}",
                                            color = BeatColors.Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (route == "Songs") SortControl(sort, reverse, { vm.order(it) }, { vm.order(reverse = !reverse) })
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(onClick = { play(tracks) }, enabled = tracks.isNotEmpty() && playback.connected,
                                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)) {
                                        Icon(BeatIcons.Play, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Play all")
                                    }
                                    OutlinedButton(onClick = { play(tracks, shuffle = true) }, enabled = tracks.isNotEmpty() && playback.connected) {
                                        Icon(BeatIcons.Shuffle, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Shuffle")
                                    }
                                }
                                if (playlist != null) {
                                    TextButton(onClick = { addTracks = true; addQuery = "" }) {
                                        Icon(BeatIcons.Plus, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Add songs")
                                    }
                                    if (user.entries.any { it.playlistId == playlist.id && it.songId !in byId })
                                        Text("Some songs are no longer on this device. Unavailable songs are skipped.",
                                            color = BeatColors.Muted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (artist != null && artist.albums.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader("Albums", Modifier.padding(top = 16.dp)) }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(artist.albums, key = { it.id }) { a -> AlbumCard(a, { navigate("album:${a.id}") }, Modifier.width(150.dp)) }
                                }
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader("Songs", Modifier.padding(top = 16.dp)) }
                        }
                        if (tracks.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyState(if (route == "Favorites") "Keep your favorites close" else if (playlist != null) "Start your mix" else "Nothing here yet",
                                if (route == "Favorites") "Tap the heart on any song to save it here."
                                else if (playlist != null) "Add songs from your device to this playlist."
                                else if (detail) "This collection has no available songs."
                                else "Add music to your device, or import audio from Downloads.",
                                if (route == "Favorites") BeatIcons.Heart else BeatIcons.Music,
                                when { playlist != null -> null; route == "Songs" -> "Import audio"; else -> "Browse songs" },
                                { navigate(if (route == "Songs") "Downloads" else "Songs") })
                        }
                        items(tracks, key = { "song:${it.id}" }, span = { GridItemSpan(maxLineSpan) }) { song -> songRow(song, tracks) }
                    }
                }
            }
        }
    }
    optionsSongId?.let { id -> byId[id]?.let { song ->
        OptionsSheet(song.title, song.artist, song.albumArtUri, listOf(
            SheetAction("Play", BeatIcons.Play, playback.connected) { play(tracks.takeIf { song in it } ?: index.songs, song) },
            SheetAction("Play next", BeatIcons.Next, playback.connected) { player.enqueue(song, true) },
            SheetAction("Add to queue", BeatIcons.Queue, playback.connected) { player.enqueue(song, false) },
            SheetAction("Add to playlist", BeatIcons.Playlist) { addSongId = song.id; pickerOpen = true },
            SheetAction(if (id in user.favorites) "Remove from favorites" else "Add to favorites", BeatIcons.Heart) { vm.favorite(song) },
            SheetAction("Go to album", BeatIcons.Album) { navigate("album:${song.albumId}") },
            SheetAction("Go to artist", BeatIcons.Artist) { navigate("artist:${song.artistId}") },
        ) + if (playlist != null) listOf(SheetAction("Remove from playlist", BeatIcons.Delete) { vm.remove(playlist.id, song) }) else emptyList(),
            onDismiss = { optionsSongId = null })
    } }
    user.playlists.find { it.id == playlistOptions }?.let { p ->
        OptionsSheet(p.name, trackCount(playlistTracks[p.id].orEmpty().size), actions = listOf(
            SheetAction("Open playlist", BeatIcons.Playlist) { navigate("playlist:${p.id}") },
            SheetAction("Rename playlist", BeatIcons.Edit) { editingId = p.id; name = p.name },
            SheetAction("Delete playlist", BeatIcons.Delete) { deletingId = p.id }),
            onDismiss = { playlistOptions = null })
    }
    if (create || editingId != null) AlertDialog(onDismissRequest = { create = false; editingId = null },
        icon = { Icon(BeatIcons.Playlist, null, tint = BeatColors.Highlight) },
        title = { Text(if (create) "Name your playlist" else "Rename playlist") },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Playlist name") }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(enabled = name.isNotBlank(), onClick = {
            if (create) vm.create(name) else editingId?.let { vm.rename(it, name) }
            create = false; editingId = null
            if (addSongId != null) pickerOpen = true
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { create = false; editingId = null }) { Text("Cancel") } })
    user.playlists.find { it.id == deletingId }?.let { p ->
        AlertDialog(onDismissRequest = { deletingId = null }, icon = { Icon(BeatIcons.Delete, null) },
            title = { Text("Delete ${p.name}?") }, text = { Text("Your songs will stay on this device.") },
            confirmButton = { Button(onClick = { vm.delete(p.id); deletingId = null; if (playlist?.id == p.id) navigate("Playlists") }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text("Keep playlist") } })
    }
    if (pickerOpen && addSongId != null) ModalBottomSheet(onDismissRequest = { pickerOpen = false; addSongId = null }, containerColor = BeatColors.Elevated) {
        Column(Modifier.fillMaxWidth().padding(horizontal = BeatDimens.Gutter).padding(bottom = 24.dp)) {
            Text("Add to playlist", style = MaterialTheme.typography.headlineSmall)
            Text(byId[addSongId]?.title.orEmpty(), color = BeatColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = { pickerOpen = false; create = true; name = "" }) {
                Icon(BeatIcons.Plus, null); Spacer(Modifier.width(8.dp)); Text("New playlist")
            }
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(user.playlists, key = { it.id }) { p ->
                    ListItem(headlineContent = { Text(p.name) },
                        supportingContent = { Text(trackCount(playlistTracks[p.id].orEmpty().size)) },
                        leadingContent = { PlaylistCover(playlistTracks[p.id].orEmpty(), Modifier.size(48.dp)) },
                        modifier = Modifier.clickable { byId[addSongId]?.let { vm.add(p.id, it) }; pickerOpen = false; addSongId = null },
                        colors = ListItemDefaults.colors(containerColor = BeatColors.Elevated))
                }
            }
        }
    }
    if (addTracks && playlist != null) ModalBottomSheet(onDismissRequest = { addTracks = false }, containerColor = BeatColors.Elevated) {
        Column(Modifier.fillMaxWidth().padding(horizontal = BeatDimens.Gutter).imePadding().padding(bottom = 24.dp)) {
            SectionHeader("Add songs", action = "Done", onAction = { addTracks = false })
            OutlinedTextField(addQuery, { addQuery = it }, singleLine = true,
                placeholder = { Text("Find a song") }, leadingIcon = { Icon(BeatIcons.Search, null) },
                modifier = Modifier.fillMaxWidth())
            val matches = remember(addQuery, index) { index.songs.filter { it.title.contains(addQuery, true) || it.artist.contains(addQuery, true) } }
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(matches, key = { it.id }) { song ->
                    val included = user.entries.any { it.playlistId == playlist.id && it.songId == song.id }
                    SongRow(song, { if (included) vm.remove(playlist.id, song) else vm.add(playlist.id, song) },
                        trailing = { Icon(if (included) BeatIcons.Check else BeatIcons.Plus,
                            if (included) "Added" else "Add", tint = if (included) BeatColors.Highlight else BeatColors.Muted) })
                }
            }
        }
    }
}

@Composable
private fun SortControl(sort: SongSort, reverse: Boolean, onSort: (SongSort) -> Unit, onReverse: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(BeatIcons.Sort, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
            Text(if (sort == SongSort.DateAdded) "Date added" else sort.name)
        }
        DropdownMenu(expanded, { expanded = false }) {
            SongSort.entries.forEach { option -> DropdownMenuItem(
                text = { Text((if (sort == option) "✓ " else "") + if (option == SongSort.DateAdded) "Date added" else option.name) },
                onClick = { onSort(option); expanded = false }) }
            HorizontalDivider()
            DropdownMenuItem(text = { Text(if (reverse) "Descending ↓" else "Ascending ↑") },
                onClick = { onReverse(); expanded = false })
        }
    }
}

@Composable
private fun CollectionHero(title: String, subtitle: String, songs: List<Song>, favorite: Boolean, artist: Boolean, albumCover: Boolean,
    onOptions: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().background(
        Brush.verticalGradient(listOf(BeatColors.Wine.copy(alpha = .65f), BeatColors.Background)), RoundedCornerShape(24.dp))
        .padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (artist) SongArtwork(null, Modifier.size(156.dp), CoverKind.Artist)
        else if (albumCover) SongArtwork(songs.firstOrNull()?.albumArtUri, Modifier.size(184.dp))
        else PlaylistCover(songs, Modifier.size(184.dp), favorite)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.headlineMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = BeatColors.Muted, style = MaterialTheme.typography.bodyMedium)
            }
            if (onOptions != null) IconButton(onClick = onOptions) { Icon(BeatIcons.More, "Playlist options") }
        }
    }
}
