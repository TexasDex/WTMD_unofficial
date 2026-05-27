package com.texasdex.wtmdappthatdoesntsuck.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "liked_songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songId: String,
    val title: String,
    val artist: String,
    val timestamp: String,
    val coverArt: String?,
    val likedAtYear: Int,
    val localCoverArtPath: String? = null
)
