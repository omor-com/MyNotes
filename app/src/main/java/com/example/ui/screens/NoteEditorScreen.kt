package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.NoteEntity
import com.example.data.local.entities.RichSpan
import com.example.ui.components.ChecklistEditor
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.FolderChip
import com.example.ui.components.PastelFolderColors
import com.example.ui.components.RichTextToolbar
import com.example.ui.components.TagChip
import com.example.ui.components.TagManagerDialog
import com.example.ui.viewmodel.NoteEditorViewModel
import com.example.ui.viewmodel.SaveStatus

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }

    // Intercept hardware and gesture back
    BackHandler {
        viewModel.performSave {
            onBack()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.performSave {
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    // Save status indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (uiState.saveStatus) {
                            SaveStatus.SAVING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Saving…",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SaveStatus.SAVED -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFF43A047)
                                )
                                Text(
                                    text = "Saved",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SaveStatus.IDLE -> Unit
                        }
                    }
                },
                actions = {
                    // Favorite Toggle
                    IconButton(
                        onClick = { viewModel.toggleFavorite() },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("editor_favorite_toggle")
                    ) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (uiState.isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Pin Toggle
                    IconButton(
                        onClick = { viewModel.togglePin() },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("editor_pin_toggle")
                    ) {
                        Icon(
                            imageVector = if (uiState.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            tint = if (uiState.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // More Menu
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("editor_more_menu")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // Assign Folder
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                                text = { Text(if (uiState.currentFolder != null) "Folder: ${uiState.currentFolder?.name}" else "Assign Folder") },
                                onClick = {
                                    showMenu = false
                                    showFolderMenu = true
                                },
                                modifier = Modifier.testTag("menu_assign_folder")
                            )

                            // Manage Tags
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.Label, contentDescription = null) },
                                text = { Text("Tags (${uiState.selectedTags.size})") },
                                onClick = {
                                    showMenu = false
                                    showTagDialog = true
                                },
                                modifier = Modifier.testTag("menu_manage_tags")
                            )

                            // Note Card Color
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.ColorLens, contentDescription = null) },
                                text = { Text("Note Color") },
                                onClick = {
                                    showMenu = false
                                    showColorMenu = true
                                },
                                modifier = Modifier.testTag("menu_change_color")
                            )

                            // Convert format
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.SwapHoriz, contentDescription = null) },
                                text = {
                                    Text(
                                        if (uiState.contentType == NoteEntity.CONTENT_TYPE_CHECKLIST) "Convert to Rich Text" else "Convert to Checklist"
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.convertToTodoList()
                                },
                                modifier = Modifier.testTag("menu_convert_format")
                            )

                            // Duplicate note
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                                text = { Text("Duplicate Note") },
                                onClick = {
                                    showMenu = false
                                    viewModel.duplicateNote {
                                        viewModel.performSave { onBack() }
                                    }
                                },
                                modifier = Modifier.testTag("menu_duplicate_note")
                            )

                            // Share text
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                                text = { Text("Share Note") },
                                onClick = {
                                    showMenu = false
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "${uiState.title}\n\n${uiState.bodyFieldValue.text}"
                                        )
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Note")
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier.testTag("menu_share_note")
                            )

                            // Delete / Move to Trash
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFE53935)) },
                                text = { Text("Delete Note", color = Color(0xFFE53935)) },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                },
                                modifier = Modifier.testTag("menu_delete_note")
                            )
                        }

                        // Folder Sub-Dropdown
                        DropdownMenu(
                            expanded = showFolderMenu,
                            onDismissRequest = { showFolderMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (Unorganized)") },
                                onClick = {
                                    viewModel.setFolder(null)
                                    showFolderMenu = false
                                }
                            )
                            uiState.allFolders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.name) },
                                    onClick = {
                                        viewModel.setFolder(folder)
                                        showFolderMenu = false
                                    }
                                )
                            }
                        }

                        // Color Sub-Dropdown
                        DropdownMenu(
                            expanded = showColorMenu,
                            onDismissRequest = { showColorMenu = false }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Card Background Color",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Default White/Dark Surface
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                            .clickable {
                                                viewModel.setNoteColor(null)
                                                showColorMenu = false
                                            }
                                    )

                                    PastelFolderColors.forEach { (hex, _) ->
                                        val color = Color(0xFF000000 or hex.removePrefix("#").toLong(16))
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                                .clickable {
                                                    viewModel.setNoteColor(hex)
                                                    showColorMenu = false
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.contentType == NoteEntity.CONTENT_TYPE_RICH_TEXT) {
                RichTextToolbar(
                    onToggleSpan = { type, value -> viewModel.toggleSpan(type, value) },
                    onInsertBullet = { viewModel.insertBulletList() },
                    onInsertNumbered = { viewModel.insertNumberedList() },
                    onConvertToTodoList = { viewModel.convertToTodoList() },
                    modifier = Modifier.testTag("editor_toolbar")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Folders and Tags Chip Bar
            if (uiState.currentFolder != null || uiState.selectedTags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.currentFolder?.let { folder ->
                        FolderChip(name = folder.name, colorHex = folder.colorHex)
                    }

                    uiState.selectedTags.forEach { tag ->
                        TagChip(name = tag.name)
                    }
                }
            }

            // Note Title (24sp Bold)
            BasicTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChange(it) },
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (uiState.title.isEmpty()) {
                            Text(
                                text = "Title",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Body Area: Rich Text or Checklist
            if (uiState.contentType == NoteEntity.CONTENT_TYPE_CHECKLIST) {
                ChecklistEditor(
                    items = uiState.checklistItems,
                    onItemChange = { idx, item -> viewModel.onChecklistItemChange(idx, item) },
                    onItemDelete = { idx -> viewModel.onDeleteChecklistItem(idx) },
                    onAddItem = { text -> viewModel.onAddChecklistItem(text) },
                    onMoveItem = { from, to -> viewModel.onMoveChecklistItem(from, to) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_checklist_editor")
                )
            } else {
                // Rich Text Visual Transformation
                val spans = uiState.spans
                val richTextTransformation = remember(spans) {
                    VisualTransformation { text ->
                        val annotated = RichSpan.toAnnotatedString(text.text, spans)
                        TransformedText(annotated, OffsetMapping.Identity)
                    }
                }

                BasicTextField(
                    value = uiState.bodyFieldValue,
                    onValueChange = { viewModel.onBodyFieldChange(it) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    visualTransformation = richTextTransformation,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (uiState.bodyFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Start typing your note…",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                        .testTag("note_body_input")
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            title = "Delete Note?",
            message = "This note will be moved to Trash. You can restore it later or permanently delete it.",
            confirmButtonText = "Move to Trash",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteNote {
                    onBack()
                }
            }
        )
    }

    // Tag Manager Dialog
    if (showTagDialog) {
        TagManagerDialog(
            allTags = uiState.allTags,
            initialSelectedTagIds = uiState.selectedTags.map { it.id },
            onDismiss = { showTagDialog = false },
            onCreateTag = { name -> viewModel.createTag(name) },
            onSaveSelectedTags = { tagIds ->
                viewModel.setSelectedTags(tagIds)
                showTagDialog = false
            }
        )
    }
}
