package com.example.localbeat

import android.content.ContentUris
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.localbeat.data.model.DownloadItem
import com.example.localbeat.data.repository.MusicRepositoryImpl
import com.example.localbeat.downloader.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Uses a generated PCM fixture; executes real packaged FFmpeg and real MediaStore. */
@RunWith(AndroidJUnit4::class)
class NativeAudioImportTest {
    @Test fun nativeConversionPublishesMp3AndM4aIntoExistingLibrary() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "native-test").apply { mkdirs() }
        val source = File(directory, "fixture.wav")
        val pcm = ByteBuffer.allocate(44100 * 2).order(ByteOrder.LITTLE_ENDIAN)
        repeat(44100) { pcm.putShort((kotlin.math.sin(it * 2 * Math.PI * 440 / 44100) * 8000).toInt().toShort()) }
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + pcm.capacity()).put("WAVEfmt ".toByteArray())
            .putInt(16).putShort(1).putShort(1).putInt(44100).putInt(88200).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(pcm.capacity()).array()
        source.outputStream().use { it.write(header); it.write(pcm.array()) }
        val processor = AudioProcessor(context)
        try {
            for (format in listOf("M4A", "MP3")) {
                val item = DownloadItem(UUID.randomUUID().toString(), "fixture", "LocalBeat native test", format = format)
                val output = File(directory, if (format == "MP3") "out.mp3" else "out.m4a")
                processor.convert(item, source, output, AtomicBoolean(false))
                val duration = processor.inspect(output).second
                assertTrue(duration > 0)
                val uri = MediaStorePublisher(context).publish(item, output, duration) {}
                try {
                    assertTrue(context.contentResolver.openInputStream(uri)!!.use { it.read() } >= 0)
                    val song = MusicRepositoryImpl(context.contentResolver).getSongById(ContentUris.parseId(uri).toString())
                    assertNotNull("Published audio must appear in existing Songs query", song)
                    assertEquals(item.title, song!!.title)
                } finally { context.contentResolver.delete(uri, null, null) }
            }
        } finally { directory.deleteRecursively() }
    }

    /** Opt-in network smoke test; normal test runs never depend on a public website. */
    @Test fun permittedUrlDownloadsThroughRealQueue() = runBlocking {
        val url = InstrumentationRegistry.getArguments().getString("sampleUrl")
        org.junit.Assume.assumeTrue("Supply a permitted sampleUrl to run the network smoke test", url != null)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context,
            com.example.localbeat.data.library.LibraryDatabase::class.java).build()
        val repo = com.example.localbeat.data.repository.DownloadRepositoryImpl(context, dao = db.downloads())
        var saved: android.net.Uri? = null
        try {
            repo.ready()
            val id = UUID.randomUUID().toString()
            val info = repo.downloader.info(url!!, id)
            assertTrue(info.title.isNotBlank())
            assertTrue(info.formats.isNotEmpty())
            db.downloads().put(DownloadItem(id, url, info.title, format = "MP3", artist = info.artist, album = info.album))
            assertTrue(repo.runNext {})
            val task = db.downloads().get(id)!!
            assertEquals(task.error, com.example.localbeat.data.model.DownloadStatus.COMPLETED, task.status)
            saved = android.net.Uri.parse(task.finalUri!!)
            assertNotNull(MusicRepositoryImpl(context.contentResolver).getSongById(ContentUris.parseId(saved).toString()))
        } finally {
            saved?.let { context.contentResolver.delete(it, null, null) }
            repo.scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            db.close()
        }
    }
}
