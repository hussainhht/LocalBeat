package com.example.localbeat.downloader

import android.content.Context
import com.yausername.youtubedl_android.*
import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/** Pinned upstream zipimport artifact, never a desktop native executable or arbitrary update channel. */
object YouTubeRuntime {
    const val VERSION = "2026.10.26"
    const val SHA256 = "e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3"
    private var checked = false
    private val lock = ReentrantReadWriteLock()
    fun <T> withExtractor(block: () -> T): T = lock.read(block)

    @Synchronized fun prepare(context: Context) = lock.write { prepareLocked(context) }

    private fun prepareLocked(context: Context) {
        if (checked) return
        val missing = missingNativeTools(File(context.applicationInfo.nativeLibraryDir))
        check(missing.isEmpty()) { "Native tools could not initialize: missing executable(s) $missing" }
        val installed = File(context.noBackupFilesDir, "youtubedl-android/yt-dlp/yt-dlp")
        val interrupted = File(installed.parentFile, "previous")
        if (interrupted.isFile) {
            interrupted.copyTo(installed, overwrite = true)
            interrupted.delete()
        }
        val current = version()
        extractionDiagnostic(context, "runtime", "yt-dlp=$current; QuickJS=" +
            File(context.applicationInfo.nativeLibraryDir, "libqjs.so").canExecute())
        if (current != VERSION) {
            val candidate = File(installed.parentFile, "verified-update")
            val backup = File(installed.parentFile, "previous")
            try {
                val connection = URL("https://github.com/yt-dlp/yt-dlp/releases/download/$VERSION/yt-dlp").openConnection()
                connection.connectTimeout = 10000; connection.readTimeout = 10000
                val digest = MessageDigest.getInstance("SHA-256")
                connection.getInputStream().use { input -> candidate.outputStream().use { output ->
                    val buffer = ByteArray(65536)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer); if (count < 0) break
                        total += count
                        check(total <= 8 * 1024 * 1024) { "Extractor update exceeded size limit" }
                        digest.update(buffer, 0, count); output.write(buffer, 0, count)
                    }
                } }
                check(digest.digest().joinToString("") { "%02x".format(it) } == SHA256) { "Extractor update checksum mismatch" }
                check(hasEjs(candidate)) { "Missing yt-dlp-ejs in verified update" }
                installed.copyTo(backup, overwrite = true)
                try {
                    check(candidate.renameTo(installed)) { "Cannot install verified extractor update" }
                    check(version() == VERSION) { "Extractor update is incompatible with packaged Python" }
                    val help = YoutubeDL.execute(YoutubeDLRequest("").apply { addOption("--help") }).out
                    check("--js-runtimes" in help && "--check-formats" in help) { "Extractor options incompatible" }
                } catch (e: Exception) {
                    backup.copyTo(installed, overwrite = true)
                    throw e
                }
            } catch (e: Exception) {
                extractionDiagnostic(context, "update failed; retaining previous extractor", e.toString())
            } finally { candidate.delete(); backup.delete() }
        }
        extractionDiagnostic(context, "runtime", "active yt-dlp=${version()}; bundled EJS=${hasEjs(installed)}")
        checked = true
    }

    fun version(): String = YoutubeDL.execute(YoutubeDLRequest("").apply { addOption("--version") }).out.trim()
    fun hasEjs(file: File): Boolean = runCatching {
        // Official zipimport executables have a shebang before the ZIP local header.
        val prefix = ByteArray(256)
        file.inputStream().use { java.io.DataInputStream(it).readFully(prefix) }
        val offset = (0..prefix.size - 4).first { index ->
            prefix[index] == 0x50.toByte() && prefix[index + 1] == 0x4b.toByte() &&
                prefix[index + 2] == 3.toByte() && prefix[index + 3] == 4.toByte()
        }
        file.inputStream().use { input ->
            check(input.skip(offset.toLong()) == offset.toLong())
            java.util.zip.ZipInputStream(input).use { zip ->
                var found = false
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name.startsWith("yt_dlp_ejs/")) { found = true; break }
                    entry = zip.nextEntry
                }
                found
            }
        }
    }.getOrDefault(false)
}
