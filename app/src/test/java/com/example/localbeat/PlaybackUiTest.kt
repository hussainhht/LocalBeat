package com.example.localbeat

import com.example.localbeat.ui.screens.formatSongDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackUiTest {
    @Test fun durationSupportsLongTracksAndUnknownDuration() {
        assertEquals("00:00", formatSongDuration(-1))
        assertEquals("00:00", formatSongDuration(0))
        assertEquals("01:05", formatSongDuration(65999))
        assertEquals("120:00", formatSongDuration(7200000))
    }
}
