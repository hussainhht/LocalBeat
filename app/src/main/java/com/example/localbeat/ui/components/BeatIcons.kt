package com.example.localbeat.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Original 24dp line icons; no external artwork or icon dependency. */
object BeatIcons {
    private fun line(name: String, mirror: Boolean = false, block: PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = mirror).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = block)
        }.build()
    private fun fill(name: String, block: PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black), pathBuilder = block)
        }.build()

    val Home = line("Home") { moveTo(3f,10f); lineTo(12f,3f); lineTo(21f,10f); verticalLineTo(21f); horizontalLineTo(15f); verticalLineTo(14f); horizontalLineTo(9f); verticalLineTo(21f); horizontalLineTo(3f); close() }
    val Search = line("Search") { moveTo(16.8f,16.8f); lineTo(21f,21f); moveTo(19f,10.5f); curveTo(19f,15.2f,15.2f,19f,10.5f,19f); curveTo(5.8f,19f,2f,15.2f,2f,10.5f); curveTo(2f,5.8f,5.8f,2f,10.5f,2f); curveTo(15.2f,2f,19f,5.8f,19f,10.5f); close() }
    val Library = line("Library") { moveTo(4f,4f); verticalLineTo(21f); moveTo(9f,4f); verticalLineTo(21f); moveTo(14f,4f); lineTo(20f,3f); lineTo(23f,20f); lineTo(17f,21f); close() }
    val Download = line("Download") { moveTo(12f,3f); verticalLineTo(15f); moveTo(7f,10f); lineTo(12f,15f); lineTo(17f,10f); moveTo(4f,16f); verticalLineTo(21f); horizontalLineTo(20f); verticalLineTo(16f) }
    val Music = line("Music") { moveTo(9f,17f); verticalLineTo(5f); lineTo(20f,3f); verticalLineTo(15f); moveTo(9f,17f); curveTo(9f,21f,3f,22f,3f,18f); curveTo(3f,15f,9f,14f,9f,17f); moveTo(20f,15f); curveTo(20f,19f,14f,20f,14f,16f); curveTo(14f,13f,20f,12f,20f,15f) }
    val Album = line("Album") { moveTo(21f,12f); curveTo(21f,24f,3f,24f,3f,12f); curveTo(3f,0f,21f,0f,21f,12f); close(); moveTo(14f,12f); curveTo(14f,15f,10f,15f,10f,12f); curveTo(10f,9f,14f,9f,14f,12f); close() }
    val Artist = line("Artist") { moveTo(16f,7f); curveTo(16f,12.5f,8f,12.5f,8f,7f); curveTo(8f,1.5f,16f,1.5f,16f,7f); close(); moveTo(4f,21f); curveTo(4f,12f,20f,12f,20f,21f) }
    val Playlist = line("Playlist") { moveTo(3f,5f); horizontalLineTo(20f); moveTo(3f,11f); horizontalLineTo(13f); moveTo(3f,17f); horizontalLineTo(10f); moveTo(17f,11f); verticalLineTo(20f); moveTo(13f,16f); lineTo(17f,20f); lineTo(21f,16f) }
    private val heart: PathBuilder.() -> Unit = { moveTo(12f,21f); curveTo(8f,17f,2f,13f,2f,8f); curveTo(2f,2f,9f,1f,12f,6f); curveTo(15f,1f,22f,2f,22f,8f); curveTo(22f,13f,16f,17f,12f,21f); close() }
    val Heart = line("Favorite", block = heart)
    val HeartFilled = fill("Favorited", heart)
    val Play = fill("Play") { moveTo(8f,4f); lineTo(21f,12f); lineTo(8f,20f); close() }
    val Pause = fill("Pause") { moveTo(6f,4f); horizontalLineTo(10f); verticalLineTo(20f); horizontalLineTo(6f); close(); moveTo(14f,4f); horizontalLineTo(18f); verticalLineTo(20f); horizontalLineTo(14f); close() }
    val Next = fill("Next") { moveTo(4f,5f); lineTo(16f,12f); lineTo(4f,19f); close(); moveTo(17f,5f); horizontalLineTo(20f); verticalLineTo(19f); horizontalLineTo(17f); close() }
    val Previous = fill("Previous") { moveTo(20f,5f); lineTo(8f,12f); lineTo(20f,19f); close(); moveTo(4f,5f); horizontalLineTo(7f); verticalLineTo(19f); horizontalLineTo(4f); close() }
    val Shuffle = line("Shuffle") { moveTo(3f,5f); horizontalLineTo(6f); curveTo(11f,5f,13f,19f,18f,19f); horizontalLineTo(21f); moveTo(18f,16f); lineTo(21f,19f); lineTo(18f,22f); moveTo(3f,19f); horizontalLineTo(6f); curveTo(11f,19f,13f,5f,18f,5f); horizontalLineTo(21f); moveTo(18f,2f); lineTo(21f,5f); lineTo(18f,8f) }
    val Repeat = line("Repeat") { moveTo(4f,10f); verticalLineTo(7f); curveTo(4f,5f,6f,4f,8f,4f); horizontalLineTo(20f); moveTo(17f,1f); lineTo(20f,4f); lineTo(17f,7f); moveTo(20f,14f); verticalLineTo(17f); curveTo(20f,19f,18f,20f,16f,20f); horizontalLineTo(4f); moveTo(7f,17f); lineTo(4f,20f); lineTo(7f,23f) }
    val Queue = line("Queue", true) { moveTo(3f,5f); horizontalLineTo(21f); moveTo(3f,11f); horizontalLineTo(21f); moveTo(3f,17f); horizontalLineTo(12f); moveTo(17f,15f); lineTo(22f,18f); lineTo(17f,21f); close() }
    val More = fill("More") { repeat(3) { val y = 5f + it * 7f; moveTo(14f,y); curveTo(14f,y+2.6f,10f,y+2.6f,10f,y); curveTo(10f,y-2.6f,14f,y-2.6f,14f,y); close() } }
    val Back = line("Back", true) { moveTo(15f,4f); lineTo(7f,12f); lineTo(15f,20f) }
    val Down = line("Collapse") { moveTo(5f,9f); lineTo(12f,16f); lineTo(19f,9f) }
    val Close = line("Close") { moveTo(6f,6f); lineTo(18f,18f); moveTo(18f,6f); lineTo(6f,18f) }
    val Plus = line("Add") { moveTo(12f,4f); verticalLineTo(20f); moveTo(4f,12f); horizontalLineTo(20f) }
    val Sort = line("Sort") { moveTo(3f,6f); horizontalLineTo(21f); moveTo(6f,12f); horizontalLineTo(18f); moveTo(9f,18f); horizontalLineTo(15f) }
    val Edit = line("Rename") { moveTo(3f,21f); lineTo(4f,15f); lineTo(17f,2f); lineTo(22f,7f); lineTo(9f,20f); close(); moveTo(14f,5f); lineTo(19f,10f) }
    val Delete = line("Delete") { moveTo(3f,6f); horizontalLineTo(21f); moveTo(9f,6f); verticalLineTo(3f); horizontalLineTo(15f); verticalLineTo(6f); moveTo(6f,6f); lineTo(7f,21f); horizontalLineTo(17f); lineTo(18f,6f); moveTo(10f,10f); verticalLineTo(17f); moveTo(14f,10f); verticalLineTo(17f) }
    val Refresh = line("Refresh") { moveTo(20f,9f); curveTo(17f,-1f,3f,2f,3f,12f); curveTo(3f,22f,18f,25f,21f,15f); moveTo(20f,3f); verticalLineTo(9f); horizontalLineTo(14f) }
    val Link = line("Link") { moveTo(9f,15f); lineTo(15f,9f); moveTo(8f,12f); curveTo(2f,8f,7f,2f,11f,5f); moveTo(12f,8f); curveTo(16f,1f,24f,7f,19f,12f); lineTo(16f,15f); moveTo(12f,16f); curveTo(8f,23f,0f,17f,5f,12f) }
    val Video = line("Video") { moveTo(3f,5f); horizontalLineTo(16f); verticalLineTo(19f); horizontalLineTo(3f); close(); moveTo(16f,9f); lineTo(22f,6f); verticalLineTo(18f); lineTo(16f,15f) }
    val Check = line("Done") { moveTo(4f,12f); lineTo(9f,17f); lineTo(20f,6f) }
    val Info = line("Information") { moveTo(21f,12f); curveTo(21f,24f,3f,24f,3f,12f); curveTo(3f,0f,21f,0f,21f,12f); close(); moveTo(12f,11f); verticalLineTo(17f); moveTo(12f,7f); verticalLineTo(7.2f) }
    val Clock = line("History") { moveTo(21f,12f); curveTo(21f,24f,3f,24f,3f,12f); curveTo(3f,0f,21f,0f,21f,12f); close(); moveTo(12f,6f); verticalLineTo(12f); lineTo(16f,14f) }
}
