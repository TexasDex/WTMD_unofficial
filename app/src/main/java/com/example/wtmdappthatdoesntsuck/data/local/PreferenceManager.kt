package com.example.wtmdappthatdoesntsuck.data.local

import android.content.Context
import com.example.wtmdappthatdoesntsuck.data.api.WTMDService

class PreferenceManager(context: Context) {
    private val prefs = context.getSharedPreferences("wtmd_prefs", Context.MODE_PRIVATE)

    fun getApiUrl(): String {
        return prefs.getString("api_url", WTMDService.DEFAULT_URL) ?: WTMDService.DEFAULT_URL
    }

    fun setApiUrl(url: String) {
        prefs.edit().putString("api_url", url).apply()
    }

    fun getPreferredService(): String {
        return prefs.getString("preferred_service", "NOT_SET") ?: "NOT_SET"
    }

    fun setPreferredService(service: String) {
        prefs.edit().putString("preferred_service", service).apply()
    }
}
