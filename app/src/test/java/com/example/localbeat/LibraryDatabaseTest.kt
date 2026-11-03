package com.example.localbeat

import android.content.Context
import androidx.room.Room
import com.example.localbeat.data.library.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LibraryDatabaseTest {
    @Test fun referencesDeduplicateAndPlaylistDeletionCascades() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), LibraryDatabase::class.java).build()
        try {
            val dao = db.library()
            dao.favorite(Favorite("missing-song"))
            dao.favorite(Favorite("missing-song"))
            assertEquals(1, dao.favorites().first().size)
            val a = dao.create(LocalPlaylist(name = "First"))
            val b = dao.create(LocalPlaylist(name = "Second"))
            dao.add(PlaylistSong(a, "missing-song", 1))
            dao.add(PlaylistSong(a, "missing-song", 2))
            dao.add(PlaylistSong(b, "missing-song", 3))
            assertEquals(2, dao.entries().first().size)
            dao.rename(a, "Renamed")
            assertTrue(dao.playlists().first().any { it.name == "Renamed" })
            dao.delete(a)
            assertEquals(listOf(b), dao.entries().first().map { it.playlistId })
            dao.remove(b, "missing-song")
            assertTrue(dao.entries().first().isEmpty())
            dao.unfavorite("missing-song")
            assertTrue(dao.favorites().first().isEmpty())
        } finally { db.close() }
    }
    @Test fun playsUpdateOneRowAndPreserveCount() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), LibraryDatabase::class.java).build()
        try {
            val dao = db.library()
            dao.record("1", 10); dao.record("1", 20); dao.record("2", 30)
            val stats = dao.stats().first()
            assertEquals(listOf("2", "1"), stats.map { it.songId })
            assertEquals(2L, stats.last().playCount)
            assertEquals(20L, stats.last().lastPlayed)
        } finally { db.close() }
    }
}
