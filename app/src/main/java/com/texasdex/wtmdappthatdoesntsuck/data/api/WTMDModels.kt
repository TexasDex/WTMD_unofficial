package com.texasdex.wtmdappthatdoesntsuck.data.api

import com.google.gson.annotations.SerializedName

data class PlaylistResponse(
    @SerializedName("items") val items: List<SongItem>
)

data class SongItem(
    @SerializedName("song_id") val songId: String,
    @SerializedName("line1") val title: String,
    @SerializedName("line2") val artist: String,
    @SerializedName("line3") val timestamp: String,
    @SerializedName("cover_art") val coverArt: String?
)
