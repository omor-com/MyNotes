package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.NoteEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NoteCard
import com.example.ui.viewmodel.FoldersViewModel
import com.example.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderId: Long,
    folderName: String,
    foldersViewModel: FoldersViewModel,
    homeViewModel: HomeViewModel,
    onNavigateToEditor: (Long, Long?, String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notesState = remember(folderId) { foldersViewModel.getNotesInFolder(folderId) }
    val notes by notesState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("folder_detail_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onNavigateToEditor(0L, folderId, NoteEntity.CONTENT_TYPE_RICH_TEXT)
                },
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("add_note_to_folder_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Note to this Folder"
                )
            }
        }
    ) { innerPadding ->
        if (notes.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.Description,
                title = "Folder is empty",
                subtitle = "There are no notes in '$folderName' yet.",
                actionButtonText = "Create Note Here",
                onActionClick = {
                    onNavigateToEditor(0L, folderId, NoteEntity.CONTENT_TYPE_RICH_TEXT)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("folder_notes_list")
            ) {
                items(notes, key = { it.note.id }) { noteWithDetails ->
                    NoteCard(
                        noteWithDetails = noteWithDetails,
                        isGridView = false,
                        onClick = { onNavigateToEditor(noteWithDetails.note.id, folderId, null) },
                        onPinToggle = { homeViewModel.togglePin(noteWithDetails.note.id, noteWithDetails.note.isPinned) },
                        onFavoriteToggle = { homeViewModel.toggleFavorite(noteWithDetails.note.id, noteWithDetails.note.isFavorite) },
                        onDelete = {
                            val id = noteWithDetails.note.id
                            homeViewModel.deleteNote(id)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Note moved to trash",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    homeViewModel.restoreNote(id)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
