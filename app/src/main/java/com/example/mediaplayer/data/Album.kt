package com.example.mediaplayer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "album_media_cross_ref",
    primaryKeys = ["albumId", "mediaId"]
)
data class AlbumMediaCrossRef(
    val albumId: Long,
    val mediaId: Long
)

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY timestamp DESC")
    fun getAllAlbums(): Flow<List<Album>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: Album): Long

    @Delete
    suspend fun deleteAlbum(album: Album)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaToAlbum(crossRef: AlbumMediaCrossRef)

    @Delete
    suspend fun removeMediaFromAlbum(crossRef: AlbumMediaCrossRef)

    @Query("SELECT * FROM album_media_cross_ref WHERE albumId = :albumId")
    fun getMediaIdsForAlbum(albumId: Long): Flow<List<AlbumMediaCrossRef>>

    @Query("SELECT name FROM albums WHERE id = :albumId")
    fun getAlbumName(albumId: Long): Flow<String>
}
