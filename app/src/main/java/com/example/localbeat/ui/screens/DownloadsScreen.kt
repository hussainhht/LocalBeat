package com.example.localbeat.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.localbeat.data.model.*

@Composable
fun DownloadsScreen(modifier: Modifier = Modifier, model: DownloadsViewModel = viewModel(),
    shared: String? = null, sharedVideo: Boolean = false) {
    val context = LocalContext.current
    val tasks by model.tasks.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf(model.input.value) }
    fun updateInput(value: String) { input = value; model.setInput(value) }
    val preview by model.preview.collectAsStateWithLifecycle()
    val state by model.state.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    var format by rememberSaveable { mutableStateOf("M4A") }
    var quality by rememberSaveable { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(shared) {
        shared?.let { input = it; if (sharedVideo) model.video(Uri.parse(it)) else model.setInput(it) }
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
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            needed += Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            needed += Manifest.permission.POST_NOTIFICATIONS
        if (needed.isEmpty()) model.enqueue(format, quality) else permissions.launch(needed.toTypedArray())
    }
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Audio imports", style = MaterialTheme.typography.headlineMedium)
                Text("Download only media you are authorized to save. Website support varies; protected and login-only sources are unsupported.")
                OutlinedTextField(input, ::updateInput, label = { Text("Media URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row {
                    TextButton(onClick = { clipboard.getText()?.text?.let(::updateInput) }) { Text("Paste") }
                    Button(onClick = model::fetchInfo, enabled = state != PreviewState.FETCHING_INFO) { Text("Fetch info") }
                }
                OutlinedButton(onClick = { picker.launch(arrayOf("video/*")) }) { Text("Extract Audio from Video") }
                if (state == PreviewState.FETCHING_INFO || state == PreviewState.VALIDATING) {
                    LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Fetching information…")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                message?.let { Text(it) }
                preview?.let { p ->
                    Card {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            p.thumbnail?.let { AsyncImage(it, "Media artwork", Modifier.height(120.dp).fillMaxWidth()) }
                            Text(p.title, style = MaterialTheme.typography.titleMedium)
                            Text(listOfNotNull(p.source, p.duration?.let { "${it.toInt()} seconds" }, p.uploader?.let { "Uploader: $it" }).joinToString(" • "))
                            if (p.formats.isNotEmpty()) Text("Source audio: " + p.formats.joinToString())
                            if (p.bitrates.isNotEmpty()) Text("Reported source bitrates: " + p.bitrates.joinToString() + " kbps")
                            Row {
                                listOf("BEST", "M4A", "MP3").forEach { f ->
                                    FilterChip(format == f, { format = f; quality = 0 }, label = { Text(f) })
                                }
                            }
                            Text("Best uses AAC in M4A, copying compatible AAC when possible. Other sources are converted.")
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { expanded = true }) { Text(if (quality == 0) "Automatic quality" else "$quality kbps (conversion)") }
                                DropdownMenu(expanded, { expanded = false }) {
                                    (listOf(0) + listOf(128, 192, 256, 320)).forEach { q ->
                                        DropdownMenuItem(text = { Text(if (q == 0) "Automatic" else "$q kbps conversion") },
                                            onClick = { quality = q; expanded = false })
                                    }
                                }
                            }
                            Text("Conversion bitrate is an output setting. Higher bitrate cannot improve source quality. Artwork is preview-only.")
                            Button(onClick = ::start) { Text(if (model.local) "Extract audio" else "Download audio") }
                        }
                    }
                }
                Row {
                    Text("Tasks & history", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = model::clear) { Text("Clear completed") }
                }
            }
        }
        items(tasks, key = { it.id }) { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.thumbnail?.let { AsyncImage(it, "Media artwork", Modifier.height(64.dp)) }
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(item.stage + " • " + item.format)
                    item.downloadedBytes?.let { Text("$it bytes downloaded") }
                    val active = item.status !in listOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELLED)
                    if (active) {
                        item.progress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth()) }
                            ?: LinearProgressIndicator(Modifier.fillMaxWidth())
                        TextButton(onClick = { model.cancel(item.id) }, enabled = item.status != DownloadStatus.SAVING) { Text("Cancel") }
                    }
                    item.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (item.status == DownloadStatus.FAILED || item.status == DownloadStatus.CANCELLED)
                        TextButton(onClick = { model.retry(item.id) }) { Text("Retry") }
                    item.finalUri?.let { uri ->
                        TextButton(onClick = {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(uri),
                                if (item.format == "MP3") "audio/mpeg" else "audio/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                            catch (e: Exception) { message = "No audio app can open this file." }
                        }) { Text("Play / open audio") }
                    }
                }
            }
        }
    }
}
