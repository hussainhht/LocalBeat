package com.example.localbeat

import com.example.localbeat.data.model.Song
import com.example.localbeat.ui.screens.*
import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [28])
class LibraryIndexTest {
    private fun song(id: String, album: Long, artist: Long, title: String, date: Long) =
        Song(id, title, "Artist $artist", album = "Album $album", albumId = album, artistId = artist, dateAdded = date)
    @Test fun groupsByMediaStoreIdentityInsteadOfDisplayName() {
        val tracks = listOf(song("1", 10, 1, "one", 1), song("2", 10, 2, "two", 2), song("3", 20, 1, "three", 3))
        val index = indexLibrary(tracks)
        assertEquals(2, index.albums.size)
        assertEquals("Various artists", index.albums.first { it.id == 10L }.artist)
        assertEquals(2, index.artists.first { it.id == 1L }.albums.size)
    }
    @Test fun sortingUsesMetadataAndSupportsDescending() {
        val tracks = listOf(song("1", 10, 1, "z", 1), song("2", 20, 2, "Alpha", 2))
        assertEquals("2", sortSongs(tracks, SongSort.Name, false).first().id)
        assertEquals("2", sortSongs(tracks, SongSort.DateAdded, true).first().id)
        assertEquals("1", tracks.first().id)
    }
}
