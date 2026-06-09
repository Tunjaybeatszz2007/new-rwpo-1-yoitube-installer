package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.network.DownloadStatus
import com.example.data.network.DownloadTask
import com.example.ui.viewmodel.DownloaderViewModel

@Composable
fun DownloaderTab(
    viewModel: DownloaderViewModel,
    modifier: Modifier = Modifier
) {
    val activeTasks by viewModel.activeTasks.collectAsState()
    var inputUrl by remember { mutableStateOf("") }
    var selectedMediaType by remember { mutableStateOf("VIDEO") } // "VIDEO" or "AUDIO"
    var selectedResolution by remember { mutableStateOf("720") } // "1080", "720", "480", "360"

    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Intro header card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadForOffline,
                            contentDescription = "Welcome logo",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column {
                        Text(
                            text = "Download High-Quality Media",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Enter a video link, select resolution or format, and stream or listen offline 100% ad-free.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Input Field Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Source Link",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("url_input_field"),
                        placeholder = { Text("Paste YouTube, Shorts or Song URL...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "URL Link icon"
                            )
                        },
                        trailingIcon = {
                            if (inputUrl.isNotEmpty()) {
                                IconButton(
                                    onClick = { inputUrl = "" },
                                    modifier = Modifier.testTag("clear_url_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear address input"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(50),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Clipboard Paste Button
                        Button(
                            onClick = {
                                val clip = clipboardManager.getText()
                                if (clip != null) {
                                    inputUrl = clip.text
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("paste_clipboard_button")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Paste Link", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Configuration (Format & Resolution selection) Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Configuration",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (selectedMediaType == "VIDEO") "MP4 Video" else "AAC Audio",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Format Toggle Button Tab - matches CSS/Tailwind flex gap-2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { selectedMediaType = "VIDEO" },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("format_video_toggle"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedMediaType == "VIDEO") MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (selectedMediaType == "VIDEO") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = if (selectedMediaType != "VIDEO") androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartDisplay,
                                contentDescription = "Download custom resolution videos",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Video", fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = { selectedMediaType = "AUDIO" },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("format_audio_toggle"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedMediaType == "AUDIO") MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (selectedMediaType == "AUDIO") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = if (selectedMediaType != "AUDIO") androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Download high quality songs",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Audio Only", fontWeight = FontWeight.Medium)
                        }
                    }

                    // Resolution selector (Visible ONLY if Video is selected)
                    AnimatedVisibility(
                        visible = selectedMediaType == "VIDEO",
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Video Resolution",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val resolutions = listOf(
                                    "1080" to "1080p",
                                    "720" to "720p",
                                    "480" to "480p"
                                )
                                resolutions.forEach { (value, label) ->
                                    val isSelected = selectedResolution == value
                                    Button(
                                        onClick = { selectedResolution = value },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("res_${value}_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = label, 
                                            style = MaterialTheme.typography.bodySmall, 
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Start Install Button - matches Tailwind full rounded h-14 bg-[#6750A4] shadow-lg
                    Button(
                        onClick = {
                            if (inputUrl.isNotBlank()) {
                                viewModel.startDownload(inputUrl, selectedMediaType, selectedResolution)
                                inputUrl = "" // clear input after starting
                            }
                        },
                        enabled = inputUrl.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("install_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Install"
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Fetch Download Link",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Active Queue Header
        if (activeTasks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Downloads (${activeTasks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(
                        onClick = { viewModel.clearAllFinishedTasks() },
                        modifier = Modifier.testTag("clear_queue_button")
                    ) {
                        Text("Clear Completed", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Queue detail cards
            items(activeTasks, key = { it.youtubeUrl }) { task ->
                ActiveTaskCard(task = task, onRemove = { viewModel.removeTask(task) })
            }
        }
    }
}

@Composable
fun ActiveTaskCard(
    task: DownloadTask,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Video Thumbnail
            if (task.thumbnailUrl != null) {
                AsyncImage(
                    model = task.thumbnailUrl,
                    contentDescription = "Thumbnail preview",
                    modifier = Modifier
                        .size(height = 60.dp, width = 80.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(height = 60.dp, width = 80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (task.mediaType == "AUDIO") Icons.Default.MusicNote else Icons.Default.SmartDisplay,
                        contentDescription = "Thumbnail fallback",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Task content Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    val isFinished = task.status == DownloadStatus.COMPLETED || task.status == DownloadStatus.FAILED
                    if (isFinished) {
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier.size(24.dp).testTag("remove_task_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete task indicator",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (task.status == DownloadStatus.FAILED) {
                    Text(
                        text = "Error: ${task.error ?: "Link extraction failed"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (task.status) {
                                DownloadStatus.PENDING -> "Pending in queue..."
                                DownloadStatus.EXTRACTING -> "Extracting..."
                                DownloadStatus.DOWNLOADING -> "Downloading: ${task.speed}"
                                DownloadStatus.COMPLETED -> "Finished & Saved!"
                                DownloadStatus.FAILED -> "Failed"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (task.status == DownloadStatus.COMPLETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (task.status == DownloadStatus.DOWNLOADING) {
                            Text(
                                text = "${(task.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.EXTRACTING) {
                        LinearProgressIndicator(
                            progress = { if (task.status == DownloadStatus.DOWNLOADING) task.progress else 0.1f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        )
                    } else if (task.status == DownloadStatus.COMPLETED) {
                        LinearProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        )
                    }
                }
            }
        }
    }
}
