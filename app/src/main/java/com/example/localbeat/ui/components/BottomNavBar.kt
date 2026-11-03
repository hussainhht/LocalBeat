package com.example.localbeat.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localbeat.navigation.Screen
import com.example.localbeat.navigation.bottomNavScreens
import com.example.localbeat.ui.theme.BeatColors

@Composable
fun BottomNavBar(currentScreen: Screen, onScreenSelected: (Screen) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = BeatColors.Elevated.copy(alpha = .98f)) {
        // Content measures its own height so enlarged labels remain readable.
        Row(Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 8.dp)
            .heightIn(min = 76.dp).selectableGroup(), verticalAlignment = Alignment.Top) {
            bottomNavScreens.forEach { screen ->
                val selected = currentScreen == screen
                val tint by animateColorAsState(if (selected) BeatColors.Highlight else BeatColors.Muted, label = "Navigation selection")
                val indicator by animateColorAsState(if (selected) BeatColors.Wine else Color.Transparent, label = "Navigation indicator")
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .selectable(selected = selected, role = Role.Tab, onClick = { onScreenSelected(screen) })
                    .heightIn(min = 64.dp).padding(horizontal = 2.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(width = 56.dp, height = 32.dp).clip(CircleShape).background(indicator),
                        contentAlignment = Alignment.Center) {
                        Icon(screen.icon, null, Modifier.size(24.dp), tint = tint)
                    }
                    Text(screen.title, Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
                        color = if (selected) BeatColors.Text else BeatColors.Muted)
                }
            }
        }
    }
}
