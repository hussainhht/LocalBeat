package com.example.localbeat.downloader

import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.URI

/** Keep signed non-YouTube URLs intact. Only canonicalize known public video URL shapes. */
fun canonicalMediaUrl(value: String): String {
    val trimmed = value.trim()
    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return trimmed
    val host = uri.host?.lowercase() ?: return trimmed
    val id = when {
        host == "youtu.be" -> uri.path.removePrefix("/").substringBefore("/")
        host in setOf("youtube.com", "www.youtube.com", "m.youtube.com") ->
            if (uri.path.startsWith("/shorts/")) uri.path.removePrefix("/shorts/").substringBefore("/")
            else if (uri.path == "/watch") uri.rawQuery.orEmpty().split("&")
                .firstOrNull { it.startsWith("v=") }?.removePrefix("v=") else null
        else -> null
    }
    return if (id != null && Regex("[A-Za-z0-9_-]{11}").matches(id))
        "https://www.youtube.com/watch?v=$id" else trimmed
}

fun usableAudioFormat(format: JSONObject): Boolean =
    !format.optBoolean("has_drm") && format.optString("acodec", "unknown") != "none" &&
        format.optString("protocol") !in setOf("mhtml", "images")

// Prefer audio-only. Unknown codecs on direct URLs are inspected by FFmpeg after transfer.
const val AUDIO_SELECTION = "bestaudio/best[acodec!=?none]"
fun missingNativeTools(directory: File): List<String> =
    listOf("libpython.so", "libqjs.so", "libffmpeg.so", "libffprobe.so")
        .filter { !File(directory, it).let { file -> file.isFile && file.canExecute() } }

/** No raw URLs, headers or cookie lines ever enter application diagnostics. */
fun sanitizedDiagnostic(value: String): String = value.lineSequence()
    .filterNot { Regex("(?i)cookie|authorization|bearer|password|token|header").containsMatchIn(it) }
    .joinToString("\n")
    .replace(Regex("https?://[^\\s\\]<>\\\"']+"), "[URL]")
    .takeLast(6000)

fun extractionDiagnostic(context: android.content.Context, stage: String, value: String) {
    if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
        sanitizedDiagnostic(value).chunked(2000).forEach { Log.d("LocalBeatDownload", "$stage: $it") }
}
