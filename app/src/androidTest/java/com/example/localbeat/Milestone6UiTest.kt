package com.example.localbeat

import android.Manifest
import android.content.ContentUris
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.localbeat.data.library.LibraryDatabase
import com.example.localbeat.data.model.DownloadItem
import com.example.localbeat.downloader.AudioProcessor
import com.example.localbeat.downloader.MediaStorePublisher
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/** Exercises real MediaStore, Room and MediaSession state; fixtures never ship in production. */
@RunWith(AndroidJUnit4::class)
class Milestone6UiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val title = "000 M6 verification — A long local track title for layout checks"
    private val playlistName = "M6 test " + UUID.randomUUID().toString().take(6)
    private var fixture: Uri? = null

    @Before fun prepareLibrary() = runBlocking {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName,
            if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
        val source = File(context.cacheDir, "m6-ui-fixture.wav")
        val frames = 8000 * 90
        val pcm = ByteBuffer.allocate(frames * 2).order(ByteOrder.LITTLE_ENDIAN)
        repeat(frames) { pcm.putShort((kotlin.math.sin(it * 2 * Math.PI * 440 / 8000) * 800).toInt().toShort()) }
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + pcm.capacity()).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(pcm.capacity()).array()
        source.outputStream().use { it.write(header); it.write(pcm.array()) }
        val output = File(context.cacheDir, "m6-ui-fixture.m4a")
        val item = DownloadItem(UUID.randomUUID().toString(), "test-fixture", title,
            artist = "LocalBeat UI test", album = "Milestone Six")
        val processor = AudioProcessor(context)
        try {
            processor.convert(item, source, output, AtomicBoolean(false))
            fixture = MediaStorePublisher(context).publish(item, output, processor.inspect(output).second) {}
        } finally { source.delete(); output.delete() }
        compose.activityRule.scenario.recreate()
        Unit
    }

    @After fun cleanLibrary() = runBlocking {
        context.stopService(android.content.Intent(context, com.example.localbeat.player.PlaybackService::class.java))
        val dao = LibraryDatabase.get(context).library()
        dao.playlists().first().filter { it.name.startsWith(playlistName) }.forEach { dao.delete(it.id) }
        fixture?.let {
            val id = ContentUris.parseId(it).toString()
            dao.unfavorite(id)
            LibraryDatabase.get(context).openHelper.writableDatabase.execSQL("DELETE FROM PlayStat WHERE songId = ?", arrayOf(id))
            context.contentResolver.delete(it, null, null)
        }
        Unit
    }

    private fun desc(value: String) = compose.onNodeWithContentDescription(value, useUnmergedTree = true)
    private fun waitDesc(value: String) {
        compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription(value, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun tab(name: String) = compose.onNode(hasText(name) and hasClickAction()).performClick()
    private fun waitText(text: String) {
        compose.waitUntil(30_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        compose.onAllNodes(isRoot()).onLast().captureToImage() // Wait for the current window to draw.
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(500) // Allow SurfaceFlinger to present the settled frame.
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("No device screenshot")
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?.takeIf { it.isNotBlank() }?.let(::File) ?: context.externalCacheDir!!
        directory.mkdirs()
        File(directory, "m6-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun navigationCollectionsAndRealPlaybackStayConnected() {
        waitText("Your music. Your rhythm.")
        screenshot("home")
        tab("Search")
        compose.onNode(hasSetTextAction()).performTextInput("000 M6")
        compose.onNode(hasSetTextAction()).performImeAction()
        waitText(title)
        screenshot("search")
        desc("Add to favorites").performClick()
        waitDesc("Remove from favorites")

        // Search drafts survive switching tabs and Activity recreation.
        tab("Downloads")
        waitText("Bring more music home.")
        compose.onNode(hasSetTextAction()).performTextInput("invalid")
        compose.onNodeWithText("Fetch info").performClick()
        waitText("Enter an HTTP or HTTPS URL.")
        screenshot("downloads")
        tab("Search")
        compose.onNode(hasSetTextAction()).assertTextContains("000 M6")
        compose.activityRule.scenario.recreate()
        waitText(title)
        compose.onNode(hasSetTextAction()).assertTextContains("000 M6")

        desc("Options for $title").performClick()
        compose.onNodeWithText("Go to album").performClick()
        waitText("Milestone Six")
        screenshot("album")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        waitText(title)
        desc("Options for $title").performClick()
        compose.onNodeWithText("Go to artist").performClick()
        waitText("LocalBeat UI test")
        screenshot("artist")

        tab("Library")
        screenshot("library-songs")
        compose.onNodeWithText("Albums").performClick()
        screenshot("albums-grid")
        compose.onNodeWithText("Artists").performClick()
        screenshot("artists-list")
        compose.onNodeWithText("Favorites").performClick()
        waitText("Your favorites")
        screenshot("favorites")
        compose.onNodeWithText("Playlists").performClick()
        compose.onNodeWithText("Create").performClick()
        compose.onNode(hasSetTextAction()).performTextInput(playlistName)
        compose.onNodeWithText("Save").performClick()
        waitText(playlistName)
        compose.onNodeWithText(playlistName).performClick()
        compose.onNodeWithText("Add songs").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("000 M6")
        compose.onNode(hasText(title) and hasClickAction()).performClick()
        compose.onNodeWithText("Done").performClick()
        waitText(title)
        screenshot("playlist")
        compose.onNodeWithText("Play all").performClick()
        compose.waitUntil(20_000) {
            compose.onAllNodesWithContentDescription("Open Now Playing", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("mini-player")
        desc("Open Now Playing").performClick()
        waitText("NOW PLAYING")
        screenshot("now-playing-artwork")
        compose.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        waitDesc("Pause")
        desc("Pause").performScrollTo().performClick()
        compose.onNodeWithTag("playback_seek").performSemanticsAction(SemanticsActions.SetProgress) { it(30000f) }
        waitText("00:30")
        compose.onNode(isToggleable()).performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(isOn()).fetchSemanticsNodes().isNotEmpty() }
        desc("Change repeat mode").performClick()
        compose.onNode(hasScrollAction()).performTouchInput { swipeDown() }
        screenshot("now-playing")
        desc("Playback queue").performClick()
        waitText("Playback queue")
        screenshot("queue")
        compose.onNodeWithText("Done").performClick()
        desc("Collapse Now Playing").performClick()
        waitDesc("Open Now Playing")

        runBlocking {
            val dao = LibraryDatabase.get(context).library()
            val id = ContentUris.parseId(fixture!!).toString()
            Assert.assertTrue(dao.favorites().first().any { it.songId == id })
            val playlist = dao.playlists().first().single { it.name == playlistName }
            Assert.assertTrue(dao.entries().first().any { it.playlistId == playlist.id && it.songId == id })
        }
        tab("Library") // Reselecting the tab returns to its root.
        compose.onNodeWithText("Playlists").performClick()
        screenshot("playlists-grid")
        desc("Options for $playlistName").performClick()
        compose.onNodeWithText("Rename playlist").performClick()
        val renamed = "$playlistName renamed"
        compose.onNode(hasSetTextAction()).performTextReplacement(renamed)
        compose.onNodeWithText("Save").performClick()
        waitText(renamed)
        desc("Options for $renamed").performClick()
        compose.onNodeWithText("Delete playlist").performClick()
        compose.onNodeWithText("Delete", substring = false).performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText(renamed).fetchSemanticsNodes().isEmpty() }
    }
}
