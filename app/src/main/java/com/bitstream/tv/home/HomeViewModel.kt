package com.bitstream.tv.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitstream.tv.core.Title
import com.bitstream.tv.core.Tmdb
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    data class Row(
        val type: String, // "movie" | "tv"
        val items: List<Title>,
        val page: Int,
        val endReached: Boolean,
    )

    data class UiState(
        val loading: Boolean = true,
        val error: Boolean = false,
        val movieRow: Row? = null,
        val tvRow: Row? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loadingMore = false

    init {
        refresh()
    }

    fun refresh() {
        _state.value = UiState(loading = true)
        viewModelScope.launch {
            try {
                val movies = async { Tmdb.trendingMovies(1) }
                val tv = async { Tmdb.trendingTv(1) }
                _state.value = UiState(
                    loading = false,
                    movieRow = Row("movie", movies.await(), 1, false),
                    tvRow = Row("tv", tv.await(), 1, false),
                )
            } catch (t: Throwable) {
                _state.value = UiState(loading = false, error = true)
            }
        }
    }

    /** Called when a poster row is close to its end — appends the next page. */
    fun loadMore(type: String) {
        if (loadingMore) return
        val snapshot = _state.value
        val row = if (type == "movie") snapshot.movieRow ?: return else snapshot.tvRow ?: return
        if (row.endReached || row.page >= MAX_PAGES) return
        loadingMore = true
        viewModelScope.launch {
            try {
                val next = row.page + 1
                val more = if (type == "movie") Tmdb.trendingMovies(next) else Tmdb.trendingTv(next)
                val newRow = row.copy(items = row.items + more, page = next, endReached = more.isEmpty())
                _state.value =
                    if (type == "movie") snapshot.copy(movieRow = newRow)
                    else snapshot.copy(tvRow = newRow)
            } catch (t: Throwable) {
                // keep the items we already have; next focus event retries
            } finally {
                loadingMore = false
            }
        }
    }

    companion object {
        private const val MAX_PAGES = 5 // ~100 items per row is plenty for a lean TV UI
    }
}
