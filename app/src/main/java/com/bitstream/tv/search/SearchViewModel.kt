package com.bitstream.tv.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitstream.tv.core.Title
import com.bitstream.tv.core.Tmdb
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    data class UiState(
        val query: String = "",
        val searching: Boolean = false,
        val searched: Boolean = false, // at least one search completed
        val error: Boolean = false,
        val results: List<Title> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var job: Job? = null

    fun onQueryChanged(text: String) {
        _state.value = _state.value.copy(query = text)
        job?.cancel()
        val query = text.trim()
        if (query.length < 2) {
            _state.value =
                _state.value.copy(searching = false, searched = false, error = false, results = emptyList())
            return
        }
        job = viewModelScope.launch {
            delay(500) // debounce while the user types
            runSearch(query)
        }
    }

    /** Immediate search (IME action). */
    fun searchNow() {
        val query = _state.value.query.trim()
        if (query.length < 2) return
        job?.cancel()
        job = viewModelScope.launch { runSearch(query) }
    }

    fun retry() = searchNow()

    private suspend fun runSearch(query: String) {
        _state.value = _state.value.copy(searching = true, error = false)
        try {
            val results = Tmdb.search(query)
            _state.value = _state.value.copy(searching = false, searched = true, results = results)
        } catch (t: Throwable) {
            _state.value = _state.value.copy(searching = false, searched = true, error = true)
        }
    }
}
