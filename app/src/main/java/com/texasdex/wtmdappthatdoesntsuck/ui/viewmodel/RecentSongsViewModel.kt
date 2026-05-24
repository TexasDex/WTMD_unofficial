package com.texasdex.wtmdappthatdoesntsuck.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.texasdex.wtmdappthatdoesntsuck.data.repository.SongRepository
import com.texasdex.wtmdappthatdoesntsuck.domain.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecentSongsViewModel(private val repository: SongRepository) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadRecentSongs()
        startAutoRefresh()
    }

    fun loadRecentSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _songs.value = repository.getRecentSongs()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(60000)
                try {
                    _songs.value = repository.getRecentSongs()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            repository.toggleLike(song)
            // Refresh to update isLiked state
            loadRecentSongs()
        }
    }

    class Factory(private val repository: SongRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecentSongsViewModel(repository) as T
        }
    }
}
