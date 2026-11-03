package com.example.localbeat.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.example.localbeat.data.library.LibraryDatabase
import com.example.localbeat.data.model.*
import com.example.localbeat.downloader.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class DownloadRepositoryImpl internal constructor(private val context: Context,
    val downloader: AudioDownloader = AudioDownloaderImpl(context),
    val processor: AudioProcessor = AudioProcessor(context),
    private val publisher: MediaStorePublisher = MediaStorePublisher(context),
    val dao: DownloadDao = LibraryDatabase.get(context).downloads()) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val libraryRevision = MutableStateFlow(0)
    @Volatile private var activeId: String? = null
    @Volatile private var flag = AtomicBoolean(false)
    private val recovery = scope.async {
        dao.interrupted().forEach {
            it.pendingUri?.let { uri -> runCatching { context.contentResolver.delete(Uri.parse(uri), null, null) } }
            dao.put(it.copy(status = DownloadStatus.FAILED, progress = null, pendingUri = null,
                stage = "Interrupted", error = "App or system interrupted this task. Retry to start again."))
            File(context.cacheDir, "imports/${it.id}").deleteRecursively()
        }
    }
    suspend fun ready() { recovery.await() }
    fun getDownloads() = dao.observe()
    suspend fun enqueue(url: String, preview: MediaPreview, format: String, quality: Int, local: Boolean = false) {
        ready()
        require(local || validMediaUrl(url))
        val item = DownloadItem(UUID.randomUUID().toString(), url, preview.title, preview.thumbnail,
            format, quality, artist = preview.artist, album = preview.album, local = local)
        dao.put(item)
        try { ContextCompat.startForegroundService(context, Intent(context, DownloadService::class.java)) }
        catch (e: Exception) { dao.put(item.copy(status = DownloadStatus.FAILED, error = "Cannot start background transfer. Keep LocalBeat open and retry.", stage = "Could not start")) }
    }
    suspend fun cancelDownload(id: String) {
        ready()
        val item = dao.get(id) ?: return
        if (item.status in listOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELLED)) return
        if (activeId == id) { flag.set(true); downloader.cancel(id); processor.cancel(id) }
        else dao.put(item.copy(status = DownloadStatus.CANCELLED, stage = "Cancelled", progress = null))
    }
    suspend fun retry(id: String) {
        ready()
        val item = dao.get(id) ?: return
        if (item.status != DownloadStatus.FAILED && item.status != DownloadStatus.CANCELLED) return
        dao.put(item.copy(status = DownloadStatus.QUEUED, progress = null, error = null, pendingUri = null, completedAt = null))
        try { ContextCompat.startForegroundService(context, Intent(context, DownloadService::class.java)) }
        catch (e: Exception) { dao.put(item.copy(status = DownloadStatus.FAILED, error = "Open LocalBeat and retry.")) }
    }
    suspend fun runNext(notify: (String) -> Unit): Boolean {
        ready()
        var item = dao.next() ?: return false
        activeId = item.id; flag = AtomicBoolean(false)
        val cancelled = flag
        val cancellationWatcher = scope.launch {
            while (isActive) {
                if (cancelled.get()) { downloader.cancel(item.id); processor.cancel(item.id) }
                delay(100)
            }
        }
        val dir = File(context.cacheDir, "imports/${item.id}").apply { mkdirs() }
        suspend fun stage(status: DownloadStatus, label: String) {
            check(!cancelled.get()) { "Cancelled" }
            item = item.copy(status = status, stage = label, progress = null); dao.put(item); notify(label)
        }
        try {
            val input = if (item.local) {
                stage(DownloadStatus.PROCESSING, "Reading selected video")
                val target = File(dir, "video")
                context.contentResolver.openInputStream(Uri.parse(item.url))?.use { source ->
                    target.outputStream().use { out ->
                        val buffer = ByteArray(65536)
                        while (true) {
                            check(!cancelled.get()) { "Cancelled" }
                            val n = source.read(buffer); if (n < 0) break
                            out.write(buffer, 0, n)
                        }
                    }
                } ?: error("Selected video is no longer accessible. Select it again.")
                target
            } else {
                stage(DownloadStatus.FETCHING_INFO, "Checking media")
                downloader.info(item.url, item.id)
                check(!cancelled.get())
                stage(DownloadStatus.DOWNLOADING, "Downloading")
                var last = 0L
                downloader.download(item, dir) { progress, bytes, total ->
                    if (cancelled.get()) downloader.cancel(item.id)
                    val now = System.currentTimeMillis()
                    if (now - last > 750) {
                        last = now
                        runBlocking {
                            item = item.copy(progress = progress, downloadedBytes = bytes, totalBytes = total)
                            dao.put(item)
                        }
                    }
                }
            }
            stage(DownloadStatus.PROCESSING, "Converting audio")
            val output = File(dir, if (item.format == "MP3") "audio.mp3" else "audio.m4a")
            processor.convert(item, input, output, cancelled)
            stage(DownloadStatus.SAVING, "Saving to Music/LocalBeat")
            withContext(NonCancellable) {
                check(!cancelled.get())
                val uri = publisher.publish(item, output, processor.inspect(output).second) {
                    item = item.copy(pendingUri = it); dao.put(item)
                }
                item = item.copy(status = DownloadStatus.COMPLETED, stage = "Saved", progress = 1f,
                    finalUri = uri.toString(), pendingUri = null, completedAt = System.currentTimeMillis(), error = null)
                dao.put(item)
                libraryRevision.value += 1
            }
        } catch (e: Exception) {
            withContext(NonCancellable) {
                dao.put(item.copy(status = if (cancelled.get()) DownloadStatus.CANCELLED else DownloadStatus.FAILED,
                    stage = if (cancelled.get()) "Cancelled" else "Failed", progress = null,
                    error = if (cancelled.get()) null else friendlyError(e), pendingUri = null))
            }
        } finally { cancellationWatcher.cancel(); downloader.cancel(item.id); processor.cancel(item.id); dir.deleteRecursively(); activeId = null }
        return true
    }
    companion object {
        @Volatile private var instance: DownloadRepositoryImpl? = null
        fun get(context: Context): DownloadRepositoryImpl = instance ?: synchronized(this) {
            instance ?: DownloadRepositoryImpl(context.applicationContext).also { instance = it }
        }
    }
}
fun friendlyError(e: Exception): String {
    val text = e.message.orEmpty().lowercase()
    return when {
        "initializ" in text -> "Native tools could not initialize. Check free storage and device compatibility."
        "unsupported url" in text -> "This URL or website is unsupported."
        "network" in text || "resolve" in text || "timed out" in text || "connection" in text -> "Network error. Check your connection and retry."
        "space" in text || "enospc" in text -> "Not enough free storage."
        e is SecurityException -> "Storage access was denied. Grant permission or select the video again."
        "audio track" in text -> "This video has no supported audio track."
        "conversion failed" in text -> "Audio conversion failed. Check storage or try another format."
        else -> "Extraction failed. The source may be unavailable, protected, or incompatible. Check storage and try another permitted source."
    }
}
