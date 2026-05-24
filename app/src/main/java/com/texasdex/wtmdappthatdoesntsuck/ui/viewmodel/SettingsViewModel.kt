package com.texasdex.wtmdappthatdoesntsuck.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.texasdex.wtmdappthatdoesntsuck.data.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SongRepository) : ViewModel() {

    private val _apiUrl = MutableStateFlow(repository.getApiUrl())
    val apiUrl: StateFlow<String> = _apiUrl

    private val _preferredService = MutableStateFlow(repository.getPreferredService())
    val preferredService: StateFlow<String> = _preferredService

    fun setApiUrl(url: String) {
        _apiUrl.value = url
        repository.setApiUrl(url)
    }

    fun setPreferredService(service: String) {
        _preferredService.value = service
        repository.setPreferredService(service)
    }

    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportLikedSongsToJson()
            onResult(json)
        }
    }

    fun importBackup(json: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.importLikedSongsFromJson(json)
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        val MUSIC_SERVICES = listOf("None", "Spotify", "YouTube Music", "Apple Music", "Tidal", "Amazon Music")
    }

    class Factory(private val repository: SongRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
