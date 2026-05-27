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
            val isLikedSpecific = songDao.isLikedSpecific(item.songId, cleanTimestamp)
            val isLikedEver = songDao.isLikedEver(item.songId)
            val count = songDao.getLikeCount(item.songId)
            val existing = songDao.getSongById(item.songId)
            
            Song(
                id = item.songId,
                title = item.title,
                artist = item.artist,
                timestamp = cleanTimestamp,
                coverArt = item.coverArt?.replace("http://", "https://"),
                isLiked = isLikedSpecific,
                isLikedBefore = isLikedEver && !isLikedSpecific,
                likeCount = count,
                localCoverArtPath = existing?.localCoverArtPath
            )
        }
    }

    fun getLikedSongs(): Flow<List<Song>> {
        return songDao.getAllLikedSongs().map { entities ->
            entities.groupBy { it.songId }.map { (songId, instances) ->
                val first = instances.first()
                Song(
                    id = songId,
                    title = first.title,
                    artist = first.artist,
                    timestamp = first.timestamp,
                    coverArt = first.coverArt?.replace("http://", "https://"),
                    isLiked = true,
                    likeCount = instances.size,
                    likedAtYear = first.likedAtYear,
                    localCoverArtPath = first.localCoverArtPath
                )
            }
        }
    }

    suspend fun toggleLike(song: Song) = withContext(Dispatchers.IO) {
        val isLikedSpecific = songDao.isLikedSpecific(song.id, song.timestamp)
        if (isLikedSpecific) {
            songDao.deleteSpecificLike(song.id, song.timestamp)
            if (!songDao.isLikedEver(song.id)) {
                val existing = songDao.getSongById(song.id)
                existing?.localCoverArtPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
            }
        } else {
            val existing = songDao.getSongById(song.id)
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

    suspend fun removeAllLikes(songId: String) = withContext(Dispatchers.IO) {
        val existing = songDao.getSongById(songId)
        existing?.localCoverArtPath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
        songDao.deleteAllLikesForSong(songId)
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
        val entities: List<SongEntity> = gson.fromJson(json, type)
        entities.forEach { entity ->
            val localPath = downloadAndSaveArt(entity.songId, entity.coverArt)
            songDao.insertSong(entity.copy(localCoverArtPath = localPath))
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
