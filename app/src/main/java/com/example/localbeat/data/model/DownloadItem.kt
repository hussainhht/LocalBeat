package com.example.localbeat.data.model

/**
 * Status of an audio download task.
 */
enum class DownloadStatus {
    IDLE,
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

/**
 * Data model representing a download item.
 * In later phases, this will track yt-dlp / FFmpeg download operations.
 */
data class DownloadItem(
    val id: String,
    val url: String,
    val title: String,
    val progress: Float = 0f,
    val status: DownloadStatus = DownloadStatus.IDLE
)
