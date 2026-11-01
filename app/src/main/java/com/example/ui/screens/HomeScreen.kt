package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.NoteEntity
import com.example.data.preferences.SortOrder
import com.example.data.preferences.ViewMode
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NoteCard
import com.example.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToEditor: (Long, Long?, String?) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSortMenu by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Header Row: Brand Title + Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MyNotes",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("app_wordmark")
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("home_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Greeting
                    Text(
                        text = "Your thoughts, organized.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Rounded Search Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onNavigateToSearch() }
                            .testTag("home_search_bar"),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Search your notes...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filters and View Mode Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Filter Chips Row (Folders and Tags)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = uiState.selectedFolderId == null && uiState.selectedTagId == null,
                                onClick = {
                                    viewModel.selectFolderFilter(null)
                                    viewModel.selectTagFilter(null)
                                },
                                label = { Text("All (${uiState.allNotesCount})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("filter_all")
                            )

                            uiState.folders.forEach { folder ->
                                val isSelected = uiState.selectedFolderId == folder.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectFolderFilter(folder.id) },
                                    label = { Text(folder.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("filter_folder_${folder.id}")
                                )
                            }

                            uiState.tags.forEach { tag ->
                                val isSelected = uiState.selectedTagId == tag.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTagFilter(tag.id) },
                                    label = { Text("#${tag.name}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("filter_tag_${tag.id}")
                                )
                            }
                        }

                        // Toggle Grid/List & Sort
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.toggleViewMode() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("toggle_view_mode")
                            ) {
                                Icon(
                                    imageVector = if (uiState.viewMode == ViewMode.GRID) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                                    contentDescription = "Toggle Grid/List View",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box {
                                IconButton(
                                    onClick = { showSortMenu = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("sort_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                                        contentDescription = "Sort Notes",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    SortOrder.entries.forEach { order ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = order.label,
                                                    fontWeight = if (uiState.sortOrder == order) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                viewModel.changeSortOrder(order)
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("create_note_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create note"
                    )
                }

                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false }
                ) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        text = { Text("New Note") },
                        onClick = {
                            showFabMenu = false
                            onNavigateToEditor(0L, uiState.selectedFolderId, NoteEntity.CONTENT_TYPE_RICH_TEXT)
                        },
                        modifier = Modifier.testTag("menu_new_rich_note")
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        text = { Text("New Checklist") },
                        onClick = {
                            showFabMenu = false
                            onNavigateToEditor(0L, uiState.selectedFolderId, NoteEntity.CONTENT_TYPE_CHECKLIST)
                        },
                        modifier = Modifier.testTag("menu_new_checklist_note")
                    )
                }
            }
        }
    ) { innerPadding ->
        val totalVisibleNotes = uiState.pinnedNotes.size + uiState.recentNotes.size

        if (totalVisibleNotes == 0) {
            EmptyStateView(
                icon = Icons.Default.Description,
                title = "No notes found",
                subtitle = if (uiState.selectedFolderId != null || uiState.selectedTagId != null) {
                    "No notes match the selected filters."
                } else {
                    "Capture ideas, write thoughts, or create to-do checklists."
                },
                actionButtonText = "Create Note",
                onActionClick = {
                    onNavigateToEditor(0L, uiState.selectedFolderId, NoteEntity.CONTENT_TYPE_RICH_TEXT)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            if (uiState.viewMode == ViewMode.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = innerPadding.calculateBottomPadding() + 80.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notes_grid")
                ) {
                    // Pinned Notes Section
                    if (uiState.pinnedNotes.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            SectionHeader(
                                icon = Icons.Filled.PushPin,
                                title = "PINNED"
                            )
                        }
                        items(uiState.pinnedNotes, key = { "pinned_${it.note.id}" }) { noteWithDetails ->
                            NoteCard(
                                noteWithDetails = noteWithDetails,
                                isGridView = true,
                                onClick = {
                                    onNavigateToEditor(noteWithDetails.note.id, null, null)
                                },
                                onPinToggle = {
                                    viewModel.togglePin(noteWithDetails.note.id, noteWithDetails.note.isPinned)
                                },
                                onFavoriteToggle = {
                                    viewModel.toggleFavorite(noteWithDetails.note.id, noteWithDetails.note.isFavorite)
                                },
                                onDelete = {
                                    val noteId = noteWithDetails.note.id
                                    viewModel.deleteNote(noteId)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Note moved to trash",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreNote(noteId)
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Recent Notes Section
                    if (uiState.recentNotes.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            SectionHeader(
                                title = if (uiState.pinnedNotes.isNotEmpty()) "RECENT" else "ALL NOTES"
                            )
                        }
                        items(uiState.recentNotes, key = { "recent_${it.note.id}" }) { noteWithDetails ->
                            NoteCard(
                                noteWithDetails = noteWithDetails,
                                isGridView = true,
                                onClick = {
                                    onNavigateToEditor(noteWithDetails.note.id, null, null)
                                },
                                onPinToggle = {
                                    viewModel.togglePin(noteWithDetails.note.id, noteWithDetails.note.isPinned)
                                },
                                onFavoriteToggle = {
                                    viewModel.toggleFavorite(noteWithDetails.note.id, noteWithDetails.note.isFavorite)
                                },
                                onDelete = {
                                    val noteId = noteWithDetails.note.id
                                    viewModel.deleteNote(noteId)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Note moved to trash",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreNote(noteId)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // List View
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
                        .testTag("notes_list")
                ) {
                    if (uiState.pinnedNotes.isNotEmpty()) {
                        item {
                            SectionHeader(
                                icon = Icons.Filled.PushPin,
                                title = "PINNED"
                            )
                        }
                        items(uiState.pinnedNotes, key = { "pinned_${it.note.id}" }) { noteWithDetails ->
                            NoteCard(
                                noteWithDetails = noteWithDetails,
                                isGridView = false,
                                onClick = {
                                    onNavigateToEditor(noteWithDetails.note.id, null, null)
                                },
                                onPinToggle = {
                                    viewModel.togglePin(noteWithDetails.note.id, noteWithDetails.note.isPinned)
                                },
                                onFavoriteToggle = {
                                    viewModel.toggleFavorite(noteWithDetails.note.id, noteWithDetails.note.isFavorite)
                                },
                                onDelete = {
                                    val noteId = noteWithDetails.note.id
                                    viewModel.deleteNote(noteId)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Note moved to trash",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreNote(noteId)
                                        }
                                    }
                                }
                            )
                        }
                    }

                    if (uiState.recentNotes.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = if (uiState.pinnedNotes.isNotEmpty()) "RECENT" else "ALL NOTES"
                            )
                        }
                        items(uiState.recentNotes, key = { "recent_${it.note.id}" }) { noteWithDetails ->
                            NoteCard(
                                noteWithDetails = noteWithDetails,
                                isGridView = false,
                                onClick = {
                                    onNavigateToEditor(noteWithDetails.note.id, null, null)
                                },
                                onPinToggle = {
                                    viewModel.togglePin(noteWithDetails.note.id, noteWithDetails.note.isPinned)
                                },
                                onFavoriteToggle = {
                                    viewModel.toggleFavorite(noteWithDetails.note.id, noteWithDetails.note.isFavorite)
                                },
                                onDelete = {
                                    val noteId = noteWithDetails.note.id
                                    viewModel.deleteNote(noteId)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Note moved to trash",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreNote(noteId)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
