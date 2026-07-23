package com.example.mediaplayer.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.example.mediaplayer.viewmodel.MediaViewModel

@Composable
fun PlayerScreen(
    viewModel: MediaViewModel,
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {}
) {
    val player = viewModel.player
    val isVideo by viewModel.isCurrentVideo.collectAsState()
    val context = LocalContext.current

    val supportsPip = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                }
            },
            update = { view ->
                // Hide playback controls while the window is shrunk into PiP.
                view.useController = !isInPipMode
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isVideo && supportsPip && !isInPipMode) {
            IconButton(
                onClick = onEnterPip,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureInPictureAlt,
                    contentDescription = "Picture in Picture",
                    tint = Color.White
                )
            }
        }
    }
}
