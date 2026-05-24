package com.example.wtmdappthatdoesntsuck.domain.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val timestamp: String,
    val coverArt: String?,
    val isLiked: Boolean = false,
    val likedAtYear: Int? = null
)
