package com.texasdex.wtmdappthatdoesntsuck

import android.app.Application
import androidx.room.Room
import com.texasdex.wtmdappthatdoesntsuck.data.api.WTMDService
import com.texasdex.wtmdappthatdoesntsuck.data.local.PreferenceManager
import com.texasdex.wtmdappthatdoesntsuck.data.local.SongDatabase
import com.texasdex.wtmdappthatdoesntsuck.data.repository.SongRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WTMDApplication : Application() {

    private val database by lazy {
        Room.databaseBuilder(this, SongDatabase::class.java, "wtmd_database")
            .addMigrations(SongDatabase.MIGRATION_3_4, SongDatabase.MIGRATION_4_5)
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
        SongRepository(this, apiService, database.songDao(), preferenceManager)
    }
}
