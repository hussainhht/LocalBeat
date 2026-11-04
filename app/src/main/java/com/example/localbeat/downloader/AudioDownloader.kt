package com.example.localbeat.downloader

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.example.localbeat.data.model.*
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.*
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*

data class MediaPreview(val title: String, val thumbnail: String?, val duration: Double?,
    val source: String, val uploader: String?, val artist: String?, val album: String?,
    val formats: List<String>, val bitrates: List<Int>)

interface AudioDownloader {
    fun info(url: String, id: String): MediaPreview
    fun download(item: DownloadItem, directory: File, callback: (Float?, Long?, Long?) -> Unit): File
    fun cancel(id: String)
}

/** Native APIs are isolated here. Arguments are passed as arrays, never through a shell. */
class AudioDownloaderImpl(private val context: Context) : AudioDownloader {
    private fun init() {
        try { YoutubeDL.init(context); FFmpeg.init(context) }
        catch (e: Exception) {
            extractionDiagnostic(context, "initialization", e.toString())
            throw IllegalStateException("Native audio tools could not initialize.", e)
        }
    }
    private fun request(url: String): YoutubeDLRequest {
        require(validMediaUrl(url)) { "Enter an HTTP or HTTPS URL without credentials." }
        val canonical = canonicalMediaUrl(url)
        if (canonical.startsWith("https://www.youtube.com/watch?v=")) YouTubeRuntime.prepare(context)
        return YoutubeDLRequest(canonical).apply {
            addOption("--no-playlist"); addOption("--no-config"); addOption("--socket-timeout", "30")
            addOption("--retries", "3")
            if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
                addOption("--verbose")
        }
    }
    override fun info(url: String, id: String): MediaPreview {
        init()
        val req = request(url).apply { addOption("--dump-single-json"); addOption("--skip-download") }
        val json = JSONObject(execute(req, id, "metadata").out)
        require(!json.optBoolean("is_live") && !json.optBoolean("has_drm")) { "Live or protected media is unsupported." }
        val formats = json.optJSONArray("formats")
        // Direct media often puts its sole format on the root object.
        val candidates = if (formats != null && formats.length() > 0)
            (0 until formats.length()).map { formats.getJSONObject(it) } else listOf(json)
        val audio = candidates.filter(::usableAudioFormat)
        require(audio.isNotEmpty()) { "No supported unprotected audio stream found." }
        fun field(key: String) = json.optString(key).takeIf { it.isNotBlank() && it != "null" }
        return MediaPreview(field("title") ?: "Audio", field("thumbnail"), json.optDouble("duration").takeIf { it.isFinite() },
            field("extractor_key") ?: "Website", field("uploader"), field("artist"), field("album"),
            audio.map { format ->
                listOf("ext", "acodec").joinToString(" / ") { key ->
                    format.optString(key).takeIf { it.isNotBlank() && it != "null" } ?: "$key unreported"
                }
            }.distinct(),
            audio.mapNotNull { it.optDouble("abr").takeIf { n -> n.isFinite() && n > 0 }?.toInt() }.distinct().sorted())
    }
    override fun download(item: DownloadItem, directory: File, callback: (Float?, Long?, Long?) -> Unit): File {
        init()
        val req = request(item.url).apply {
            addOption("-f", AUDIO_SELECTION); addOption("--check-formats"); addOption("--no-part")
            addOption("--downloader", "native")
            addOption("-o", File(directory, "source.%(ext)s").absolutePath)
            addOption("--newline")
        }
        execute(req, item.id, "transfer") { percent, _, line ->
            val processing = line.startsWith("[Extract") || line.startsWith("[Merger")
            callback(if (!processing && percent in 0f..100f) percent / 100f else null, null, null)
        }
        return directory.listFiles()!!.single { it.name.startsWith("source.") && !it.name.endsWith(".part") }
    }
    private fun execute(req: YoutubeDLRequest, id: String, stage: String,
        callback: ((Float, Long, String) -> Unit)? = null): YoutubeDLResponse {
        try {
            val response = YouTubeRuntime.withExtractor { YoutubeDL.execute(req, id, callback) }
            extractionDiagnostic(context, stage, response.err)
            return response
        } catch (e: Exception) {
            extractionDiagnostic(context, stage, e.message.orEmpty())
            throw e
        }
    }
    override fun cancel(id: String) { YoutubeDL.destroyProcessById(id) }
}

