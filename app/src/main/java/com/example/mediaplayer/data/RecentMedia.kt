package com.example.mediaplayer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recent_media")
data class RecentMedia(
    @PrimaryKey val mediaId: Long,
    val mediaUri: String,
    val timestamp: Long,
    val mediaType: String
)

@Dao
interface RecentMediaDao {
    @Query("SELECT * FROM recent_media ORDER BY timestamp DESC LIMIT 20")
    fun getRecentMedia(): Flow<List<RecentMedia>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentMedia)

    @Query("DELETE FROM recent_media WHERE mediaId = :mediaId")
    suspend fun deleteById(mediaId: Long)
}
