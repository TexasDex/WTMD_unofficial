package com.texasdex.wtmdappthatdoesntsuck.domain.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val timestamp: String,
    val coverArt: String?,
    val isLiked: Boolean = false,
    val isLikedBefore: Boolean = false,
    val likeCount: Int = 0,
    val likedAtYear: Int? = null,
    val localCoverArtPath: String? = null,
    val addedAt: Long = 0
)
