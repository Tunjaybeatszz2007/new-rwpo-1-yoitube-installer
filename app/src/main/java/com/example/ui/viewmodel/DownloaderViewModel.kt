package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.DownloadedMedia
import com.example.data.network.CobaltService
import com.example.data.network.DownloadStatus
import com.example.data.network.DownloadTask
import com.example.util.YoutubeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class DownloaderViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val cobaltService = CobaltService(application)

    // Reactive list of downloaded items
    val downloadedMediaList: StateFlow<List<DownloadedMedia>> = db.offlineMediaDao().getAllMedia()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeTasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val activeTasks: StateFlow<List<DownloadTask>> = _activeTasks.asStateFlow()

    fun startDownload(youtubeUrl: String, mediaType: String, resolution: String) {
        val trimmedUrl = youtubeUrl.trim()
        if (trimmedUrl.isBlank()) return

        // Verify if it's already downloading or queued
        val alreadyRunning = _activeTasks.value.any { 
            it.youtubeUrl == trimmedUrl && (it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.EXTRACTING) 
        }
        if (alreadyRunning) {
            Log.d("DownloaderViewModel", "Download already in progress for $trimmedUrl")
            return
        }

        val videoId = YoutubeUtils.extractYoutubeVideoId(trimmedUrl)
        val initialTitle = if (videoId != null) {
            if (mediaType == "AUDIO") "Extracting Song ID: $videoId" else "Extracting Video ID: $videoId"
        } else {
            "Searching Link Content..."
        }

        val newTask = DownloadTask(
            youtubeUrl = trimmedUrl,
            title = initialTitle,
            thumbnailUrl = YoutubeUtils.getYoutubeThumbnailUrl(trimmedUrl),
            progress = 0f,
            speed = "Connecting...",
            status = DownloadStatus.PENDING,
            mediaType = mediaType,
            resolution = resolution
        )

        // Add to active tasks
        _activeTasks.value = _activeTasks.value + newTask

        viewModelScope.launch {
            try {
                cobaltService.processDownload(
                    youtubeUrl = trimmedUrl,
                    mediaType = mediaType,
                    resolution = resolution,
                    onProgressUpdate = { progress, speed ->
                        updateTaskProgress(trimmedUrl, progress, speed)
                    },
                    onTaskStatusChange = { status, error ->
                        updateTaskStatus(trimmedUrl, status, error)
                    }
                )
            } catch (e: Exception) {
                updateTaskStatus(trimmedUrl, DownloadStatus.FAILED, e.localizedMessage ?: "Unknown extraction server glitch.")
            }
        }
    }

    private fun updateTaskProgress(url: String, progress: Float, speed: String) {
        _activeTasks.value = _activeTasks.value.map { task ->
            if (task.youtubeUrl == url) {
                task.copy(
                    progress = progress,
                    speed = speed,
                    status = DownloadStatus.DOWNLOADING,
                    title = if (task.title.startsWith("Extracting")) "Downloading file..." else task.title
                )
            } else {
                task
            }
        }
    }

    private fun updateTaskStatus(url: String, status: DownloadStatus, error: String?) {
        _activeTasks.value = _activeTasks.value.map { task ->
            if (task.youtubeUrl == url) {
                task.copy(
                    status = status,
                    error = error,
                    speed = if (status == DownloadStatus.COMPLETED) "Finished" else if (status == DownloadStatus.FAILED) "Failed" else task.speed,
                    progress = if (status == DownloadStatus.COMPLETED) 1f else task.progress,
                    title = if (status == DownloadStatus.FAILED) "Failed Download" else task.title
                )
            } else {
                task
            }
        }
    }

    fun removeTask(task: DownloadTask) {
        _activeTasks.value = _activeTasks.value.filter { it.youtubeUrl != task.youtubeUrl }
    }

    fun deleteMedia(media: DownloadedMedia) {
        viewModelScope.launch {
            // Delete actual file
            try {
                val file = File(media.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e("DownloaderViewModel", "Failed to delete file from disk: ${e.localizedMessage}")
            }

            // Delete database entry
            db.offlineMediaDao().deleteMedia(media)
        }
    }

    fun clearAllFinishedTasks() {
        _activeTasks.value = _activeTasks.value.filter { 
            it.status != DownloadStatus.COMPLETED && it.status != DownloadStatus.FAILED 
        }
    }
}
