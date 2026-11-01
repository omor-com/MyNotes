package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.FolderEntity
import com.example.data.local.entities.FolderWithCount
import com.example.data.local.entities.NoteWithDetails
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FoldersUiState(
    val foldersWithCount: List<FolderWithCount> = emptyList(),
    val isLoading: Boolean = false
)

class FoldersViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    val uiState: StateFlow<FoldersUiState> = repository.foldersWithCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        ).let { flow ->
            MutableStateFlow(FoldersUiState()).apply {
                viewModelScope.launch {
                    flow.collect { list ->
                        value = FoldersUiState(foldersWithCount = list, isLoading = false)
                    }
                }
            }
        }

    fun createFolder(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.createFolder(name, colorHex)
        }
    }

    fun renameFolder(folder: FolderEntity, newName: String, newColorHex: String) {
        viewModelScope.launch {
            repository.updateFolder(
                folder.copy(
                    name = newName,
                    colorHex = newColorHex,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
        }
    }

    fun getNotesInFolder(folderId: Long): StateFlow<List<NoteWithDetails>> {
        return repository.getNotesInFolder(folderId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
}
