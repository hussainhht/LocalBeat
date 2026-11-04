package com.example.localbeat

import android.content.ContentUris
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.localbeat.data.model.*
import com.example.localbeat.data.repository.*
import com.example.localbeat.downloader.*
import com.yausername.youtubedl_android.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class YouTubeRuntimeTest {
    @Test fun runtimeAndLiveExtraction() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Opt in with liveYouTube=true", args.getString("liveYouTube") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        YoutubeDL.init(context)
        Log.i("LocalBeatRuntime", "bundled yt-dlp=" + YouTubeRuntime.version())
        YouTubeRuntime.prepare(context)
        Log.i("LocalBeatRuntime", "active yt-dlp=" + YouTubeRuntime.version())
        val cases = listOf(
            "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
            "https://youtu.be/aqz-KE-bpKQ?si=share",
            "https://www.youtube.com/shorts/aqz-KE-bpKQ")
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context,
            com.example.localbeat.data.library.LibraryDatabase::class.java).build()
        val repo = DownloadRepositoryImpl(context, dao = db.downloads())
        val failures = mutableListOf<String>()
        try {
            repo.ready()
            for ((index, url) in cases.withIndex().filter { args.getString("caseIndex")?.toIntOrNull()?.let { selected -> it.index == selected } ?: true })
                for (format in listOf("MP3", "M4A").filter { args.getString("outputFormat")?.let { selected -> selected == it } ?: true }) {
                val id = UUID.randomUUID().toString()
                var saved: android.net.Uri? = null
                try {
                    val preview = repo.downloader.info(url, id)
                    db.downloads().put(DownloadItem(id, url, preview.title, format = format))
                    assertTrue(repo.runNext {})
                    val task = db.downloads().get(id)!!
                    assertEquals(task.error, DownloadStatus.COMPLETED, task.status)
                    saved = android.net.Uri.parse(task.finalUri!!)
                    assertTrue(context.contentResolver.openAssetFileDescriptor(saved, "r")!!.use { it.length > 1024 })
                    val song = MusicRepositoryImpl(context.contentResolver).getSongById(ContentUris.parseId(saved).toString())
                    assertNotNull(song)
                    // Exercise the application's actual controller and Media3 PlaybackService.
                    val store = androidx.lifecycle.ViewModelStore()
                    lateinit var appPlayer: com.example.localbeat.player.AudioPlayer
                    val instrumentation = InstrumentationRegistry.getInstrumentation()
                    val activity = instrumentation.startActivitySync(android.content.Intent(context,
                        MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    instrumentation.runOnMainSync {
                        appPlayer = androidx.lifecycle.ViewModelProvider(store,
                            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(
                                context.applicationContext as android.app.Application))
                            .get(com.example.localbeat.player.AudioPlayer::class.java)
                    }
                    try {
                        withTimeout(15000) { while (!appPlayer.state.value.connected) delay(100) }
                        instrumentation.runOnMainSync { appPlayer.play(listOf(song!!), song) }
                        withTimeout(15000) {
                            while (!appPlayer.state.value.isPlaying || appPlayer.state.value.currentPosition <= 0) {
                                check(appPlayer.state.value.error == null) { appPlayer.state.value.error.orEmpty() }
                                delay(100)
                            }
                        }
                    } finally {
                        instrumentation.runOnMainSync {
                            if (appPlayer.state.value.isPlaying) appPlayer.toggle()
                            store.clear()
                            activity.finish()
                        }
                    }
                    Log.i("LocalBeatRuntime", "PASS case=$index format=$format bytes/publish/library/LocalBeat-playback")
                } catch (e: Throwable) {
                    if (e is CancellationException || e !is Exception && e !is AssertionError) throw e
                    val message = sanitizedDiagnostic(e.stackTraceToString())
                    Log.e("LocalBeatRuntime", "case=$index format=$format $message")
                    failures += "case=$index format=$format $message"
                } finally { saved?.let { context.contentResolver.delete(it, null, null) } }
            }
            assertTrue(failures.joinToString("\n"), failures.isEmpty())
        } finally { repo.scope.cancel(); db.close() }
    }
}
