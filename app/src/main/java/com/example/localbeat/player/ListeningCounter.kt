package com.example.localbeat.player

/** One count per media-item visit, after 30 seconds or half a short track of audible playback. */
class ListeningCounter {
    private var elapsed = 0L
    private var counted = false
    fun reset() { elapsed = 0; counted = false }
    fun advance(playingMs: Long, duration: Long): Boolean {
        if (counted || playingMs <= 0 || duration <= 0) return false
        elapsed += playingMs
        if (elapsed < minOf(30_000L, (duration / 2).coerceAtLeast(1))) return false
        counted = true
        return true
    }
}
