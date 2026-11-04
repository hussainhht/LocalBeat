package com.example.localbeat

import com.example.localbeat.downloader.*
import com.example.localbeat.data.repository.friendlyError
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class YouTubePolicyTest {
    @Test fun publicVideoShapesAndDirectSignedUrls() {
        val id = "aqz-KE-bpKQ"
        val expected = "https://www.youtube.com/watch?v=$id"
        listOf(expected + "&feature=share", "https://youtu.be/$id?si=share",
            "https://m.youtube.com/watch?v=$id", "https://youtube.com/shorts/$id?feature=share")
            .forEach { assertEquals(expected, canonicalMediaUrl(it)) }
        val direct = "https://example.org/song.m4a?signature=abc&expires=123"
        assertEquals(direct, canonicalMediaUrl(direct))
        assertEquals("https://youtu.be.evil.org/$id", canonicalMediaUrl("https://youtu.be.evil.org/$id"))
    }
    @Test fun audioFormatsDoNotDependOnExtensionOrBitrate() {
        assertTrue(usableAudioFormat(JSONObject("""{"acodec":"opus","vcodec":"none","ext":"webm"}""")))
        assertTrue(usableAudioFormat(JSONObject("""{"acodec":"aac","vcodec":"h264","ext":"mp4"}""")))
        assertTrue(usableAudioFormat(JSONObject("""{"ext":"mp3"}""")))
        assertFalse(usableAudioFormat(JSONObject("""{"acodec":"none"}""")))
        assertFalse(usableAudioFormat(JSONObject("""{"acodec":"aac","has_drm":true}""")))
        assertEquals("bestaudio/best[acodec!=?none]", AUDIO_SELECTION)
    }
    @Test fun categoriesAndSanitization() {
        mapOf("HTTP Error 403: Forbidden" to "403", "HTTP Error 429" to "rate limiting",
            "Sign in to confirm you're not a bot" to "sign-in", "Requested format is not available" to "audio format",
            "Signature extraction failed" to "challenge", "No JavaScript runtime" to "runtime",
            "Missing yt-dlp-ejs" to "solver", "not available in your country" to "region")
            .forEach { (raw, expected) -> assertTrue(friendlyError(Exception(raw)).contains(expected)) }
        val clean = sanitizedDiagnostic("WARNING: failure https://host/x?sig=secret\nAuthorization: secret\nCookie: secret")
        assertFalse(clean.contains("secret"))
        assertTrue(sanitizedDiagnostic("x".repeat(10000)).length <= 6000)
    }
    @Test fun missingNativeFilesAreReported() {
        assertEquals(4, missingNativeTools(File("/nonexistent/localbeat")).size)
        val directory = kotlin.io.path.createTempDirectory("localbeat-runtime").toFile()
        try {
            listOf("libpython.so", "libqjs.so", "libffmpeg.so", "libffprobe.so").forEach {
                File(directory, it).apply { writeText("fixture"); setExecutable(true) }
            }
            assertTrue(missingNativeTools(directory).isEmpty())
            File(directory, "libqjs.so").delete()
            assertEquals(listOf("libqjs.so"), missingNativeTools(directory))
        } finally { directory.deleteRecursively() }
    }
    @Test fun ejsCheckAcceptsOfficialShebangZipLayout() {
        val file = File.createTempFile("extractor", ".zip")
        try {
            file.outputStream().use { output ->
                output.write("#!/usr/bin/env python3\n".toByteArray())
                java.util.zip.ZipOutputStream(output).use { zip ->
                    zip.putNextEntry(java.util.zip.ZipEntry("yt_dlp/__init__.py"))
                    zip.write(ByteArray(300)); zip.closeEntry()
                    zip.putNextEntry(java.util.zip.ZipEntry("yt_dlp_ejs/__init__.py"))
                    zip.write(ByteArray(300)); zip.closeEntry()
                }
            }
            assertTrue(YouTubeRuntime.hasEjs(file))
            file.writeText("not an extractor")
            assertFalse(YouTubeRuntime.hasEjs(file))
        } finally { file.delete() }
    }
}
