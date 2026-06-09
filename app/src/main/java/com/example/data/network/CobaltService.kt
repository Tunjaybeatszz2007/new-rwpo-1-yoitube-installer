package com.example.data.network

import android.content.Context
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.DownloadedMedia
import com.example.util.YoutubeUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

enum class DownloadStatus {
    PENDING,
    EXTRACTING,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class DownloadTask(
    val youtubeUrl: String,
    val title: String,
    val thumbnailUrl: String?,
    val progress: Float = 0f,
    val speed: String = "0 KB/s",
    val status: DownloadStatus = DownloadStatus.PENDING,
    val mediaType: String, // "VIDEO" or "AUDIO"
    val resolution: String,
    val error: String? = null
)

class CobaltService(private val context: Context) {
    private val oKHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.MINUTES) // downloads can take time
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.cobalt.tools/") // fallback base, but we use dynamic @Url
        .client(oKHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(CobaltApi::class.java)
    private val db = AppDatabase.getDatabase(context)

    // Curated list of popular, active, public cobalt instances for high availability!
    private val cobaltInstances = listOf(
        "https://api.cobalt.tools/",
        "https://cobalt.api.ryboflops.eu.org/",
        "https://api.cobalt.club/",
        "https://cobalt-api.drgns.space/",
        "https://cobalt-api.mikaela.sh/"
    )

    suspend fun processDownload(
        youtubeUrl: String,
        mediaType: String, // "VIDEO" or "AUDIO"
        resolution: String, // e.g., "720", "1080", "480", "360"
        onProgressUpdate: (Float, String) -> Unit, // progress, progress status message
        onTaskStatusChange: (DownloadStatus, String?) -> Unit // status, error explanation
    ) {
        withContext(Dispatchers.IO) {
            onTaskStatusChange(DownloadStatus.EXTRACTING, "Contacting extraction servers...")

            var downloadUrl: String? = null
            var finalTitle = "YouTube Media"
            var extractionError: String? = null

            // Clean/sanitize URL
            val cleanUrl = youtubeUrl.trim()

            // Construct requests
            val requestBody = if (mediaType == "AUDIO") {
                CobaltRequest(
                    url = cleanUrl,
                    isAudioOnly = true,
                    audioFormat = "mp3"
                )
            } else {
                CobaltRequest(
                    url = cleanUrl,
                    videoQuality = resolution,
                    isAudioOnly = false
                )
            }

            // Loop through instances to find a working extraction server
            for (instanceEndpoint in cobaltInstances) {
                try {
                    Log.d("CobaltService", "Trying link extraction on $instanceEndpoint")
                    val response = api.extractDownloadLink(instanceEndpoint, requestBody)
                    if (response.status == "success" && response.url != null) {
                        downloadUrl = response.url
                        break
                    } else if (response.status == "stream" && response.url != null) {
                        downloadUrl = response.url
                        break
                    } else if (response.status == "redirect" && response.url != null) {
                        downloadUrl = response.url
                        break
                    } else if (response.status == "error") {
                        extractionError = response.text ?: "Extract failed on server"
                    }
                } catch (e: Exception) {
                    Log.e("CobaltService", "Failed on $instanceEndpoint: ${e.localizedMessage}")
                    extractionError = e.localizedMessage ?: "Unknown connection error"
                }
            }

            if (downloadUrl == null) {
                onTaskStatusChange(DownloadStatus.FAILED, extractionError ?: "All fallback extract servers were unreachable. Please verify if the URL is valid or retry in a moment.")
                return@withContext
            }

            // Try to extract title from URL or generate a beautiful fallback title
            val videoId = YoutubeUtils.extractYoutubeVideoId(cleanUrl)
            val fallbackTitle = if (videoId != null) {
                if (mediaType == "AUDIO") "Song_$videoId" else "Video_$videoId"
            } else {
                "Media_" + System.currentTimeMillis().toString().takeLast(6)
            }
            finalTitle = fallbackTitle

            onTaskStatusChange(DownloadStatus.DOWNLOADING, "Connecting to media stream...")

            // Start actual OkHttp Downloading to files dir
            try {
                val downloadRequest = Request.Builder().url(downloadUrl).build()
                oKHttpClient.newCall(downloadRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        onTaskStatusChange(DownloadStatus.FAILED, "Download server returned HTTP code ${response.code}")
                        return@withContext
                    }

                    val body = response.body
                    if (body == null) {
                        onTaskStatusChange(DownloadStatus.FAILED, "Download server returned empty response body")
                        return@withContext
                    }

                    // Extract actual title from response header if possible!
                    val contentDisposition = response.header("Content-Disposition")
                    var parsedTitle = finalTitle
                    if (contentDisposition != null) {
                        try {
                            val filenameRegex = """filename\*=UTF-8''([^;\n]+)""".toRegex()
                            val matchResult = filenameRegex.find(contentDisposition)
                            val extractedFilename = matchResult?.groups?.get(1)?.value ?: run {
                                val altRegex = """filename=["']?([^"']+)["']?""".toRegex()
                                altRegex.find(contentDisposition)?.groups?.get(1)?.value
                            }
                            if (!extractedFilename.isNullOrBlank()) {
                                // Decode URL encoded characters if any
                                val decoded = java.net.URLDecoder.decode(extractedFilename, "UTF-8")
                                parsedTitle = decoded
                                    .substringBeforeLast(".") // remove file suffix
                                    .replace("_", " ")
                                    .replace("+", " ")
                                    .trim()
                            }
                        } catch (e: Exception) {
                            Log.e("CobaltService", "Failed to parse filename headers: ${e.localizedMessage}")
                        }
                    }

                    if (parsedTitle == fallbackTitle || parsedTitle.isBlank()) {
                        parsedTitle = if (mediaType == "AUDIO") "Offline YouTube Audio" else "Offline YouTube Video"
                    }

                    // Extract actual size or guess it
                    val totalBytes = body.contentLength()
                    val isRangeOrUnknown = totalBytes <= 0

                    // Create subfolder 'downloads' inside app storage
                    val downloadsFolder = File(context.filesDir, "downloads")
                    if (!downloadsFolder.exists()) {
                        downloadsFolder.mkdirs()
                    }

                    // Safe filename
                    val suffix = if (mediaType == "AUDIO") ".mp3" else ".mp4"
                    val safeName = parsedTitle.replace("[^a-zA-Z0-9]".toRegex(), "_") + "_" + System.currentTimeMillis() + suffix
                    val outputFile = File(downloadsFolder, safeName)

                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(outputFile)
                    val buffer = ByteArray(128 * 1024) // 128KB buffer for faster streaming
                    var bytesRead: Int
                    var bytesDownloaded = 0L
                    val startTime = System.currentTimeMillis()

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        if (!isRangeOrUnknown) {
                            val progressFloat = bytesDownloaded.toFloat() / totalBytes
                            val elapsedTimeSec = (System.currentTimeMillis() - startTime) / 1000.0
                            val speedString = if (elapsedTimeSec > 0) {
                                val speedKb = (bytesDownloaded / 1024.0) / elapsedTimeSec
                                if (speedKb > 1024) {
                                    String.format("%.2f MB/s", speedKb / 1024.0)
                                } else {
                                    String.format("%.1f KB/s", speedKb)
                                }
                            } else "0 KB/s"

                            onProgressUpdate(progressFloat, speedString)
                        } else {
                            onProgressUpdate(0.5f, "Streaming...")
                        }
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()

                    // Register DownloadedItem in SQLite database
                    val mediaItem = DownloadedMedia(
                        youtubeUrl = cleanUrl,
                        title = parsedTitle,
                        thumbnailUrl = YoutubeUtils.getYoutubeThumbnailUrl(cleanUrl),
                        localFilePath = outputFile.absolutePath,
                        mediaType = mediaType,
                        resolution = if (mediaType == "AUDIO") "Audio" else "${resolution}p",
                        fileSize = bytesDownloaded,
                        durationString = if (mediaType == "AUDIO") "HD Audio" else "Video player",
                        timestamp = System.currentTimeMillis()
                    )

                    db.offlineMediaDao().insertMedia(mediaItem)
                    onTaskStatusChange(DownloadStatus.COMPLETED, null)
                }
            } catch (e: Exception) {
                Log.e("CobaltService", "Download process crashed: ${e.localizedMessage}")
                onTaskStatusChange(DownloadStatus.FAILED, e.localizedMessage ?: "Network interrupted during file save.")
            }
        }
    }
}
