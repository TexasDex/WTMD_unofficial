package com.texasdex.wtmdappthatdoesntsuck.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.texasdex.wtmdappthatdoesntsuck.WTMDApplication
import com.texasdex.wtmdappthatdoesntsuck.domain.model.Song
import com.texasdex.wtmdappthatdoesntsuck.ui.utils.MusicServiceHelper
import com.texasdex.wtmdappthatdoesntsuck.ui.viewmodel.RecentSongsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentSongsScreen(
    onNavigateToLiked: () -> Unit,
    onNavigateToSettings: (String?) -> Unit
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as WTMDApplication).repository
    val viewModel: RecentSongsViewModel = viewModel(
        factory = RecentSongsViewModel.Factory(repository)
    )

    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isIdle by viewModel.isIdle.collectAsState()
    val preferredService by repository.preferredServiceFlow.collectAsState(initial = repository.getPreferredService())

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadRecentSongs(isUserInitiated = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recent Songs") },
                actions = {
                    IconButton(onClick = { viewModel.loadRecentSongs(isUserInitiated = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onNavigateToLiked) {
                        Icon(Icons.Default.Favorite, contentDescription = "Liked Songs")
                    }
                    IconButton(onClick = { onNavigateToSettings(null) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.loadRecentSongs(isUserInitiated = true) },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isIdle) {
                    Text(
                        text = "Not auto-fetching due to inactivity, refresh to start",
                        color = Color.Red,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                if (songs.isEmpty() && isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(songs) { song ->
                            SongItemRow(
                                song = song,
                                onLikeClick = { viewModel.toggleLike(song) },
                                onArtClick = {
                                    if (preferredService == "NOT_SET") {
                                        onNavigateToSettings("music")
                                    } else if (preferredService != "None") {
                                        MusicServiceHelper.openSongInService(context, song, preferredService)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongItemRow(song: Song, onLikeClick: () -> Unit, onArtClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.localCoverArtPath ?: song.coverArt,
                contentDescription = "Open in Music Service",
                placeholder = rememberVectorPainter(Icons.Default.MusicNote),
                error = rememberVectorPainter(Icons.Default.MusicNote),
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onArtClick() },
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(song.artist, style = MaterialTheme.typography.bodyMedium)
                Text(song.timestamp, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = onLikeClick) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (song.isLiked) Color.Red else Color.Gray
                )
            }
        }
    }
}
