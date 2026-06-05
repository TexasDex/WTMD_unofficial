package com.texasdex.wtmdappthatdoesntsuck.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.texasdex.wtmdappthatdoesntsuck.data.api.WTMDService
import com.texasdex.wtmdappthatdoesntsuck.data.local.PreferenceManager
import com.texasdex.wtmdappthatdoesntsuck.data.local.SongDao
import com.texasdex.wtmdappthatdoesntsuck.data.local.SongEntity
import com.texasdex.wtmdappthatdoesntsuck.domain.model.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

class SongRepository(
    private val context: Context,
    private val apiService: WTMDService,
    private val songDao: SongDao,
    private val preferenceManager: PreferenceManager
) {
    private val gson = Gson()

    private val _preferredService = MutableStateFlow(preferenceManager.getPreferredService())
    val preferredServiceFlow: Flow<String> = _preferredService

    suspend fun getRecentSongs(): List<Song> = withContext(Dispatchers.IO) {
        val url = preferenceManager.getApiUrl()
        val response = apiService.getRecentSongs(url)
        response.items.map { item ->
            val cleanTimestamp = item.timestamp.replace(" (et)", "")
            // Specific like (this exact playback) still uses isLikedSpecific for simplicity/efficiency
            // but we could also use fuzzy here if we wanted to be extremely safe.
            // Let's use fuzzy for everything to be consistent with the cross-device requirement.
            val isLikedSpecific = songDao.isLikedFuzzy(item.artist, item.title, cleanTimestamp)
            val isLikedEver = songDao.isLikedEverFuzzy(item.artist, item.title)
            
            // For counts and art, also use artist/title matching
            val count = songDao.getLikeCountFuzzy(item.artist, item.title)
            val existing = songDao.getSongByMetadata(item.artist, item.title)
            
            Song(
                id = item.songId,
                title = item.title,
                artist = item.artist,
                timestamp = cleanTimestamp,
                coverArt = item.coverArt?.replace("http://", "https://"),
                isLiked = isLikedSpecific,
                isLikedBefore = isLikedEver && !isLikedSpecific,
                likeCount = count,
                localCoverArtPath = existing?.localCoverArtPath,
                addedAt = existing?.addedAt ?: 0L
            )
        }
    }

    fun getLikedSongs(): Flow<List<Song>> {
        return songDao.getAllLikedSongs().map { entities ->
            entities.groupBy { it.artist + "|" + it.title }.map { (_, instances) ->
                val first = instances.first()
                // Use the most recent 'addedAt' for the group's timestamp
                val mostRecentAddedAt = instances.maxOf { it.addedAt }
                Song(
                    id = first.songId,
                    title = first.title,
                    artist = first.artist,
                    timestamp = first.timestamp,
                    coverArt = first.coverArt?.replace("http://", "https://"),
                    isLiked = true,
                    likeCount = instances.size,
                    likedAtYear = first.likedAtYear,
                    localCoverArtPath = first.localCoverArtPath,
                    addedAt = mostRecentAddedAt
                )
            }
        }
    }

    suspend fun toggleLike(song: Song) = withContext(Dispatchers.IO) {
        val isLikedSpecific = songDao.isLikedFuzzy(song.artist, song.title, song.timestamp)
        if (isLikedSpecific) {
            songDao.deleteSpecificLikeFuzzy(song.artist, song.title, song.timestamp)
            if (!songDao.isLikedEverFuzzy(song.artist, song.title)) {
                val existing = songDao.getSongByMetadata(song.artist, song.title)
                existing?.localCoverArtPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
            }
        } else {
            val existing = songDao.getSongByMetadata(song.artist, song.title)
            val localPath = existing?.localCoverArtPath ?: downloadAndSaveArt(song.id, song.coverArt)
            songDao.insertSong(
                SongEntity(
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    timestamp = song.timestamp,
                    coverArt = song.coverArt,
                    likedAtYear = Calendar.getInstance().get(Calendar.YEAR),
                    localCoverArtPath = localPath
                )
            )
        }
    }

    suspend fun removeAllLikes(artist: String, title: String) = withContext(Dispatchers.IO) {
        val existing = songDao.getSongByMetadata(artist, title)
        existing?.localCoverArtPath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
        songDao.deleteAllLikesForSongFuzzy(artist, title)
    }

    suspend fun addManualLike(artist: String, title: String) = withContext(Dispatchers.IO) {
        val allLiked = songDao.getAllLikedSongs().first()
        
        fun normalize(s: String) = s.lowercase().replace(Regex("[^a-z0-9]"), "")
        
        val normalizedArtist = normalize(artist)
        val normalizedTitle = normalize(title)
        
        // Find existing match ignoring punctuation/case
        val existingMatch = allLiked.find { 
            normalize(it.artist) == normalizedArtist && normalize(it.title) == normalizedTitle 
        }
        
        // Use existing strings if found to "merge" them into the same grouping
        val finalArtist = existingMatch?.artist ?: artist
        val finalTitle = existingMatch?.title ?: title
        val finalCoverArt = existingMatch?.coverArt
        val localPath = existingMatch?.localCoverArtPath
        
        songDao.insertSong(
            SongEntity(
                songId = existingMatch?.songId ?: "manual_${UUID.randomUUID()}",
                title = finalTitle,
                artist = finalArtist,
                timestamp = "Manual entry - ${java.text.SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date())}",
                coverArt = finalCoverArt,
                likedAtYear = Calendar.getInstance().get(Calendar.YEAR),
                localCoverArtPath = localPath
            )
        )
    }

    private suspend fun downloadAndSaveArt(songId: String, url: String?): String? = withContext(Dispatchers.IO) {
        if (url == null) return@withContext null
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()
        val result = loader.execute(request)
        if (result is SuccessResult) {
            val drawable = result.drawable
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                val directory = File(context.filesDir, "album_art")
                if (!directory.exists()) directory.mkdirs()
                val file = File(directory, "$songId.webp")
                try {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.WEBP, 90, out)
                    }
                    return@withContext file.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return@withContext null
    }

    suspend fun exportLikedSongsToJson(): String = withContext(Dispatchers.IO) {
        val entities = songDao.getAllLikedSongs().first()
        gson.toJson(entities)
    }

    suspend fun importLikedSongsFromJson(json: String) = withContext(Dispatchers.IO) {
        val type = object : TypeToken<List<SongEntity>>() {}.type
        val importedEntities: List<SongEntity> = gson.fromJson(json, type)
        importedEntities.forEach { entity ->
            // Match by artist, title, and timestamp since songId might differ between devices
            val exists = songDao.isLikedFuzzy(entity.artist, entity.title, entity.timestamp)
            if (!exists) {
                // Also check if we already have the art for this song (matched by artist/title)
                val existingSong = songDao.getSongByMetadata(entity.artist, entity.title)
                val localPath = existingSong?.localCoverArtPath ?: downloadAndSaveArt(entity.songId, entity.coverArt)
                // Insert as a new entry (Room will handle the auto-increment 'id')
                songDao.insertSong(entity.copy(id = 0, localCoverArtPath = localPath))
            }
        }
    }

    fun getApiUrl() = preferenceManager.getApiUrl()
    fun setApiUrl(url: String) = preferenceManager.setApiUrl(url)

    fun getPreferredService() = preferenceManager.getPreferredService()
    fun setPreferredService(service: String) {
        preferenceManager.setPreferredService(service)
        _preferredService.value = service
    }
}
