package com.example.wtmdappthatdoesntsuck.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.wtmdappthatdoesntsuck.domain.model.Song
import java.net.URLEncoder

object MusicServiceHelper {

    fun openSongInService(context: Context, song: Song, service: String) {
        val query = URLEncoder.encode("${song.artist} ${song.title}", "UTF-8")
        val uri = when (service) {
            "Spotify" -> Uri.parse("https://open.spotify.com/search/$query")
            "YouTube Music" -> Uri.parse("https://music.youtube.com/search?q=$query")
            "Apple Music" -> Uri.parse("https://music.apple.com/search?term=$query")
            "Tidal" -> Uri.parse("https://listen.tidal.com/search/$query")
            "Amazon Music" -> Uri.parse("https://music.amazon.com/search/$query")
            else -> Uri.parse("https://www.google.com/search?q=$query")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }

    fun exportToPlaylistInService(context: Context, songs: List<Song>, service: String) {
        val query = songs.joinToString("\n") { "${it.artist} - ${it.title}" }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, query)
        }
        context.startActivity(Intent.createChooser(intent, "Share song list to $service"))
    }
}
