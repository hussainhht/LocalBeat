package com.example.localbeat.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.localbeat.data.library.*
import com.example.localbeat.data.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class AlbumGroup(val id: Long, val name: String, val artist: String, val songs: List<Song>)
data class ArtistGroup(val id: Long, val name: String, val songs: List<Song>, val albums: List<AlbumGroup>)
data class LibraryIndex(val songs: List<Song> = emptyList(), val albums: List<AlbumGroup> = emptyList(), val artists: List<ArtistGroup> = emptyList())
fun indexLibrary(songs: List<Song>): LibraryIndex {
    val albums = songs.groupBy { it.albumId }.map { (id, tracks) ->
        AlbumGroup(id, tracks.first().album, tracks.map { it.artist }.distinct().let { if (it.size == 1) it.first() else "Various artists" }, tracks)
    }.sortedBy { it.name.lowercase() }
    val artists = songs.groupBy { it.artistId }.map { (id, tracks) ->
        val albumIds = tracks.map { it.albumId }.toSet()
        ArtistGroup(id, tracks.first().artist, tracks, albums.filter { it.id in albumIds })
    }.sortedBy { it.name.lowercase() }
    return LibraryIndex(songs, albums, artists)
}
enum class SongSort { Name, Artist, Album, DateAdded, Duration }
fun sortSongs(songs: List<Song>, sort: SongSort, descending: Boolean): List<Song> {
    val comparator = when (sort) {
        SongSort.Name -> compareBy<Song> { it.title.lowercase() }
        SongSort.Artist -> compareBy { it.artist.lowercase() }
        SongSort.Album -> compareBy { it.album.lowercase() }
        SongSort.DateAdded -> compareBy { it.dateAdded }
        SongSort.Duration -> compareBy { it.durationMs }
    }.thenBy { it.id }
    return songs.sortedWith(if (descending) comparator.reversed() else comparator)
}
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LibraryRepository(application)
    private val preferences = application.getSharedPreferences("library", 0)
    val sort = MutableStateFlow(runCatching { SongSort.valueOf(preferences.getString("sort", "Name")!!) }.getOrDefault(SongSort.Name))
    val descending = MutableStateFlow(preferences.getBoolean("descending", false))
    val error = MutableStateFlow<String?>(null)
    val user = MutableStateFlow(UserLibrary())
    val index = MutableStateFlow(LibraryIndex())
    private var indexJob: Job? = null
    private var observer: Job? = null
    init { retry() }
    fun retry() {
        observer?.cancel()
        observer = viewModelScope.launch(Dispatchers.IO) {
            try { repository.observe().collect { user.value = it; error.value = null } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = "Unable to read saved library data. Retry to reconnect." }
        }
    }
    fun load(songs: List<Song>) {
        indexJob?.cancel()
        indexJob = viewModelScope.launch(Dispatchers.Default) { index.value = indexLibrary(songs) }
    }
    fun order(value: SongSort = sort.value, reverse: Boolean = descending.value) {
        sort.value = value; descending.value = reverse
        preferences.edit().putString("sort", value.name).putBoolean("descending", reverse).apply()
    }
    private fun update(action: suspend LibraryRepository.() -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try { repository.action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error.value = "Unable to save this change. Please try again." }
        }
    }
    fun favorite(song: Song) = update { favorite(song.id, song.id !in user.value.favorites) }
    fun create(name: String) = update { create(name); Unit }
    fun rename(id: Long, name: String) = update { rename(id, name) }
    fun delete(id: Long) = update { delete(id) }
    fun add(id: Long, song: Song) = update { add(id, song.id) }
    fun remove(id: Long, song: Song) = update { remove(id, song.id) }
}
