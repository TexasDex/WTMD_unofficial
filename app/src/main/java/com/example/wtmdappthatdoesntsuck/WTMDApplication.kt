package com.example.wtmdappthatdoesntsuck

import android.app.Application
import androidx.room.Room
import com.example.wtmdappthatdoesntsuck.data.api.WTMDService
import com.example.wtmdappthatdoesntsuck.data.local.PreferenceManager
import com.example.wtmdappthatdoesntsuck.data.local.SongDatabase
import com.example.wtmdappthatdoesntsuck.data.repository.SongRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WTMDApplication : Application() {

    private val database by lazy {
        Room.databaseBuilder(this, SongDatabase::class.java, "wtmd_database")
            .fallbackToDestructiveMigration()
            .build()
    }

    private val preferenceManager by lazy {
        PreferenceManager(this)
    }

    private val apiService by lazy {
        Retrofit.Builder()
            .baseUrl(WTMDService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WTMDService::class.java)
    }

    val repository by lazy {
        SongRepository(apiService, database.songDao(), preferenceManager)
    }
}
