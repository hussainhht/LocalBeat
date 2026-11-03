package com.example.localbeat.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localbeat.data.model.*
import com.example.localbeat.ui.components.*
import com.example.localbeat.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadsScreen(modifier: Modifier = Modifier, model: DownloadsViewModel = viewModel(),
    shared: String? = null, sharedVideo: Boolean = false) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val tasks by model.tasks.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf(model.input.value) }
    fun updateInput(value: String) { input = value; model.setInput(value) }
    val preview by model.preview.collectAsStateWithLifecycle()
    val state by model.state.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    var format by rememberSaveable { mutableStateOf("M4A") }
    var quality by rememberSaveable { mutableIntStateOf(0) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var handledShare by rememberSaveable { mutableStateOf<String?>(null) }
    var historyFilter by rememberSaveable { mutableStateOf("All") }
    var details by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(Unit) {
        if (input != model.input.value) {
            if (input.startsWith("content://")) model.video(Uri.parse(input)) else model.setInput(input)
        }
    }
    LaunchedEffect(shared, sharedVideo) {
        if (shared != null && shared != handledShare) {
            handledShare = shared; input = shared
            if (sharedVideo) model.video(Uri.parse(shared)) else model.setInput(shared)
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            input = it.toString(); model.video(it)
        }
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (Build.VERSION.SDK_INT > 28 || result[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true) model.enqueue(format, quality)
        else message = "Storage permission is required to save music on this Android version."
    }
    fun start() {
        focus.clearFocus()
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            needed += Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            needed += Manifest.permission.POST_NOTIFICATIONS
        if (needed.isEmpty()) model.enqueue(format, quality) else permissions.launch(needed.toTypedArray())
    }
    val active = tasks.filter { it.isActive }
    val history = tasks.filter { !it.isActive }.filter {
        when (historyFilter) { "Completed" -> it.status == DownloadStatus.COMPLETED
            "Failed" -> it.status == DownloadStatus.FAILED; else -> true }
    }
    LazyColumn(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(BeatColors.Wine.copy(alpha = .5f), BeatColors.Background)))
                .padding(horizontal = BeatDimens.Gutter).padding(top = 20.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Downloads", style = MaterialTheme.typography.headlineLarge)
                Text("Bring more music home.", color = BeatColors.Muted)
            }
        }
        item {
            Surface(onClick = { focus.clearFocus(); picker.launch(arrayOf("video/*")) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = BeatDimens.Gutter),
                shape = MaterialTheme.shapes.medium, color = BeatColors.Surface, contentColor = BeatColors.Text) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(BeatIcons.Video, null, tint = BeatColors.Highlight, modifier = Modifier.size(30.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Import from a video", style = MaterialTheme.typography.titleMedium)
                        Text("Extract audio from a file on your device", style = MaterialTheme.typography.bodySmall, color = BeatColors.Muted)
                    }
                    Icon(BeatIcons.Plus, null, tint = BeatColors.Muted)
                }
            }
        }
        item {
            Surface(modifier = Modifier.padding(horizontal = BeatDimens.Gutter).fillMaxWidth(), shape = MaterialTheme.shapes.large, color = BeatColors.Elevated) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(BeatIcons.Link, null, tint = BeatColors.Highlight)
                        Text("New download", style = MaterialTheme.typography.titleLarge)
                    }
                    OutlinedTextField(if (model.local) preview?.title ?: "Video selected" else input, ::updateInput,
                        readOnly = model.local, label = { Text(if (model.local) "Selected video" else "Media URL") },
                        placeholder = { Text("Paste an audio or video link") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { focus.clearFocus(); model.fetchInfo() }),
                        trailingIcon = { if (input.isNotBlank()) IconButton(onClick = { updateInput("") }) { Icon(BeatIcons.Close, "Clear media URL") } })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { clipboard.getText()?.text?.let(::updateInput) }, modifier = Modifier.weight(1f)) { Text("Paste") }
                        Button(onClick = { focus.clearFocus(); model.fetchInfo() }, modifier = Modifier.weight(1f),
                            enabled = input.isNotBlank() && state != PreviewState.FETCHING_INFO && state != PreviewState.VALIDATING) { Text("Fetch info") }
                    }
                    if (state == PreviewState.FETCHING_INFO || state == PreviewState.VALIDATING) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Reading media information…", color = BeatColors.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    error?.let { Text(it, color = BeatColors.Highlight, style = MaterialTheme.typography.bodyMedium) }
                    message?.let {
                        Text(it, color = BeatColors.Highlight, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { message = null }) { Text("Dismiss") }
                    }
                    Text("Save only media you're authorized to download. Website support varies; protected or login-only sources aren't supported.",
                        color = BeatColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        preview?.let { p -> item {
            Surface(Modifier.padding(horizontal = BeatDimens.Gutter).fillMaxWidth(), shape = MaterialTheme.shapes.large, color = BeatColors.Surface, contentColor = BeatColors.Text) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        SongArtwork(p.thumbnail, Modifier.size(84.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(p.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(p.source, p.duration?.let { formatSongDuration((it * 1000).toLong()) }).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, color = BeatColors.Muted)
                        }
                    }
                    Text("Audio format", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("BEST", "M4A", "MP3").forEach { f ->
                            FilterChip(format == f, { format = f; quality = 0 }, label = { Text(if (f == "BEST") "Best" else f, maxLines = 1) })
                        }
                    }
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (quality == 0) "Automatic quality" else "$quality kbps conversion", Modifier.weight(1f))
                            Icon(BeatIcons.Down, null, Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded, { expanded = false }) {
                            listOf(0, 128, 192, 256, 320).forEach { q ->
                                DropdownMenuItem(text = { Text(if (q == 0) "Automatic" else "$q kbps conversion") },
                                    onClick = { quality = q; expanded = false })
                            }
                        }
                    }
                    Text("Higher output bitrate can't improve the original source quality.",
                        style = MaterialTheme.typography.bodySmall, color = BeatColors.Muted)
                    Button(onClick = ::start, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Icon(if (model.local) BeatIcons.Video else BeatIcons.Download, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp)); Text(if (model.local) "Extract audio" else "Download audio")
                    }
                    TextButton(onClick = { details = !details }) { Text(if (details) "Hide source details" else "Source details") }
                    if (details) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        p.uploader?.let { Text("Uploaded by $it", style = MaterialTheme.typography.bodySmall, color = BeatColors.Muted) }
                        if (p.formats.isNotEmpty()) Text("Source audio: " + p.formats.joinToString(), style = MaterialTheme.typography.bodySmall, color = BeatColors.Muted)
                        if (p.bitrates.isNotEmpty()) Text("Reported bitrates: " + p.bitrates.joinToString() + " kbps", style = MaterialTheme.typography.bodySmall, color = BeatColors.Muted)
                        Text("Best saves AAC in M4A, copying compatible audio where possible. Other audio is converted. Artwork is preview-only.",
                            style = MaterialTheme.typography.bodySmall, color = BeatColors.Muted)
                    }
                }
            }
        } }
        if (active.isNotEmpty()) {
            item { SectionHeader("In progress · ${active.size}", Modifier.padding(horizontal = BeatDimens.Gutter)) }
            items(active, key = { it.id }) { item ->
                Box(Modifier.padding(horizontal = BeatDimens.Gutter)) {
                    DownloadTaskCard(item, { model.cancel(item.id) }, { model.retry(item.id) }, {})
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = BeatDimens.Gutter)) {
                SectionHeader("History", action = if (tasks.any { it.status == DownloadStatus.COMPLETED }) "Clear completed" else null, onAction = model::clear)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All", "Completed", "Failed")) { filter ->
                        FilterChip(historyFilter == filter, { historyFilter = filter }, label = { Text(filter) })
                    }
                }
            }
        }
        if (history.isEmpty()) item {
            EmptyState(if (active.isNotEmpty()) "Your music is on its way" else "Ready when you are",
                if (historyFilter == "All") "Completed downloads and imports will appear here."
                else "No ${historyFilter.lowercase()} downloads.", BeatIcons.Download)
        }
        items(history, key = { it.id }) { item ->
            Box(Modifier.padding(horizontal = BeatDimens.Gutter)) {
                DownloadTaskCard(item, { model.cancel(item.id) }, { model.retry(item.id) }, {
                    item.finalUri?.let { uri ->
                        try { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri),
                            if (item.format == "MP3") "audio/mpeg" else "audio/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                        catch (_: Exception) { message = "No audio app can open this file." }
                    }
                })
            }
        }
    }
}
