package com.example.localbeat.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.localbeat.MainActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)
    private val counter = ListeningCounter()
    private var lastSample = 0L
    private var wasPlaying = false

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        val repository = com.example.localbeat.data.library.LibraryRepository(applicationContext)
        fun sample() {
            val now = android.os.SystemClock.elapsedRealtime()
            val elapsed = if (wasPlaying && lastSample > 0) now - lastSample else 0
            lastSample = now
            wasPlaying = player.isPlaying
            val id = player.currentMediaItem?.mediaId
            if (id != null && counter.advance(elapsed, player.duration)) {
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try { repository.record(id) }
                    catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (e: Exception) { android.util.Log.w("LocalBeat", "Unable to save listening statistics", e) }
                }
            }
        }
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onMediaItemTransition(item: androidx.media3.common.MediaItem?, reason: Int) {
                counter.reset()
                lastSample = android.os.SystemClock.elapsedRealtime()
                wasPlaying = player.isPlaying
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) { sample() }
        })
        scope.launch {
            while (true) { sample(); kotlinx.coroutines.delay(1000) }
        }
        val launch = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, player).setSessionActivity(launch).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        scope.cancel()
        session?.let { it.player.release(); it.release() }
        session = null
        super.onDestroy()
    }
}
