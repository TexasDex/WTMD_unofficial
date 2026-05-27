package com.texasdex.wtmdappthatdoesntsuck.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM liked_songs ORDER BY id DESC")
    fun getAllLikedSongs(): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Delete
    suspend fun deleteSong(song: SongEntity)

    @Query("DELETE FROM liked_songs WHERE songId = :songId AND timestamp = :timestamp")
    suspend fun deleteSpecificLike(songId: String, timestamp: String)

    @Query("DELETE FROM liked_songs WHERE songId = :songId")
    suspend fun deleteAllLikesForSong(songId: String)

    @Query("SELECT COUNT(*) FROM liked_songs WHERE songId = :songId")
    suspend fun getLikeCount(songId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE songId = :songId AND timestamp = :timestamp)")
    suspend fun isLikedSpecific(songId: String, timestamp: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE songId = :songId)")
    suspend fun isLikedEver(songId: String): Boolean

    @Query("SELECT * FROM liked_songs WHERE songId = :songId LIMIT 1")
    suspend fun getSongById(songId: String): SongEntity?
}
