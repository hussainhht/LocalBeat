package com.example.localbeat.downloader

import com.example.localbeat.data.model.DownloadItem
import com.example.localbeat.data.model.DownloadStatus

/**
 * Interface representing the audio downloader contract.
 * Future phases will integrate yt-dlp and FFmpeg wrappers.
 */
interface AudioDownloader {
    fun enqueue(url: String, title: String): DownloadItem
    fun cancel(downloadId: String)
}

/**
 * Placeholder implementation of AudioDownloader for Phase 1.
 */
class AudioDownloaderImpl : AudioDownloader {
    override fun enqueue(url: String, title: String): DownloadItem {
        return DownloadItem(
            id = System.currentTimeMillis().toString(),
            url = url,
            title = title,
            progress = 0f,
            status = DownloadStatus.QUEUED
        )
    }

    override fun cancel(downloadId: String) {
        // Will be connected to process cancellation in downloader integration
    }
}
