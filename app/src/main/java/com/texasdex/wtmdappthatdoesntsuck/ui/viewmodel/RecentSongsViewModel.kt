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

    private val _isIdle = MutableStateFlow(false)
    val isIdle: StateFlow<Boolean> = _isIdle

    private var lastActivityTime = System.currentTimeMillis()

    init {
        loadRecentSongs(isUserInitiated = true)
        startAutoRefresh()
    }

    fun loadRecentSongs(isUserInitiated: Boolean = false) {
        if (isUserInitiated) {
            lastActivityTime = System.currentTimeMillis()
            _isIdle.value = false
        }
        if (_isLoading.value) return

        _isLoading.value = true
        viewModelScope.launch {
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
                // Stop refreshing if inactive for more than 1 hour (3,600,000 ms)
                if (System.currentTimeMillis() - lastActivityTime > 3600000) {
                    _isIdle.value = true
                    continue
                }
                _isIdle.value = false
                loadRecentSongs(isUserInitiated = false)
            }
        }
    }

    fun toggleLike(song: Song) {
        lastActivityTime = System.currentTimeMillis()
        _isIdle.value = false
        viewModelScope.launch {
            repository.toggleLike(song)
            // Refresh to update isLiked state
            loadRecentSongs(isUserInitiated = false)
        }
    }

    class Factory(private val repository: SongRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecentSongsViewModel(repository) as T
        }
    }
}
