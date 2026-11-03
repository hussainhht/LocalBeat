package com.example.localbeat.data.repository

import com.example.localbeat.data.model.DownloadItem
import com.example.localbeat.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Repository interface for managing audio downloads.
 */
interface DownloadRepository {
    fun getDownloads(): Flow<List<DownloadItem>>
    suspend fun startDownload(url: String, title: String): DownloadItem
    suspend fun cancelDownload(id: String)
}

/**
 * Placeholder implementation of DownloadRepository for Phase 1.
 * yt-dlp wrapper and FFmpeg integration will be added in subsequent phases.
 */
class DownloadRepositoryImpl : DownloadRepository {
    private val _downloads = MutableStateFlow(
        listOf(
            DownloadItem(
                id = "d1",
                url = "https://example.com/audio/track1",
                title = "Lo-Fi Study Beats Session #1",
                progress = 0.65f,
                status = DownloadStatus.DOWNLOADING
            ),
            DownloadItem(
                id = "d2",
                url = "https://example.com/audio/track2",
                title = "Synthwave Road Trip 2026",
                progress = 1.0f,
                status = DownloadStatus.COMPLETED
            )
        )
    )

    override fun getDownloads(): Flow<List<DownloadItem>> = _downloads.asStateFlow()

    override suspend fun startDownload(url: String, title: String): DownloadItem {
        val newItem = DownloadItem(
            id = System.currentTimeMillis().toString(),
            url = url,
            title = title.ifBlank { "Audio Download #${_downloads.value.size + 1}" },
            progress = 0f,
            status = DownloadStatus.QUEUED
        )
        _downloads.update { current -> current + newItem }
        return newItem
    }

    override suspend fun cancelDownload(id: String) {
        _downloads.update { current -> current.filterNot { it.id == id } }
    }
}
