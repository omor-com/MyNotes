package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.FolderEntity
import com.example.data.local.entities.NoteWithDetails
import com.example.data.local.entities.TagEntity
import com.example.data.preferences.SortOrder
import com.example.data.preferences.ViewMode
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val pinnedNotes: List<NoteWithDetails> = emptyList(),
    val recentNotes: List<NoteWithDetails> = emptyList(),
    val allNotesCount: Int = 0,
    val folders: List<FolderEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val selectedFolderId: Long? = null,
    val selectedTagId: Long? = null,
    val viewMode: ViewMode = ViewMode.GRID,
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val isLoading: Boolean = false
)

private data class HomeFilterOptions(
    val viewMode: ViewMode,
    val sortOrder: SortOrder,
    val selectedFolderId: Long?,
    val selectedTagId: Long?
)

class HomeViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    private val _selectedTagId = MutableStateFlow<Long?>(null)

    private val filterOptionsFlow = combine(
        repository.viewMode,
        repository.sortOrder,
        _selectedFolderId,
        _selectedTagId
    ) { mode, sort, folderId, tagId ->
        HomeFilterOptions(mode, sort, folderId, tagId)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.activeNotes,
        repository.allFolders,
        repository.allTags,
        filterOptionsFlow
    ) { notes, folders, tags, filters ->
        // Apply folder filter
        var filtered = if (filters.selectedFolderId != null) {
            notes.filter { it.note.folderId == filters.selectedFolderId }
        } else {
            notes
        }

        // Apply tag filter
        if (filters.selectedTagId != null) {
            filtered = filtered.filter { noteWithDetails ->
                noteWithDetails.tags.any { it.id == filters.selectedTagId }
            }
        }

        // Apply sort order
        val sorted = when (filters.sortOrder) {
            SortOrder.MODIFIED_DESC -> filtered.sortedWith(
                compareByDescending<NoteWithDetails> { it.note.isPinned }
                    .thenByDescending { it.note.updatedAt }
            )
            SortOrder.MODIFIED_ASC -> filtered.sortedWith(
                compareByDescending<NoteWithDetails> { it.note.isPinned }
                    .thenBy { it.note.updatedAt }
            )
            SortOrder.CREATED_DESC -> filtered.sortedWith(
                compareByDescending<NoteWithDetails> { it.note.isPinned }
                    .thenByDescending { it.note.createdAt }
            )
            SortOrder.TITLE_ASC -> filtered.sortedWith(
                compareByDescending<NoteWithDetails> { it.note.isPinned }
                    .thenBy { it.note.title.lowercase() }
            )
        }

        val pinned = sorted.filter { it.note.isPinned }
        val recent = sorted.filter { !it.note.isPinned }

        HomeUiState(
            pinnedNotes = pinned,
            recentNotes = recent,
            allNotesCount = notes.size,
            folders = folders,
            tags = tags,
            selectedFolderId = filters.selectedFolderId,
            selectedTagId = filters.selectedTagId,
            viewMode = filters.viewMode,
            sortOrder = filters.sortOrder,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

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

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.setDeleted(noteId, true)
        }
    }

    fun restoreNote(noteId: Long) {
        viewModelScope.launch {
            repository.setDeleted(noteId, false)
        }
    }

    fun selectFolderFilter(folderId: Long?) {
        _selectedFolderId.value = if (_selectedFolderId.value == folderId) null else folderId
    }

    fun selectTagFilter(tagId: Long?) {
        _selectedTagId.value = if (_selectedTagId.value == tagId) null else tagId
    }

    fun toggleViewMode() {
        viewModelScope.launch {
            val next = if (uiState.value.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
            repository.setViewMode(next)
        }
    }

    fun changeSortOrder(order: SortOrder) {
        viewModelScope.launch {
            repository.setSortOrder(order)
        }
    }
}
