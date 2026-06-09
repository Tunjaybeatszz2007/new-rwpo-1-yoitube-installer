package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "downloaded_media")
data class DownloadedMedia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val youtubeUrl: String,
    val title: String,
    val thumbnailUrl: String?,
    val localFilePath: String,
    val mediaType: String, // "VIDEO" or "AUDIO"
    val resolution: String, // e.g. "1080p", "720p", "MP3", "WAV"
    val fileSize: Long,
    val durationString: String = "00:00",
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
