package com.example.localbeat.downloader

import android.content.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import com.example.localbeat.data.model.*
import java.io.File
import kotlinx.coroutines.*
import kotlin.coroutines.resume

open class MediaStorePublisher(private val context: Context) {
    open suspend fun publish(item: DownloadItem, file: File, duration: Long, pending: suspend (String) -> Unit): Uri {
        val resolver = context.contentResolver
        val ext = if (item.format == "MP3") "mp3" else "m4a"
        val name = safeAudioName(item.title) + "-" + item.id.take(8) + "." + ext
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.TITLE, item.title)
            put(MediaStore.Audio.Media.MIME_TYPE, if (ext == "mp3") "audio/mpeg" else "audio/mp4")
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            put(MediaStore.Audio.Media.DURATION, duration)
            item.artist?.let { put(MediaStore.Audio.Media.ARTIST, it) }
            item.album?.let { put(MediaStore.Audio.Media.ALBUM, it) }
        }
        if (Build.VERSION.SDK_INT >= 29) {
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/LocalBeat")
            values.put(MediaStore.Audio.Media.IS_PENDING, 1)
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: error("Cannot create music file.")
            try {
                pending(uri.toString())
                resolver.openOutputStream(uri, "w")!!.use { out ->
                    file.inputStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buffer); if (n < 0) break
                            out.write(buffer, 0, n)
                        }
                    }
                }
                currentCoroutineContext().ensureActive()
                check(resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null) == 1)
                return uri
            } catch (e: Exception) { resolver.delete(uri, null, null); throw e }
        }
        @Suppress("DEPRECATION")
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "LocalBeat")
        check(folder.exists() || folder.mkdirs()) { "Cannot access Music folder. Grant storage permission." }
        val destination = File(folder, name)
        try {
            file.inputStream().use { input -> destination.outputStream().use { out -> input.copyTo(out) } }
            currentCoroutineContext().ensureActive()
            return suspendCancellableCoroutine { continuation ->
                MediaScannerConnection.scanFile(context, arrayOf(destination.absolutePath),
                    arrayOf(if (ext == "mp3") "audio/mpeg" else "audio/mp4")) { _, uri ->
                    if (continuation.isActive) {
                        if (uri != null) continuation.resume(uri)
                        else continuation.resumeWith(Result.failure(IllegalStateException("MediaStore indexing failed.")))
                    }
                }
            }
        } catch (e: Exception) { destination.delete(); throw e }
    }
}
