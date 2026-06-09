package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.DownloadedMedia
import com.example.ui.viewmodel.DownloaderViewModel

@Composable
fun LibraryTab(
    viewModel: DownloaderViewModel,
    onMediaSelect: (DownloadedMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaItems by viewModel.downloadedMediaList.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "VIDEO", "AUDIO"
    var mediaToDelete by remember { mutableStateOf<DownloadedMedia?>(null) }

    // Filtered lists
    val filteredItems = remember(mediaItems, searchQuery, selectedFilter) {
        mediaItems.filter { item ->
            val matchesSearch = item.title.contains(searchQuery, ignoreCase = true)
            val matchesCategory = when (selectedFilter) {
                "VIDEO" -> item.mediaType == "VIDEO"
                "AUDIO" -> item.mediaType == "AUDIO"
                else -> true
            }
            matchesSearch && matchesCategory
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search Filter Section - styled like Tailwind Input: bg-[#ECE6F0] rounded-full
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .testTag("library_search_input"),
            placeholder = { Text("Search offline tracks & videos...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search offline catalog") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.testTag("library_search_clear")
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search query")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            )
        )

        // Category Toggles Match Tailwind design
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filters = listOf(
                "ALL" to "All Offline",
                "VIDEO" to "Videos",
                "AUDIO" to "Songs & Music"
            )
            filters.forEach { (value, label) ->
                val isSelected = selectedFilter == value
                Button(
                    onClick = { selectedFilter = value },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) else null,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("filter_${value}_chip"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        // List / Empty State section
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.LibraryMusic,
                            contentDescription = "Empty state icon",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No Matching Downloads" else "No Offline Tracks Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) {
                            "Try revising keywords or clear filter categories to find what you downloaded."
                        } else {
                            "Browse and select formats in the Downloader tab. Your downloaded files will automatically assemble here for completely offline, clean, ad-free play."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    DownloadedMediaCard(
                        media = item,
                        onPlayClick = { onMediaSelect(item) },
                        onDeleteClick = { mediaToDelete = item }
                    )
                }
            }
        }
    }

    // Deletion Confirmation Dialog
    if (mediaToDelete != null) {
        val target = mediaToDelete!!
        AlertDialog(
            onDismissRequest = { mediaToDelete = null },
            title = { Text("Confirm Deletion", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete \"${target.title}\" and free up ${formatBytes(target.fileSize)} space?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMedia(target)
                        mediaToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_btn")
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mediaToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_btn")
                ) {
                    Text("Keep File")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun DownloadedMediaCard(
    media: DownloadedMedia,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlayClick)
            .testTag("media_card_${media.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Video/Audio thumbnail
            if (media.thumbnailUrl != null) {
                Box(
                    modifier = Modifier
                        .size(height = 64.dp, width = 86.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    AsyncImage(
                        model = media.thumbnailUrl,
                        contentDescription = "Track preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Display Badge depending on AUDIO/VIDEO
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = media.resolution,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(height = 64.dp, width = 86.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (media.mediaType == "AUDIO") Icons.Default.MusicNote else Icons.Default.SmartDisplay,
                        contentDescription = "Fallback preview icon",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Text detail parameters
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${media.mediaType} • ${formatBytes(media.fileSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Tap to Play Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Permanent Delete bin icon button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.testTag("delete_media_button_${media.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete download file",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
