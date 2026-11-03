package com.example.localbeat.player

import com.example.localbeat.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interface representing the audio player contract.
 * Future phases will back this with Media3 / ExoPlayer.
 */
interface AudioPlayer {
    val currentSong: StateFlow<Song?>
    val isPlaying: StateFlow<Boolean>
    val currentPositionMs: StateFlow<Long>

    fun play(song: Song)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
}

/**
 * Placeholder implementation of AudioPlayer for Phase 1.
 * Holds in-memory player state for UI testing and placeholders.
 */
class AudioPlayerImpl : AudioPlayer {
    private val _currentSong = MutableStateFlow<Song?>(null)
    override val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    override val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    override fun play(song: Song) {
        _currentSong.value = song
        _isPlaying.value = true
        _currentPositionMs.value = 0L
    }

    override fun pause() {
        _isPlaying.value = false
    }

    override fun resume() {
        if (_currentSong.value != null) {
            _isPlaying.value = true
        }
    }

    override fun stop() {
        _isPlaying.value = false
        _currentSong.value = null
        _currentPositionMs.value = 0L
    }

    override fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
    }
}
