package com.example.mediaplayer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.example.mediaplayer.viewmodel.MediaViewModel

@Composable
fun PlayerScreen(viewModel: MediaViewModel) {
    val player = viewModel.player

    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                this.player = player
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            // We might want to stop playback when leaving screen if it's a video player
            // but for a background media player we might keep it.
            // For now, let's just keep it playing or handled by ViewModel
        }
    }
}