open class AudioProcessor(private val context: Context) {
    private val processes = ConcurrentHashMap<String, Process>()
    private fun nativeProcess(binary: String, arguments: List<String>): Process =
        ProcessBuilder(listOf(File(context.applicationInfo.nativeLibraryDir, binary).absolutePath) + arguments)
            .redirectErrorStream(true).apply {
                environment()["LD_LIBRARY_PATH"] = listOf(
                    File(context.noBackupFilesDir, "youtubedl-android/packages/python/usr/lib").absolutePath,
                    File(context.noBackupFilesDir, "youtubedl-android/packages/ffmpeg/usr/lib").absolutePath,
                    context.applicationInfo.nativeLibraryDir
                ).joinToString(":")
            }.start()
    open fun cancel(id: String) { processes.remove(id)?.destroy() }
    open fun inspect(file: File): Pair<String, Long> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) return mime to
                    (if (f.containsKey(MediaFormat.KEY_DURATION)) f.getLong(MediaFormat.KEY_DURATION) / 1000 else 0)
            }
            error("This file has no supported audio track.")
        } finally { extractor.release() }
    }
    open fun convert(item: DownloadItem, input: File, output: File, cancelled: AtomicBoolean) {
        try { YoutubeDL.init(context); FFmpeg.init(context) }
        catch (e: Exception) { error("Native audio tools could not initialize.") }
        check(!cancelled.get()) { "Cancelled" }
        val probe = nativeProcess("libffprobe.so", listOf("-v", "error", "-select_streams", "a:0",
            "-show_entries", "stream=codec_name", "-of", "default=noprint_wrappers=1:nokey=1", input.absolutePath))
        processes[item.id] = probe
        val codec = try {
            if (cancelled.get()) probe.destroy()
            val result = probe.inputStream.bufferedReader().use { it.readText().take(4096).trim() }
            check(probe.waitFor() == 0 && result.isNotBlank() && !cancelled.get()) { "This file has no supported audio track." }
            result
        } finally { processes.remove(item.id); probe.destroy() }
        val copy = item.format != "MP3" && codec == "aac" && item.quality == 0
        val args = mutableListOf(File(context.applicationInfo.nativeLibraryDir, "libffmpeg.so").absolutePath,
            "-nostdin", "-v", "error", "-y", "-i", input.absolutePath, "-map", "0:a:0", "-vn")
        args += listOf("-c:a", if (copy) "copy" else if (item.format == "MP3") "libmp3lame" else "aac")
        if (!copy) args += if (item.quality > 0) listOf("-b:a", "${item.quality}k")
            else if (item.format == "MP3") listOf("-q:a", "2") else listOf("-b:a", "192k")
        args += listOf("-metadata", "title=${item.title}")
        item.artist?.let { args += listOf("-metadata", "artist=$it") }
        item.album?.let { args += listOf("-metadata", "album=$it") }
        args += output.absolutePath
        check(!cancelled.get()) { "Cancelled" }
        val process = nativeProcess("libffmpeg.so", args.drop(1))
        processes[item.id] = process
        try {
            if (cancelled.get()) process.destroy()
            // Bounded diagnostics stay inside the downloader; UI uses sanitized error categories.
            val diagnostics = StringBuilder()
            process.inputStream.bufferedReader().useLines { lines -> lines.forEach {
                diagnostics.append(it).append('\n')
                if (diagnostics.length > 4096) diagnostics.delete(0, diagnostics.length - 4096)
            } }
            check(process.waitFor() == 0 && !cancelled.get()) { "Audio conversion failed: $diagnostics" }
            check(output.length() > 0 && inspect(output).second > 0) { "The converted audio is empty or invalid." }
        } finally { processes.remove(item.id); process.destroy() }
    }
}
