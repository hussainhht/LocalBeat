package com.example.localbeat.data.model

import android.net.Uri

/**
 * Data model representing a music track.
 * Duration is measured in milliseconds; contentUri identifies the MediaStore item.
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val path: String = "",
    val albumArtUri: String? = null,
    val contentUri: Uri = Uri.EMPTY
)
