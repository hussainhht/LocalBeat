package com.example.localbeat.data.library

import android.content.Context
import kotlinx.coroutines.flow.combine

data class UserLibrary(
    val favorites: Set<String> = emptySet(),
    val playlists: List<LocalPlaylist> = emptyList(),
    val entries: List<PlaylistSong> = emptyList(),
    val stats: List<PlayStat> = emptyList(),
)

class LibraryRepository(context: Context) {
    private val dao by lazy { LibraryDatabase.get(context).library() }
    fun observe() = combine(dao.favorites(), dao.playlists(), dao.entries(), dao.stats()) { favorites, playlists, entries, stats ->
        UserLibrary(favorites.map { it.songId }.toSet(), playlists, entries, stats)
    }
    suspend fun favorite(id: String, enabled: Boolean) {
        if (enabled) dao.favorite(Favorite(id)) else dao.unfavorite(id)
    }
    suspend fun create(name: String) = dao.create(LocalPlaylist(name = name.trim().also { require(it.isNotEmpty()) }))
    suspend fun rename(id: Long, name: String) = dao.rename(id, name.trim().also { require(it.isNotEmpty()) })
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun add(playlist: Long, song: String) = dao.add(PlaylistSong(playlist, song, System.currentTimeMillis()))
    suspend fun remove(playlist: Long, song: String) = dao.remove(playlist, song)
    suspend fun record(id: String) = dao.record(id, System.currentTimeMillis())
}
