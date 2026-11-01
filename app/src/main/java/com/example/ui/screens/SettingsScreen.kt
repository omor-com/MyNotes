package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.FontSizePreference
import com.example.data.preferences.SortOrder
import com.example.data.preferences.ThemePreference
import com.example.data.preferences.ViewMode
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showSortOrderDialog by remember { mutableStateOf(false) }
    var showLocalRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showDriveRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    // Google Sign-In Activity Result Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onGoogleSignInResult(result.data) { res ->
            scope.launch {
                res.onSuccess { email ->
                    snackbarHostState.showSnackbar("Connected Google Drive: $email")
                }.onFailure { e ->
                    snackbarHostState.showSnackbar("Google Sign-In failed: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    // SAF Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackup(uri) { result ->
                scope.launch {
                    result.onSuccess { stats ->
                        snackbarHostState.showSnackbar(
                            "Export successful! ${stats.noteCount} notes, ${stats.folderCount} folders saved."
                        )
                    }.onFailure { e ->
                        snackbarHostState.showSnackbar("Export failed: ${e.localizedMessage ?: "Unknown error"}")
                    }
                }
            }
        }
    }

    // SAF Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
            showLocalRestoreConfirmDialog = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Section 1: Google Drive Cloud Backup
            SettingsSectionHeader(title = "GOOGLE DRIVE BACKUP (V3)")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Account row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.googleDriveAccount != null) Icons.Default.CloudDone else Icons.Default.Cloud,
                            contentDescription = null,
                            tint = if (uiState.googleDriveAccount != null) Color(0xFF43A047) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google Drive Account",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = uiState.googleDriveAccount ?: "Not connected (tap to connect)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.googleDriveAccount != null) Color(0xFF43A047) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (uiState.googleDriveAccount != null) {
                            TextButton(
                                onClick = {
                                    viewModel.disconnectGoogleDrive { res ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Disconnected from Google Drive")
                                        }
                                    }
                                }
                            ) {
                                Text("Disconnect", color = Color(0xFFE53935))
                            }
                        } else {
                            Button(
                                onClick = {
                                    googleSignInLauncher.launch(viewModel.getGoogleSignInIntent())
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Connect")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Automatic Daily Backup Switch (always visible)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatic Daily Backup",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Back up notes to Google Drive automatically once per day and on note edits",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            modifier = Modifier.testTag("auto_backup_toggle"),
                            checked = uiState.autoDriveBackupEnabled,
                            onCheckedChange = { isChecked ->
                                viewModel.toggleAutoDriveBackup(isChecked)
                                if (isChecked && uiState.googleDriveAccount == null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Connect Google Drive for automatic cloud backup")
                                    }
                                    googleSignInLauncher.launch(viewModel.getGoogleSignInIntent())
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Last backup status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.lastDriveBackupTimestamp > 0) {
                                "Last synced: ${SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault()).format(Date(uiState.lastDriveBackupTimestamp))}"
                            } else {
                                "No cloud backup created yet"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Drive Actions: Backup Now & Restore (always visible)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("backup_now_button"),
                            onClick = {
                                if (uiState.googleDriveAccount == null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Connect Google Drive first to back up your notes")
                                    }
                                    googleSignInLauncher.launch(viewModel.getGoogleSignInIntent())
                                } else {
                                    viewModel.backupToGoogleDrive { res ->
                                        scope.launch {
                                            res.onSuccess {
                                                snackbarHostState.showSnackbar("Backup uploaded to Google Drive successfully!")
                                            }.onFailure { e ->
                                                snackbarHostState.showSnackbar("Drive upload failed: ${e.localizedMessage ?: "Network error"}")
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !uiState.isDriveOperationInProgress,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (uiState.isDriveOperationInProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Backup Now")
                        }

                        OutlinedButton(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("drive_restore_button"),
                            onClick = {
                                if (uiState.googleDriveAccount == null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Connect Google Drive first to restore backups")
                                    }
                                    googleSignInLauncher.launch(viewModel.getGoogleSignInIntent())
                                } else {
                                    showDriveRestoreConfirmDialog = true
                                }
                            },
                            enabled = !uiState.isDriveOperationInProgress,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Appearance & Preferences
            SettingsSectionHeader(title = "APPEARANCE & DISPLAY")

            SettingsItemCard(
                icon = Icons.Outlined.BrightnessMedium,
                title = "Theme",
                subtitle = when (uiState.theme) {
                    ThemePreference.SYSTEM -> "System Default"
                    ThemePreference.LIGHT -> "Light Theme"
                    ThemePreference.DARK -> "Dark Theme"
                },
                onClick = { showThemeDialog = true },
                testTag = "settings_theme_item"
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsItemCard(
                icon = Icons.Outlined.GridView,
                title = "Default Note View",
                subtitle = if (uiState.viewMode == ViewMode.GRID) "Grid View (2 Columns)" else "List View",
                onClick = {
                    val next = if (uiState.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                    viewModel.setViewMode(next)
                },
                testTag = "settings_view_mode_item"
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsItemCard(
                icon = Icons.Outlined.FormatSize,
                title = "Editor Font Size",
                subtitle = uiState.fontSize.title,
                onClick = { showFontSizeDialog = true },
                testTag = "settings_font_size_item"
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsItemCard(
                icon = Icons.AutoMirrored.Outlined.Sort,
                title = "Sort Order",
                subtitle = uiState.sortOrder.label,
                onClick = { showSortOrderDialog = true },
                testTag = "settings_sort_order_item"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Data & Local Backup
            SettingsSectionHeader(title = "LOCAL BACKUP & TRASH")

            SettingsItemCard(
                icon = Icons.Outlined.Delete,
                title = "Trash",
                subtitle = "View, restore, or permanently delete notes",
                onClick = onNavigateToTrash,
                testTag = "settings_trash_item"
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsItemCard(
                icon = Icons.Outlined.CloudUpload,
                title = "Export Local Backup (JSON)",
                subtitle = "Save notes, checklists, folders, and tags to a device file",
                onClick = {
                    val timestamp = System.currentTimeMillis()
                    exportLauncher.launch("MyNotes_Backup_$timestamp.json")
                },
                testTag = "settings_export_backup"
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsItemCard(
                icon = Icons.Outlined.CloudDownload,
                title = "Restore from Local File",
                subtitle = "Import notes from a previously saved JSON file",
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                },
                testTag = "settings_import_backup"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 4: About App
            SettingsSectionHeader(title = "ABOUT")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "MyNotes",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Version 1.0 (Build 1)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Clean, private, offline-first note-taking application with Google Drive v3 automatic backup and local JSON export/restore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Google Drive Restore Confirmation Dialog
    if (showDriveRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDriveRestoreConfirmDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Restore from Google Drive?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Would you like to merge notes from Google Drive with your local notes, or replace all local data?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDriveRestoreConfirmDialog = false
                        viewModel.restoreFromGoogleDrive(replaceExisting = false) { result ->
                            scope.launch {
                                result.onSuccess { stats ->
                                    snackbarHostState.showSnackbar("Restored ${stats.noteCount} notes from Google Drive!")
                                }.onFailure { e ->
                                    snackbarHostState.showSnackbar("Drive restore error: ${e.localizedMessage ?: "Error"}")
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Merge Notes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDriveRestoreConfirmDialog = false
                        viewModel.restoreFromGoogleDrive(replaceExisting = true) { result ->
                            scope.launch {
                                result.onSuccess { stats ->
                                    snackbarHostState.showSnackbar("Replaced all data with ${stats.noteCount} notes from Drive!")
                                }.onFailure { e ->
                                    snackbarHostState.showSnackbar("Drive restore error: ${e.localizedMessage ?: "Error"}")
                                }
                            }
                        }
                    }
                ) {
                    Text("Replace All", color = Color(0xFFE53935))
                }
            }
        )
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemePreference.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setTheme(theme)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.theme == theme,
                                onClick = {
                                    viewModel.setTheme(theme)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (theme) {
                                    ThemePreference.SYSTEM -> "System Default"
                                    ThemePreference.LIGHT -> "Light Theme"
                                    ThemePreference.DARK -> "Dark Theme"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Font Size Dialog
    if (showFontSizeDialog) {
        AlertDialog(
            onDismissRequest = { showFontSizeDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Editor Font Size", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    FontSizePreference.entries.forEach { size ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setFontSize(size)
                                    showFontSizeDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.fontSize == size,
                                onClick = {
                                    viewModel.setFontSize(size)
                                    showFontSizeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = size.title)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontSizeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Sort Order Dialog
    if (showSortOrderDialog) {
        AlertDialog(
            onDismissRequest = { showSortOrderDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Sort Order", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    SortOrder.entries.forEach { order ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setSortOrder(order)
                                    showSortOrderDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.sortOrder == order,
                                onClick = {
                                    viewModel.setSortOrder(order)
                                    showSortOrderDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = order.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortOrderDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Local Restore Confirmation Dialog
    if (showLocalRestoreConfirmDialog && pendingRestoreUri != null) {
        val uri = pendingRestoreUri!!
        AlertDialog(
            onDismissRequest = {
                showLocalRestoreConfirmDialog = false
                pendingRestoreUri = null
            },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Restore Local Backup?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Would you like to merge the backup items with your current notes, or replace all existing data?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocalRestoreConfirmDialog = false
                        viewModel.importBackup(uri, replaceExisting = false) { result ->
                            scope.launch {
                                result.onSuccess { stats ->
                                    snackbarHostState.showSnackbar("Restored ${stats.noteCount} notes successfully!")
                                }.onFailure { e ->
                                    snackbarHostState.showSnackbar("Restore error: ${e.localizedMessage ?: "Invalid file"}")
                                }
                            }
                        }
                        pendingRestoreUri = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Merge Notes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLocalRestoreConfirmDialog = false
                        viewModel.importBackup(uri, replaceExisting = true) { result ->
                            scope.launch {
                                result.onSuccess { stats ->
                                    snackbarHostState.showSnackbar("All data replaced! Restored ${stats.noteCount} notes.")
                                }.onFailure { e ->
                                    snackbarHostState.showSnackbar("Restore error: ${e.localizedMessage ?: "Invalid file"}")
                                }
                            }
                        }
                        pendingRestoreUri = null
                    }
                ) {
                    Text("Replace All", color = Color(0xFFE53935))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
