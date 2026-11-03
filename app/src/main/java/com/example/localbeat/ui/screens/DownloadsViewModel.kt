package com.example.localbeat.ui.screens

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.*
import com.example.localbeat.data.repository.*
import com.example.localbeat.data.model.*
import com.example.localbeat.downloader.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

enum class PreviewState { EMPTY, VALIDATING, FETCHING_INFO, READY, UNSUPPORTED_URL, NETWORK_ERROR, EXTRACTION_ERROR }
class DownloadsViewModel(app: Application) : AndroidViewModel(app) {
    val repository = DownloadRepositoryImpl.get(app)
    val tasks = repository.getDownloads().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val input = MutableStateFlow("")
    val preview = MutableStateFlow<MediaPreview?>(null)
    val state = MutableStateFlow(PreviewState.EMPTY)
    val error = MutableStateFlow<String?>(null)
    var local = false
        private set
    private var fetch: Job? = null
    private var processId: String? = null
    fun setInput(value: String) {
        processId?.let(repository.downloader::cancel); fetch?.cancel()
        input.value = value; local = false; preview.value = null; state.value = PreviewState.EMPTY; error.value = null
    }
    fun video(uri: Uri) {
        setInput(uri.toString()); local = true
        fetchInfo()
    }
    fun fetchInfo() {
        processId?.let(repository.downloader::cancel); fetch?.cancel()
        val id = UUID.randomUUID().toString(); processId = id
        fetch = viewModelScope.launch {
            state.value = PreviewState.VALIDATING; error.value = null; preview.value = null
            if (!local && !validMediaUrl(input.value)) {
                state.value = PreviewState.UNSUPPORTED_URL; error.value = "Enter an HTTP or HTTPS URL."; return@launch
            }
            state.value = PreviewState.FETCHING_INFO
            try {
                val value = withContext(Dispatchers.IO) {
                    if (!local) repository.downloader.info(input.value, id)
                    else {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(getApplication<Application>(), Uri.parse(input.value))
                            check(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes") { "No audio track" }
                            MediaPreview(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Imported audio",
                                null, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toDoubleOrNull()?.div(1000),
                                "Local video", null, retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM), emptyList(), emptyList())
                        } finally { retriever.release() }
                    }
                }
                preview.value = value; state.value = PreviewState.READY
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                error.value = friendlyError(e)
                state.value = if (error.value!!.startsWith("Network")) PreviewState.NETWORK_ERROR
                    else if (error.value!!.contains("unsupported")) PreviewState.UNSUPPORTED_URL else PreviewState.EXTRACTION_ERROR
            }
        }
    }
    fun enqueue(format: String, quality: Int) {
        val p = preview.value ?: return
        viewModelScope.launch {
            try { repository.enqueue(input.value, p, format, quality, local) }
            catch (e: Exception) { error.value = friendlyError(e) }
        }
    }
    fun cancel(id: String) { viewModelScope.launch { repository.cancelDownload(id) } }
    fun retry(id: String) { viewModelScope.launch { repository.retry(id) } }
    fun clear() { viewModelScope.launch { repository.dao.clear() } }
    override fun onCleared() { processId?.let(repository.downloader::cancel) }
}
