package com.texasdex.wtmdappthatdoesntsuck.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.texasdex.wtmdappthatdoesntsuck.data.repository.SongRepository
import com.texasdex.wtmdappthatdoesntsuck.domain.model.Song
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class LikedSongsViewModel(private val repository: SongRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortOrder = MutableStateFlow(SortOrder.RECENT)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _selectedYear = MutableStateFlow<Int?>(null)
    val selectedYear: StateFlow<Int?> = _selectedYear

    val availableYears: StateFlow<List<Int>> = repository.getLikedSongs()
        .map { songs -> songs.mapNotNull { it.likedAtYear }.distinct().sortedDescending() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.getLikedSongs().first().let { allSongs ->
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                if (allSongs.any { it.likedAtYear == currentYear }) {
                    _selectedYear.value = currentYear
                }
            }
        }
    }

    val likedSongs: StateFlow<List<Song>> = combine(
        repository.getLikedSongs(),
        _searchQuery,
        _sortOrder,
        _selectedYear
    ) { songs, query, sort, year ->
        songs.filter {
            (it.artist.contains(query, ignoreCase = true) || it.title.contains(query, ignoreCase = true)) &&
                    (year == null || it.likedAtYear == year)
        }.let { filtered ->
            when (sort) {
                SortOrder.ARTIST -> filtered.sortedBy { it.artist }
                SortOrder.TITLE -> filtered.sortedBy { it.title }
                SortOrder.RECENT -> filtered.reversed()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedYear(year: Int?) {
        _selectedYear.value = year
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun removeLike(song: Song) {
        viewModelScope.launch {
            repository.toggleLike(song)
        }
    }

    enum class SortOrder { ARTIST, TITLE, RECENT }

    class Factory(private val repository: SongRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LikedSongsViewModel(repository) as T
        }
    }
}
