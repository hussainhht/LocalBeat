package com.example.localbeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.localbeat.navigation.AppNavigation
import com.example.localbeat.ui.theme.LocalBeatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalBeatTheme {
                AppNavigation()
            }
        }
    }
}
