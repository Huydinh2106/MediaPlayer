package com.example.mediaplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.example.mediaplayer.viewmodel.MediaViewModel

@Composable
fun PlayerScreen(viewModel: MediaViewModel) {
    val player by viewModel.player.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsState()

    if (player == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val activePlayer = player!!
        Column(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        this.player = activePlayer
                    }
                },
                update = { view ->
                    view.player = activePlayer
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                        val icon = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                            Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                            else -> Icons.Default.Repeat
                        }
                        val tint = if (repeatMode == Player.REPEAT_MODE_OFF) {
                            LocalContentColor.current.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                        Icon(icon, contentDescription = "Repeat", tint = tint)
                    }

                    IconButton(onClick = { activePlayer.seekToPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                    }

                    IconButton(
                        onClick = {
                            if (activePlayer.isPlaying) activePlayer.pause() else activePlayer.play()
                        },
                        modifier = Modifier.size(64.dp)
                    ) {
                        var isPlaying by remember { mutableStateOf(activePlayer.isPlaying) }
                        
                        DisposableEffect(activePlayer) {
                            val listener = object : Player.Listener {
                                override fun onIsPlayingChanged(playing: Boolean) {
                                    isPlaying = playing
                                }
                            }
                            activePlayer.addListener(listener)
                            onDispose { activePlayer.removeListener(listener) }
                        }

                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    IconButton(onClick = { activePlayer.seekToNext() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next")
                    }
                    
                    IconButton(onClick = { viewModel.toggleShuffleMode() }) {
                        val tint = if (shuffleModeEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            LocalContentColor.current.copy(alpha = 0.38f)
                        }
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = tint)
                    }
                }
            }
        }
    }
}
