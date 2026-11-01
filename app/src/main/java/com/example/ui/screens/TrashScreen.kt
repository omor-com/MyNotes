package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.NoteWithDetails
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.TrashViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trashNotes by viewModel.trashNotes.collectAsState()
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var noteToDeletePermanently by remember { mutableStateOf<NoteWithDetails?>(null) }

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("trash_back_button")
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
                        text = "Trash",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    if (trashNotes.isNotEmpty()) {
                        TextButton(
                            onClick = { showEmptyTrashDialog = true },
                            modifier = Modifier.testTag("empty_trash_button")
                        ) {
                            Text("Empty", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (trashNotes.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.DeleteForever,
                title = "Trash is empty",
                subtitle = "Notes that you delete will appear here before being permanently removed.",
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
                    bottom = innerPadding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("trash_notes_list")
            ) {
                items(trashNotes, key = { it.note.id }) { item ->
                    TrashNoteCard(
                        noteWithDetails = item,
                        onRestore = { viewModel.restoreNote(item.note.id) },
                        onDeletePermanently = { noteToDeletePermanently = item }
                    )
                }
            }
        }
    }

    // Empty Trash confirmation dialog
    if (showEmptyTrashDialog) {
        DeleteConfirmationDialog(
            title = "Empty Trash?",
            message = "All ${trashNotes.size} notes in trash will be permanently deleted. This cannot be undone.",
            confirmButtonText = "Empty Trash",
            onDismiss = { showEmptyTrashDialog = false },
            onConfirm = {
                viewModel.emptyTrash()
                showEmptyTrashDialog = false
            }
        )
    }

    // Delete single note permanently confirmation
    noteToDeletePermanently?.let { item ->
        DeleteConfirmationDialog(
            title = "Delete Permanently?",
            message = "Are you sure you want to delete '${item.note.title.ifBlank { "Untitled" }}'? This cannot be undone.",
            confirmButtonText = "Delete Permanently",
            onDismiss = { noteToDeletePermanently = null },
            onConfirm = {
                viewModel.deletePermanently(item.note.id)
                noteToDeletePermanently = null
            }
        )
    }
}

@Composable
private fun TrashNoteCard(
    noteWithDetails: NoteWithDetails,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val note = noteWithDetails.note

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Restore action
            IconButton(
                onClick = onRestore,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("restore_note_${note.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = "Restore Note",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Delete permanently action
            IconButton(
                onClick = onDeletePermanently,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("delete_permanent_note_${note.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteForever,
                    contentDescription = "Delete Permanently",
                    tint = Color(0xFFE53935)
                )
            }
        }
    }
}
