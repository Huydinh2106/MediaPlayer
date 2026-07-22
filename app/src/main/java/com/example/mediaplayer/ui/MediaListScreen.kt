package com.example.mediaplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mediaplayer.data.MediaFile
import com.example.mediaplayer.viewmodel.MediaViewModel
import com.example.mediaplayer.viewmodel.SortType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaListScreen(
    viewModel: MediaViewModel,
    onMediaClick: (MediaFile) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Audio", "Video")

    val audioFiles by viewModel.filteredAudioFiles.collectAsState()
    val videoFiles by viewModel.filteredVideoFiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search songs, artists, videos…") },
            shape = RoundedCornerShape(28.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                Row {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        viewModel.setSortType(type)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        val currentList = if (selectedTab == 0) audioFiles else videoFiles

        if (currentList.isEmpty()) {
            EmptyState(
                icon = if (selectedTab == 0) Icons.Default.LibraryMusic else Icons.Default.VideoLibrary,
                title = if (searchQuery.isNotEmpty()) "No results" else "No ${tabs[selectedTab].lowercase()} found",
                subtitle = if (searchQuery.isNotEmpty()) {
                    "Try a different search term."
                } else {
                    "Add some media to your device to see it here."
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = currentList,
                    key = { it.id }
                ) { file ->
                    MediaItemRow(file = file, onClick = { onMediaClick(file) })
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadMedia()
    }
}
