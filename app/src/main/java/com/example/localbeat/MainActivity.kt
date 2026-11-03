package com.example.localbeat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.localbeat.navigation.AppNavigation
import com.example.localbeat.ui.theme.LocalBeatTheme

class MainActivity : ComponentActivity() {
    private var shared by mutableStateOf<String?>(null)
    private var video by mutableStateOf(false)
    private fun receive(value: Intent) {
        if (value.action != Intent.ACTION_SEND) return
        if (value.type == "text/plain") {
            shared = value.getStringExtra(Intent.EXTRA_TEXT)?.let {
                Regex("https?://[^\\s]+").find(it)?.value
            }
            video = false
        } else if (value.type?.startsWith("video/") == true) {
            @Suppress("DEPRECATION")
            val uri = value.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri?.scheme == "content") { shared = uri.toString(); video = true }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receive(intent)
        enableEdgeToEdge()
        setContent { LocalBeatTheme { AppNavigation(shared = shared, sharedVideo = video) } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent); receive(intent)
    }
}
