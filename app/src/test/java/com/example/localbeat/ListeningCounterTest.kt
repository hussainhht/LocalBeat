package com.example.localbeat

import com.example.localbeat.player.ListeningCounter
import org.junit.Assert.*
import org.junit.Test

class ListeningCounterTest {
    @Test fun countsOncePerVisitAndResetsForRepeat() {
        val counter = ListeningCounter()
        assertFalse(counter.advance(29_000, 200_000))
        assertTrue(counter.advance(1_000, 200_000))
        assertFalse(counter.advance(60_000, 200_000))
        counter.reset()
        assertTrue(counter.advance(30_000, 200_000))
    }
    @Test fun shortTracksUseHalfDurationAndPausedTimeDoesNotCount() {
        val counter = ListeningCounter()
        assertFalse(counter.advance(0, 10_000))
        assertFalse(counter.advance(4_000, 10_000))
        assertTrue(counter.advance(1_000, 10_000))
        counter.reset()
        assertFalse(counter.advance(50_000, -1))
    }
}
