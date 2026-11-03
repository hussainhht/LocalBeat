package com.example.localbeat.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localbeat.data.model.*
import com.example.localbeat.ui.theme.*
import kotlin.math.roundToInt

internal val DownloadItem.isActive: Boolean
    get() = status !in listOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELLED)

@Composable
fun DownloadTaskCard(item: DownloadItem, cancel: () -> Unit, retry: () -> Unit, open: () -> Unit) {
    val context = LocalContext.current
    Surface(shape = MaterialTheme.shapes.medium, color = BeatColors.Elevated, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SongArtwork(item.thumbnail, Modifier.size(58.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(item.stage + " · " + if (item.format == "BEST") "Best / M4A" else item.format,
                        style = MaterialTheme.typography.bodySmall, color = BeatColors.Muted)
                }
                if (item.status == DownloadStatus.COMPLETED) Icon(BeatIcons.Check, "Completed", tint = BeatColors.Highlight)
            }
            if (item.isActive) {
                val progress = item.progress
                if (progress != null) {
                    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(),
                        color = BeatColors.Crimson, trackColor = BeatColors.Border, drawStopIndicator = {})
                    Text("${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = BeatColors.Muted)
                } else LinearProgressIndicator(Modifier.fillMaxWidth(), color = BeatColors.Crimson, trackColor = BeatColors.Border)
                item.downloadedBytes?.let { bytes ->
                    Text(Formatter.formatShortFileSize(context, bytes) + (item.totalBytes?.let { " / " + Formatter.formatShortFileSize(context, it) } ?: ""),
                        style = MaterialTheme.typography.bodySmall, color = BeatColors.Muted)
                }
            }
            item.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = BeatColors.Highlight) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (item.isActive) TextButton(onClick = cancel, enabled = item.status != DownloadStatus.SAVING) {
                    Icon(BeatIcons.Close, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Cancel")
                }
                if (item.status == DownloadStatus.FAILED || item.status == DownloadStatus.CANCELLED)
                    TextButton(onClick = retry) { Icon(BeatIcons.Refresh, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Retry") }
                if (item.finalUri != null) FilledTonalButton(onClick = open) {
                    Icon(BeatIcons.Play, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Play / open")
                }
            }
        }
    }
}
