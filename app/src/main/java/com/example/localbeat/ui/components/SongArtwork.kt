package com.example.localbeat.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage

@Composable
fun SongArtwork(uri: String?, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = uri, contentDescription = "Album artwork", modifier = modifier,
        contentScale = ContentScale.Crop,
        loading = { ArtworkFallback() }, error = { ArtworkFallback() },
    )
}

@Composable
private fun ArtworkFallback() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
    }
}
