package com.example.localbeat.ui.screens

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.localbeat.data.model.Song
import com.example.localbeat.data.repository.MusicRepositoryImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object MusicPermission {
    val name: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED
}

data class SongsUiState(
    val hasPermission: Boolean = false,
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val error: String? = null,
)

class SongsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepositoryImpl(application.contentResolver)
    private val mutableUiState = MutableStateFlow(SongsUiState())
    val uiState = mutableUiState.asStateFlow()
    private var scanJob: Job? = null

    /** Recheck the OS grant on resume and before every scan, including retries. */
    fun refresh() {
        if (!MusicPermission.isGranted(getApplication())) {
            scanJob?.cancel()
            mutableUiState.value = SongsUiState()
            return
        }
        if (scanJob?.isActive == true) return
        mutableUiState.value = SongsUiState(hasPermission = true, isLoading = true)
        scanJob = viewModelScope.launch {
            try {
                repository.getSongs().collect { songs ->
                    mutableUiState.value = SongsUiState(hasPermission = true, songs = songs)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (denied: SecurityException) {
                mutableUiState.value = SongsUiState(
                    error = "Audio access was denied. Please allow access and try again.",
                )
            } catch (exception: Exception) {
                mutableUiState.value = SongsUiState(
                    hasPermission = true,
                    error = "Unable to read your music library. Please try again.",
                )
            }
        }
    }
}
