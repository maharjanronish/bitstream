package com.bitstream.tv.show

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitstream.tv.core.Episode
import com.bitstream.tv.core.Tmdb
import com.bitstream.tv.core.TvDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Season/episode picker state for one TV show. */
class ShowViewModel : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val error: Boolean = false,
        val show: TvDetail? = null,
        val season: Int = 1,
        val episodes: List<Episode> = emptyList(),
        val seasonLoading: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val seasonCache = mutableMapOf<Int, List<Episode>>()
    private var tmdbId: Long = -1
    private var started = false

    fun load(id: Long, title: String) {
        if (started) return
        started = true
        tmdbId = id
        _state.value = UiState(loading = true)
        viewModelScope.launch {
            try {
                val show = Tmdb.tvShow(id)
                _state.value = UiState(loading = false, show = show)
                loadSeason(1)
            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = true)
            }
        }
    }

    fun selectSeason(season: Int) {
        val show = _state.value.show ?: return
        val target = season.coerceIn(1, show.number_of_seasons)
        if (target == _state.value.season && _state.value.episodes.isNotEmpty()) return
        _state.value = _state.value.copy(season = target)
        loadSeason(target)
    }

    fun retry() {
        if (_state.value.show == null) load(tmdbId, "")
        else loadSeason(_state.value.season)
    }

    private fun loadSeason(season: Int) {
        seasonCache[season]?.let { cached ->
            _state.value = _state.value.copy(episodes = cached, seasonLoading = false)
            return
        }
        _state.value = _state.value.copy(seasonLoading = true)
        viewModelScope.launch {
            try {
                val episodes = Tmdb.season(tmdbId, season)
                seasonCache[season] = episodes
                if (_state.value.season == season) {
                    _state.value = _state.value.copy(episodes = episodes, seasonLoading = false)
                }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(episodes = emptyList(), seasonLoading = false)
            }
        }
    }
}
