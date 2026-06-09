package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DownloadedMedia
import com.example.ui.components.DownloaderTab
import com.example.ui.components.LibraryTab
import com.example.ui.components.OfflinePlayer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DownloaderViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: DownloaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableIntStateOf(0) } // 0 = Downloader, 1 = Library
                var playingMedia by remember { mutableStateOf<DownloadedMedia?>(null) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        HeaderBar()
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.DownloadForOffline,
                                        contentDescription = "Downloader page tab link"
                                    )
                                },
                                label = { Text("Downloader") },
                                modifier = Modifier.testTag("tab_downloader")
                            )

                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.LibraryMusic,
                                        contentDescription = "Library offline folder tab link"
                                    )
                                },
                                label = { Text("Offline Library") },
                                modifier = Modifier.testTag("tab_library")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Display correct Tab content
                        when (currentTab) {
                            0 -> DownloaderTab(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            1 -> LibraryTab(
                                viewModel = viewModel,
                                onMediaSelect = { media ->
                                    playingMedia = media
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Floating / Animated Overlaid Offline Player layout
                        AnimatedVisibility(
                            visible = playingMedia != null,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        ) {
                            playingMedia?.let { media ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Transparent)
                                ) {
                                    OfflinePlayer(
                                        media = media,
                                        onClose = { playingMedia = null },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("active_offline_player")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar() {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circle Logo Container matching class "w-10 h-10 rounded-full bg-[#EADDFF] flex items-center justify-center text-[#21005D]"
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflineBolt,
                        contentDescription = "TubeFlow App logo Icon",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "TubeFlow",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                )
            }
        },
        actions = {
            // Offline security badge matches HTML/M3 clean aesthetics
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Secure status active badge",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ad-free",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}
