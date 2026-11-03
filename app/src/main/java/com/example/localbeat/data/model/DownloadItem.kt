package com.example.localbeat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus { QUEUED, FETCHING_INFO, DOWNLOADING, PROCESSING, SAVING, COMPLETED, FAILED, CANCELLED }
@Entity
data class DownloadItem(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val thumbnail: String? = null,
    val format: String = "M4A",
    val quality: Int = 0,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float? = null,
    val downloadedBytes: Long? = null,
    val totalBytes: Long? = null,
    val stage: String = "Queued",
    val error: String? = null,
    val finalUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val artist: String? = null,
    val album: String? = null,
    val local: Boolean = false,
    val pendingUri: String? = null
)
fun safeAudioName(title: String): String = title.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
    .trim().trim('.').take(100).ifBlank { "Audio" }
fun validMediaUrl(value: String): Boolean = runCatching {
    val uri = java.net.URI(value)
    uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank() && uri.rawUserInfo == null
}.getOrDefault(false)
