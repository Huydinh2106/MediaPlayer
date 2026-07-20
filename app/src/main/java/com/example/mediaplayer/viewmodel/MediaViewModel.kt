package com.example.mediaplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.mediaplayer.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortType {
    NAME, SIZE, DATE, DURATION, ARTIST, ALBUM
}

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaStoreRepository(application.contentResolver)
    private val db = AppDatabase.getDatabase(application)
    private val recentDao = db.recentMediaDao()

    private val _audioFiles = MutableStateFlow<List<MediaFile>>(emptyList())
    private val _videoFiles = MutableStateFlow<List<MediaFile>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortType = MutableStateFlow(SortType.NAME)
    val sortType: StateFlow<SortType> = _sortType

    val filteredAudioFiles = combine(_audioFiles, _searchQuery, _sortType) { files, query, sort ->
        filterAndSort(files, query, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredVideoFiles = combine(_videoFiles, _searchQuery, _sortType) { files, query, sort ->
        filterAndSort(files, query, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentMediaFiles = combine(recentDao.getRecentMedia(), _audioFiles, _videoFiles) { recentList, audios, videos ->
        val allMedia = audios + videos
        recentList.mapNotNull { recent ->
            allMedia.find { it.id == recent.mediaId }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentPlayerState = MutableStateFlow<Player?>(null)
    val currentPlayer: StateFlow<Player?> = _currentPlayerState

    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(application).build().also {
            _currentPlayerState.value = it
        }
    }

    fun loadMedia() {
        viewModelScope.launch {
            _audioFiles.value = repository.getAudioFiles()
            _videoFiles.value = repository.getVideoFiles()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortType(sortType: SortType) {
        _sortType.value = sortType
    }

    fun playMedia(mediaFile: MediaFile) {
        val mediaItem = MediaItem.fromUri(mediaFile.uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        viewModelScope.launch {
            recentDao.insertRecent(
                RecentMedia(
                    mediaId = mediaFile.id,
                    mediaUri = mediaFile.uri.toString(),
                    timestamp = System.currentTimeMillis(),
                    mediaType = mediaFile.type.name
                )
            )
        }
    }

    private fun filterAndSort(files: List<MediaFile>, query: String, sort: SortType): List<MediaFile> {
        val filtered = if (query.isBlank()) files else {
            files.filter { it.title.contains(query, ignoreCase = true) || it.artist?.contains(query, ignoreCase = true) == true }
        }
        return when (sort) {
            SortType.NAME -> filtered.sortedBy { it.title }
            SortType.SIZE -> filtered.sortedByDescending { it.size }
            SortType.DATE -> filtered.sortedByDescending { it.dateModified }
            SortType.DURATION -> filtered.sortedByDescending { it.duration }
            SortType.ARTIST -> filtered.sortedBy { it.artist ?: "" }
            SortType.ALBUM -> filtered.sortedBy { it.album ?: "" }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
