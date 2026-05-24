package com.example.wtmdappthatdoesntsuck.data.repository

import com.example.wtmdappthatdoesntsuck.data.api.WTMDService
import com.example.wtmdappthatdoesntsuck.data.local.PreferenceManager
import com.example.wtmdappthatdoesntsuck.data.local.SongDao
import com.example.wtmdappthatdoesntsuck.data.local.SongEntity
import com.example.wtmdappthatdoesntsuck.domain.model.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.*

class SongRepository(
    private val apiService: WTMDService,
    private val songDao: SongDao,
    private val preferenceManager: PreferenceManager
) {
    private val gson = Gson()

    private val _preferredService = MutableStateFlow(preferenceManager.getPreferredService())
    val preferredServiceFlow: Flow<String> = _preferredService

    suspend fun getRecentSongs(): List<Song> {
        val url = preferenceManager.getApiUrl()
        val response = apiService.getRecentSongs(url)
        return response.items.map { item ->
            Song(
                id = item.songId,
                title = item.title,
                artist = item.artist,
                timestamp = item.timestamp.replace(" (et)", ""),
                coverArt = item.coverArt?.replace("http://", "https://"),
                isLiked = songDao.isLiked(item.songId)
            )
        }
    }

    fun getLikedSongs(): Flow<List<Song>> {
        return songDao.getAllLikedSongs().map { entities ->
            entities.map { entity ->
                Song(
                    id = entity.songId,
                    title = entity.title,
                    artist = entity.artist,
                    timestamp = entity.timestamp.replace(" (et)", ""),
                    coverArt = entity.coverArt?.replace("http://", "https://"),
                    isLiked = true,
                    likedAtYear = entity.likedAtYear
                )
            }
        }
    }

    suspend fun toggleLike(song: Song) {
        if (song.isLiked) {
            songDao.deleteSong(
                SongEntity(
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    timestamp = song.timestamp,
                    coverArt = song.coverArt,
                    likedAtYear = 0 // Not needed for deletion
                )
            )
        } else {
            songDao.insertSong(
                SongEntity(
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    timestamp = song.timestamp,
                    coverArt = song.coverArt,
                    likedAtYear = Calendar.getInstance().get(Calendar.YEAR)
                )
            )
        }
    }

    suspend fun exportLikedSongsToJson(): String {
        val entities = songDao.getAllLikedSongs().first()
        return gson.toJson(entities)
    }

    suspend fun importLikedSongsFromJson(json: String) {
        val type = object : TypeToken<List<SongEntity>>() {}.type
        val entities: List<SongEntity> = gson.fromJson(json, type)
        entities.forEach { songDao.insertSong(it) }
    }

    fun getApiUrl() = preferenceManager.getApiUrl()
    fun setApiUrl(url: String) = preferenceManager.setApiUrl(url)

    fun getPreferredService() = preferenceManager.getPreferredService()
    fun setPreferredService(service: String) {
        preferenceManager.setPreferredService(service)
        _preferredService.value = service
    }
}
