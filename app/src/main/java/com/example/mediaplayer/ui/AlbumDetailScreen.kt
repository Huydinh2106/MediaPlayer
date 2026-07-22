package com.example.mediaplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mediaplayer.data.MediaFile
import com.example.mediaplayer.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    viewModel: MediaViewModel,
    albumId: Long,
    onBack: () -> Unit,
    onMediaClick: (MediaFile, List<MediaFile>) -> Unit
) {
    val albumMedia by viewModel.getAlbumWithMedia(albumId).collectAsState(initial = emptyList())
    val albumName by viewModel.getAlbumName(albumId).collectAsState(initial = "Album")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(albumName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (albumMedia.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No media in this album.")
                }
            } else {
                LazyColumn {
                    items(albumMedia) { file ->
                        MediaItemRow(
                            file = file,
                            onClick = { onMediaClick(file, albumMedia) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.removeMediaFromAlbum(file.id, albumId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove from Album")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
