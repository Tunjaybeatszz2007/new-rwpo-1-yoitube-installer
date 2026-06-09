package com.example.data.database

import androidx.room.*
import com.example.data.model.DownloadedMedia
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineMediaDao {
    @Query("SELECT * FROM downloaded_media ORDER BY timestamp DESC")
    fun getAllMedia(): Flow<List<DownloadedMedia>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: DownloadedMedia)

    @Delete
    suspend fun deleteMedia(media: DownloadedMedia)

    @Query("DELETE FROM downloaded_media WHERE id = :id")
    suspend fun deleteMediaById(id: Int)
}
