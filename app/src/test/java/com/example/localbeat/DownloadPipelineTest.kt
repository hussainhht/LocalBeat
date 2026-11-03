package com.example.localbeat

import android.content.Context
import android.net.Uri
import androidx.room.Room
import com.example.localbeat.data.library.LibraryDatabase
import com.example.localbeat.data.model.*
import com.example.localbeat.data.repository.DownloadRepositoryImpl
import com.example.localbeat.downloader.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DownloadPipelineTest {
    private class Fake : AudioDownloader {
        var fail = false
        var block = false
        val started = CountDownLatch(1)
        val stopped = CountDownLatch(1)
        var onInfo: () -> Unit = {}
        var onDownload: () -> Unit = {}
        override fun info(url: String, id: String): MediaPreview {
            onInfo()
            return MediaPreview("Sample", null, 1.0, "Test", null, null, null, listOf("aac"), listOf(128))
        }
        override fun download(item: DownloadItem, directory: File, callback: (Float?, Long?, Long?) -> Unit): File {
            onDownload(); started.countDown()
            if (block) { check(stopped.await(5, TimeUnit.SECONDS)); error("cancelled") }
            check(!fail) { "network unavailable" }
            callback(.5f, 512, 1024)
            return File(directory, "source").apply { writeBytes(byteArrayOf(1)) }
        }
        override fun cancel(id: String) { stopped.countDown() }
    }
    @Test fun stagesSuccessFailureAndRetryUseTheActualPipeline() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(app, LibraryDatabase::class.java).build()
        val fake = Fake()
        val stages = mutableListOf<DownloadStatus>()
        val id = "pipeline-test"
        fun stage() { runBlocking { stages += db.downloads().get(id)!!.status } }
        fake.onInfo = ::stage; fake.onDownload = ::stage
        val processor = object : AudioProcessor(app) {
            override fun inspect(file: File) = "audio/mp4a-latm" to 1000L
            override fun convert(item: DownloadItem, input: File, output: File, cancelled: AtomicBoolean) {
                stage(); output.writeBytes(byteArrayOf(2))
            }
        }
        val publisher = object : MediaStorePublisher(app) {
            override suspend fun publish(item: DownloadItem, file: File, duration: Long, pending: suspend (String) -> Unit): Uri {
                stage(); return Uri.parse("content://media/external/audio/media/1")
            }
        }
        val repo = DownloadRepositoryImpl(app, fake, processor, publisher, db.downloads())
        try {
            repo.ready()
            val item = DownloadItem(id, "https://example.org", "Sample")
            db.downloads().put(item)
            fake.fail = true
            assertTrue(repo.runNext {})
            assertEquals(DownloadStatus.FAILED, db.downloads().get(id)!!.status)
            assertNull(db.downloads().get(id)!!.finalUri)
            repo.retry(id)
            assertEquals(DownloadStatus.QUEUED, db.downloads().get(id)!!.status)
            fake.fail = false; stages.clear()
            assertTrue(repo.runNext {})
            assertEquals(listOf(DownloadStatus.FETCHING_INFO, DownloadStatus.DOWNLOADING, DownloadStatus.PROCESSING, DownloadStatus.SAVING), stages)
            val saved = db.downloads().get(id)!!
            assertEquals(DownloadStatus.COMPLETED, saved.status)
            assertNotNull(saved.finalUri)
            assertNotNull(saved.completedAt)
            assertFalse(repo.runNext {})
        } finally { repo.scope.cancel(); db.close() }
    }
    @Test fun cancellationStopsUnderlyingDownloaderAndNeverPublishes() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val db = Room.inMemoryDatabaseBuilder(app, LibraryDatabase::class.java).build()
        val fake = Fake().apply { block = true }
        val repo = DownloadRepositoryImpl(app, fake, dao = db.downloads())
        try {
            repo.ready()
            db.downloads().put(DownloadItem("cancel-test", "https://example.org", "Cancel"))
            val running = async(Dispatchers.IO) { repo.runNext {} }
            assertTrue(withContext(Dispatchers.IO) { fake.started.await(5, TimeUnit.SECONDS) })
            repo.cancelDownload("cancel-test")
            withTimeout(5000) { running.await() }
            val item = db.downloads().get("cancel-test")!!
            assertEquals(DownloadStatus.CANCELLED, item.status)
            assertNull(item.finalUri)
            assertEquals(0, fake.stopped.count)
        } finally { repo.scope.cancel(); db.close() }
    }
}
