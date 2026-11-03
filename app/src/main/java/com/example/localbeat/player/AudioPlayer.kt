package com.example.localbeat.player

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.localbeat.data.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val queue: List<Song> = emptyList(),
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val connected: Boolean = false,
    val error: String? = null,
)

class AudioPlayer(application: Application) : AndroidViewModel(application) {
    private val mutableState = MutableStateFlow(PlaybackState())
    val state = mutableState.asStateFlow()
    private var controller: MediaController? = null
    private val future = MediaController.Builder(application,
        SessionToken(application, ComponentName(application, PlaybackService::class.java))).buildAsync()
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            mutableState.value = mutableState.value.copy(error = "Unable to play this file: ${error.errorCodeName}")
        }
    }

    init {
        future.addListener({
            try {
                controller = future.get().also { it.addListener(listener) }
                publish()
            } catch (e: Exception) {
                mutableState.value = mutableState.value.copy(error = "Unable to connect to playback")
            }
        }, ContextCompat.getMainExecutor(application))
        viewModelScope.launch {
            while (true) { publish(); delay(500) }
        }
    }

    private fun publish() {
        val p = controller ?: return
        val queue = (0 until p.mediaItemCount).map { p.getMediaItemAt(it).toSong() }
        mutableState.value = mutableState.value.copy(
            currentSong = p.currentMediaItem?.toSong(), isPlaying = p.isPlaying,
            currentPosition = p.currentPosition.coerceAtLeast(0),
            duration = p.duration.takeIf { it > 0 } ?: p.currentMediaItem?.mediaMetadata?.durationMs ?: 0,
            queue = queue, shuffleEnabled = p.shuffleModeEnabled, repeatMode = p.repeatMode,
            connected = true,
        )
    }

    fun play(songs: List<Song>, song: Song) {
        val index = songs.indexOf(song)
        if (index < 0) return
        mutableState.value = mutableState.value.copy(error = null)
        controller?.apply {
            setMediaItems(songs.map { it.toMediaItem() }, index, 0)
            prepare()
            play()
        }
    }
    fun toggle() { controller?.apply {
        if (isPlaying) pause() else {
            if (playbackState == Player.STATE_ENDED) seekToDefaultPosition()
            if (playbackState == Player.STATE_IDLE) prepare()
            play()
        }
    } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun seek(position: Long) { controller?.seekTo(position.coerceIn(0, state.value.duration.coerceAtLeast(0))) }
    fun select(index: Int) { controller?.apply { seekToDefaultPosition(index); play() } }
    fun shuffle() { controller?.apply { shuffleModeEnabled = !shuffleModeEnabled } }
    fun repeat() { controller?.apply { repeatMode = when (repeatMode) {
        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
        else -> Player.REPEAT_MODE_OFF
    } } }

    override fun onCleared() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(future)
        controller = null
        super.onCleared()
    }
}

private fun Song.toMediaItem() = MediaItem.Builder().setMediaId(id).setUri(contentUri)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album)
        .setArtworkUri(albumArtUri?.let(Uri::parse)).setDurationMs(durationMs).build()).build()

private fun MediaItem.toSong() = Song(
    id = mediaId, title = mediaMetadata.title?.toString().orEmpty(),
    artist = mediaMetadata.artist?.toString().orEmpty(),
    album = mediaMetadata.albumTitle?.toString().orEmpty(),
    durationMs = mediaMetadata.durationMs ?: 0,
    albumArtUri = mediaMetadata.artworkUri?.toString(),
    contentUri = localConfiguration?.uri ?: Uri.EMPTY,
)
