package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.NoteWithDetails
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SearchFilter {
    ALL,
    PINNED,
    FAVORITES
}

data class SearchUiState(
    val query: String = "",
    val activeFilter: SearchFilter = SearchFilter.ALL,
    val results: List<NoteWithDetails> = emptyList(),
    val isSearching: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _filter = MutableStateFlow(SearchFilter.ALL)

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _filter,
        _query.flatMapLatest { q ->
            if (q.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchNotes(q.trim())
            }
        }
    ) { query, filter, searchResults ->
        val filtered = when (filter) {
            SearchFilter.ALL -> searchResults
            SearchFilter.PINNED -> searchResults.filter { it.note.isPinned }
            SearchFilter.FAVORITES -> searchResults.filter { it.note.isFavorite }
        }

        SearchUiState(
            query = query,
            activeFilter = filter,
            results = filtered,
            isSearching = query.isNotBlank()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun clearQuery() {
        _query.value = ""
    }

    fun setFilter(filter: SearchFilter) {
        _filter.value = filter
    }

    fun togglePin(noteId: Long, current: Boolean) {
        viewModelScope.launch {
            repository.setPinned(noteId, !current)
        }
    }

    fun toggleFavorite(noteId: Long, current: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(noteId, !current)
        }
    }
}
