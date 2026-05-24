package com.example.wtmdappthatdoesntsuck.data.api

import retrofit2.http.GET
import retrofit2.http.Url

interface WTMDService {
    @GET
    suspend fun getRecentSongs(@Url url: String): PlaylistResponse

    companion object {
        const val DEFAULT_URL = "https://api.airkast.net/getPlaylistWebMaster/TlRFNE5pMHlNelE9?sig=a928790373f0c2aa626a3eb1bcdb0ca28c961d71edb8bafc54ab1771ca51ab23"
        const val BASE_URL = "https://api.airkast.net/"
    }
}
