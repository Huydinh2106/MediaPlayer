package com.example.mediaplayer.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.mediaplayer.viewmodel.MediaViewModel

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(viewModel: MediaViewModel) {
    val player by viewModel.player.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsState()

    if (player == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val activePlayer = player!!
    val view = LocalView.current
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
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

    LaunchedEffect(isFullscreen) {
        val window = view.context.findActivity()?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        if (isFullscreen) {
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            view.context.findActivity()?.window?.let { window ->
                WindowCompat.getInsetsController(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
        playerView?.setFullscreenButtonState(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = activePlayer
                    setShowPreviousButton(false)
                    setShowNextButton(false)
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            controlsVisible = visibility == View.VISIBLE
                        }
                    )
                    setFullscreenButtonClickListener { fullscreen ->
                        isFullscreen = fullscreen
                    }
                }
            },
            update = { pv ->
                pv.player = activePlayer
                playerView = pv
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                // Sits just above the PlayerView time bar and bottom bar (timer /
                // settings / fullscreen row) so it hides together with them.
                .padding(bottom = 88.dp)
        ) {
            PlayerControlBar(
                isPlaying = isPlaying,
                repeatMode = repeatMode,
                shuffleModeEnabled = shuffleModeEnabled,
                onRepeatClick = { viewModel.toggleRepeatMode() },
                onPreviousClick = { activePlayer.seekToPreviousMediaItem() },
                onPlayPauseClick = {
                    if (activePlayer.isPlaying) activePlayer.pause() else activePlayer.play()
                },
                onNextClick = { activePlayer.seekToNextMediaItem() },
                onShuffleClick = { viewModel.toggleShuffleMode() },
                onAnyInteraction = { playerView?.showController() }
            )
        }
    }
}

@Composable
private fun PlayerControlBar(
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleModeEnabled: Boolean,
    onRepeatClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onAnyInteraction: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.55f),
        contentColor = Color.White,
        shape = RoundedCornerShape(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                onRepeatClick()
                onAnyInteraction()
            }) {
                val icon = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                }
                val tint = if (repeatMode == Player.REPEAT_MODE_OFF) {
                    LocalContentColor.current.copy(alpha = 0.38f)
                } else {
                    MaterialTheme.colorScheme.primary
                }
                Icon(icon, contentDescription = "Repeat", tint = tint)
            }

            IconButton(onClick = {
                onPreviousClick()
                onAnyInteraction()
            }) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = {
                    onPlayPauseClick()
                    onAnyInteraction()
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(44.dp)
                )
            }

            IconButton(onClick = {
                onNextClick()
                onAnyInteraction()
            }) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = {
                onShuffleClick()
                onAnyInteraction()
            }) {
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
