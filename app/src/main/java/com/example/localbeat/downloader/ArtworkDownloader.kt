package com.example.localbeat.downloader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/** Bounded network and bitmap allocation; normalize WebP and other decodable images to JPEG. */
internal fun downloadArtwork(url: String?, directory: File, cancelled: AtomicBoolean): File? {
    if (url.isNullOrBlank()) return null
    val raw = File(directory, "cover.download")
    val jpeg = File(directory, "cover.jpg")
    try {
        var current = URL(url)
        for (redirect in 0..5) {
            require(current.protocol in listOf("http", "https") && current.userInfo == null)
            check(!cancelled.get())
            val connection = current.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = false
            try {
                val status = connection.responseCode
                if (status in listOf(301, 302, 303, 307, 308)) {
                    require(redirect < 5)
                    current = URL(current, connection.getHeaderField("Location") ?: error("Missing redirect"))
                    continue
                }
                require(status in 200..299)
                require(connection.contentLengthLong <= 8 * 1024 * 1024)
                val deadline = System.nanoTime() + 15_000_000_000L
                connection.inputStream.use { input -> raw.outputStream().use { output ->
                    val buffer = ByteArray(16384)
                    var total = 0
                    while (true) {
                        check(!cancelled.get() && System.nanoTime() < deadline)
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= 8 * 1024 * 1024)
                        output.write(buffer, 0, count)
                    }
                } }
                require(raw.length() > 0)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(raw.absolutePath, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0)
                val options = BitmapFactory.Options()
                while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize.coerceAtLeast(1) > 1024)
                    options.inSampleSize = options.inSampleSize.coerceAtLeast(1) * 2
                val bitmap = BitmapFactory.decodeFile(raw.absolutePath, options) ?: error("Invalid image")
                try { jpeg.outputStream().use { require(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) } }
                finally { bitmap.recycle() }
                require(jpeg.length() > 0)
                return jpeg
            } finally { connection.disconnect() }
        }
    } catch (e: Exception) {
        if (!cancelled.get()) Log.d("LocalBeat", "Artwork download skipped")
    } finally { raw.delete() }
    jpeg.delete()
    return null
}
