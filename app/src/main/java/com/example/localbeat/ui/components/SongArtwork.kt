package com.example.localbeat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.localbeat.data.model.Song
import com.example.localbeat.ui.theme.BeatColors
import com.example.localbeat.ui.theme.BeatDimens

enum class CoverKind { Music, Playlist, Favorites, Artist }

/** The original waveform fallback also remains visible while Coil loads. */
@Composable
fun SongArtwork(uri: String?, modifier: Modifier = Modifier, kind: CoverKind = CoverKind.Music) {
    Box(modifier.clip(if (kind == CoverKind.Artist) CircleShape else RoundedCornerShape(BeatDimens.ArtworkRadius))
        .background(Brush.linearGradient(listOf(BeatColors.Wine, BeatColors.Surface, BeatColors.Background))),
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = size.minDimension
            drawCircle(Color.White.copy(alpha = 0.035f), unit * 0.42f,
                center = Offset(size.width * 0.82f, size.height * 0.15f), style = Stroke(unit * 0.08f))
            drawCircle(BeatColors.Crimson.copy(alpha = 0.12f), unit * 0.38f,
                center = Offset(size.width * 0.16f, size.height * 0.88f), style = Stroke(unit * 0.008f))
            if (kind == CoverKind.Music) {
                listOf(.18f, .32f, .50f, .38f, .23f).forEachIndexed { index, h ->
                    val x = size.width * .5f + (index - 2) * unit * .09f
                    drawLine(if (index == 2) BeatColors.Highlight else BeatColors.Text.copy(alpha = .85f),
                        Offset(x, size.height * .5f - unit * h / 2),
                        Offset(x, size.height * .5f + unit * h / 2), unit * .045f, StrokeCap.Round)
                }
            }
        }
        if (kind != CoverKind.Music) Icon(
            when (kind) { CoverKind.Playlist -> BeatIcons.Playlist; CoverKind.Favorites -> BeatIcons.HeartFilled; else -> BeatIcons.Artist },
            contentDescription = null, tint = BeatColors.Text, modifier = Modifier.fillMaxSize(.38f))
        if (uri != null && kind != CoverKind.Artist)
            AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun PlaylistCover(songs: List<Song>, modifier: Modifier = Modifier, favorite: Boolean = false) {
    if (favorite || songs.size < 4) {
        SongArtwork(if (favorite) null else songs.firstOrNull()?.albumArtUri, modifier,
            if (favorite) CoverKind.Favorites else CoverKind.Playlist)
    } else {
        Column(modifier.clip(RoundedCornerShape(BeatDimens.ArtworkRadius))) {
            repeat(2) { row ->
                Row(Modifier.weight(1f)) {
                    repeat(2) { col -> SongArtwork(songs[row * 2 + col].albumArtUri, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
    }
}
