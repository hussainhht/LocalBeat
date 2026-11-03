package com.example.localbeat

import androidx.room.Room
import com.example.localbeat.data.library.*
import com.example.localbeat.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DownloadPersistenceTest {
    @Test fun filenamesAndUrlsAreSafe() {
        assertEquals("Audio", safeAudioName("..."))
        assertEquals("a_b_c", safeAudioName("a/b:c"))
        assertTrue(safeAudioName("x".repeat(500)).length <= 100)
        assertTrue(validMediaUrl("https://example.org/audio"))
        listOf("file:///etc/passwd", "ftp://example.org", "https://user:secret@example.org", "--exec command", "https://").forEach {
            assertFalse(validMediaUrl(it))
        }
    }
    @Test fun queueSurvivesReopenAndTerminalRecordsStayOutOfQueue() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val name = "download-test.db"
        app.deleteDatabase(name)
        var db = Room.databaseBuilder(app, LibraryDatabase::class.java, name).build()
        val item = DownloadItem("first", "https://example.org/audio", "Audio", createdAt = 1)
        db.downloads().put(item)
        db.library().favorite(Favorite("song"))
        db.close()
        db = Room.databaseBuilder(app, LibraryDatabase::class.java, name).build()
        try {
            assertEquals(item, db.downloads().next())
            db.downloads().put(item.copy(status = DownloadStatus.DOWNLOADING))
            assertNull(db.downloads().next())
            assertEquals(1, db.downloads().interrupted().size)
            db.downloads().put(item.copy(status = DownloadStatus.CANCELLED))
            assertTrue(db.downloads().interrupted().isEmpty())
            db.downloads().put(item.copy(status = DownloadStatus.QUEUED))
            assertEquals("first", db.downloads().next()!!.id)
            db.downloads().put(item.copy(status = DownloadStatus.COMPLETED, finalUri = "content://media/1"))
            db.downloads().clear()
            assertNull(db.downloads().get(item.id))
        } finally { db.close(); app.deleteDatabase(name) }
    }
    @Test fun migrationPreservesLibraryAndCreatesValidatedDownloadTable() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val name = "migration-test.db"
        app.deleteDatabase(name)
        val old = app.openOrCreateDatabase(name, 0, null)
        old.execSQL("CREATE TABLE Favorite(songId TEXT NOT NULL PRIMARY KEY)")
        old.execSQL("CREATE TABLE LocalPlaylist(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
        old.execSQL("CREATE TABLE PlaylistSong(playlistId INTEGER NOT NULL, songId TEXT NOT NULL, addedAt INTEGER NOT NULL, PRIMARY KEY(playlistId,songId), FOREIGN KEY(playlistId) REFERENCES LocalPlaylist(id) ON DELETE CASCADE)")
        old.execSQL("CREATE TABLE PlayStat(songId TEXT NOT NULL PRIMARY KEY, lastPlayed INTEGER NOT NULL, playCount INTEGER NOT NULL)")
        old.execSQL("INSERT INTO Favorite VALUES('song')")
        old.execSQL("INSERT INTO LocalPlaylist VALUES(1,'Saved')")
        old.execSQL("INSERT INTO PlaylistSong VALUES(1,'song',10)")
        old.execSQL("INSERT INTO PlayStat VALUES('song',10,7)")
        old.version = 1; old.close()
        val db = Room.databaseBuilder(app, LibraryDatabase::class.java, name)
            .addMigrations(LibraryDatabase.MIGRATION_1_2).build()
        try {
            db.downloads().put(DownloadItem("id", "https://example.org", "Test"))
            assertEquals(1, db.openHelper.readableDatabase.query("SELECT * FROM Favorite").use { it.count })
            assertEquals(7, db.openHelper.readableDatabase.query("SELECT playCount FROM PlayStat").use { it.moveToFirst(); it.getInt(0) })
            assertEquals(1, db.openHelper.readableDatabase.query("SELECT * FROM PlaylistSong").use { it.count })
        } finally { db.close(); app.deleteDatabase(name) }
    }
}
