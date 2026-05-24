package com.texasdex.wtmdappthatdoesntsuck.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.texasdex.wtmdappthatdoesntsuck.WTMDApplication
import com.texasdex.wtmdappthatdoesntsuck.domain.model.Song
import com.texasdex.wtmdappthatdoesntsuck.ui.utils.MusicServiceHelper
import com.texasdex.wtmdappthatdoesntsuck.ui.viewmodel.LikedSongsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikedSongsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: (String?) -> Unit
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as WTMDApplication).repository
    val viewModel: LikedSongsViewModel = viewModel(
        factory = LikedSongsViewModel.Factory(repository)
    )

    val songs by viewModel.likedSongs.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val preferredService by repository.preferredServiceFlow.collectAsState(initial = repository.getPreferredService())

    val availableYears = remember(songs) {
        songs.mapNotNull { it.likedAtYear }.distinct().sortedDescending()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Liked Songs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Export")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Export as TXT") },
                            onClick = {
                                showMenu = false
                                exportSongs(context, songs, "txt")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as CSV") },
                            onClick = {
                                showMenu = false
                                exportSongs(context, songs, "csv")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy for $preferredService") },
                            onClick = {
                                showMenu = false
                                MusicServiceHelper.exportToPlaylistInService(context, songs, preferredService)
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Filter by Artist/Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = sortOrder == LikedSongsViewModel.SortOrder.ARTIST,
                    onClick = { viewModel.setSortOrder(LikedSongsViewModel.SortOrder.ARTIST) },
                    label = { Text("Artist") }
                )
                FilterChip(
                    selected = sortOrder == LikedSongsViewModel.SortOrder.TITLE,
                    onClick = { viewModel.setSortOrder(LikedSongsViewModel.SortOrder.TITLE) },
                    label = { Text("Title") }
                )
                FilterChip(
                    selected = sortOrder == LikedSongsViewModel.SortOrder.RECENT,
                    onClick = { viewModel.setSortOrder(LikedSongsViewModel.SortOrder.RECENT) },
                    label = { Text("Recent") }
                )
            }

            if (availableYears.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text("Year: ", modifier = Modifier.align(Alignment.CenterVertically))
                    FilterChip(
                        selected = selectedYear == null,
                        onClick = { viewModel.setSelectedYear(null) },
                        label = { Text("All") }
                    )
                    availableYears.forEach { year ->
                        Spacer(Modifier.width(4.dp))
                        FilterChip(
                            selected = selectedYear == year,
                            onClick = { viewModel.setSelectedYear(year) },
                            label = { Text(year.toString()) }
                        )
                    }
                }
            }

            LazyColumn {
                items(songs) { song ->
                    SongItemRow(
                        song = song,
                        onLikeClick = { viewModel.removeLike(song) },
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

fun exportSongs(context: Context, songs: List<Song>, format: String) {
    val fileName = "liked_songs.$format"
    val content = when (format) {
        "csv" -> {
            val sb = StringBuilder("Title,Artist,Timestamp\n")
            songs.forEach { sb.append("\"${it.title}\",\"${it.artist}\",\"${it.timestamp}\"\n") }
            sb.toString()
        }
        else -> {
            val sb = StringBuilder()
            songs.forEach { sb.append("${it.title} - ${it.artist} (${it.timestamp})\n") }
            sb.toString()
        }
    }

    val file = File(context.cacheDir, fileName)
    file.writeText(content)

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (format == "csv") "text/csv" else "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export Liked Songs"))
}
