package com.example.mediaplayer.data

import android.net.Uri

enum class MediaType {
    AUDIO, VIDEO
}

data class MediaFile(
    val id: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    val year: Int?,
    val duration: Long,
    val size: Long,
    val dateModified: Long,
    val uri: Uri,
    val type: MediaType
)
