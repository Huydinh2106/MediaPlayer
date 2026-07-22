package com.example.mediaplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mediaplayer.viewmodel.MediaViewModel

@Composable
fun SettingsScreen(viewModel: MediaViewModel) {
    val isBackgroundPlayEnabled by viewModel.isBackgroundPlayEnabled.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Background Playback",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Continue playing audio/video when app is minimized",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = isBackgroundPlayEnabled,
                onCheckedChange = { viewModel.toggleBackgroundPlay(it) }
            )
        }
    }
}
