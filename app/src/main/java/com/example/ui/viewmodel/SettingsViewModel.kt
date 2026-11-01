package com.example.ui.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupStats
import com.example.data.preferences.FontSizePreference
import com.example.data.preferences.SortOrder
import com.example.data.preferences.ThemePreference
import com.example.data.preferences.ViewMode
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val viewMode: ViewMode = ViewMode.GRID,
    val fontSize: FontSizePreference = FontSizePreference.MEDIUM,
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val defaultNoteColor: String? = null,
    val isOperationInProgress: Boolean = false,
    val statusMessage: String? = null,
    val googleDriveAccount: String? = null,
    val autoDriveBackupEnabled: Boolean = true,
    val lastDriveBackupTimestamp: Long = 0L,
    val isDriveOperationInProgress: Boolean = false
)

private data class SettingsPrefState(
    val theme: ThemePreference,
    val viewMode: ViewMode,
    val fontSize: FontSizePreference,
    val sortOrder: SortOrder,
    val defaultNoteColor: String?
)

private data class DrivePrefState(
    val account: String?,
    val autoBackup: Boolean,
    val lastBackup: Long
)

class SettingsViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _isOperationInProgress = MutableStateFlow(false)
    private val _isDriveOperationInProgress = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<String?>(null)

    private val prefStateFlow = combine(
        repository.themePreference,
        repository.viewMode,
        repository.fontSize,
        repository.sortOrder,
        repository.defaultNoteColor
    ) { theme, viewMode, fontSize, sortOrder, defaultColor ->
        SettingsPrefState(theme, viewMode, fontSize, sortOrder, defaultColor)
    }

    private val drivePrefFlow = combine(
        repository.googleDriveAccount,
        repository.autoDriveBackupEnabled,
        repository.lastDriveBackupTimestamp
    ) { account, autoBackup, lastBackup ->
        DrivePrefState(account, autoBackup, lastBackup)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        prefStateFlow,
        drivePrefFlow,
        _isOperationInProgress,
        _isDriveOperationInProgress,
        _statusMessage
    ) { prefs, drive, inProgress, driveProgress, message ->
        SettingsUiState(
            theme = prefs.theme,
            viewMode = prefs.viewMode,
            fontSize = prefs.fontSize,
            sortOrder = prefs.sortOrder,
            defaultNoteColor = prefs.defaultNoteColor,
            isOperationInProgress = inProgress,
            statusMessage = message,
            googleDriveAccount = drive.account,
            autoDriveBackupEnabled = drive.autoBackup,
            lastDriveBackupTimestamp = drive.lastBackup,
            isDriveOperationInProgress = driveProgress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { repository.setThemePreference(theme) }
    }

    fun setViewMode(viewMode: ViewMode) {
        viewModelScope.launch { repository.setViewMode(viewMode) }
    }

    fun setFontSize(fontSize: FontSizePreference) {
        viewModelScope.launch { repository.setFontSize(fontSize) }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { repository.setSortOrder(order) }
    }

    fun setDefaultNoteColor(colorHex: String?) {
        viewModelScope.launch { repository.setDefaultNoteColor(colorHex) }
    }

    fun exportBackup(uri: Uri, onResult: (Result<BackupStats>) -> Unit) {
        viewModelScope.launch {
            _isOperationInProgress.value = true
            val result = repository.exportBackup(uri)
            _isOperationInProgress.value = false
            onResult(result)
        }
    }

    fun importBackup(uri: Uri, replaceExisting: Boolean, onResult: (Result<BackupStats>) -> Unit) {
        viewModelScope.launch {
            _isOperationInProgress.value = true
            val result = repository.importBackup(uri, replaceExisting)
            _isOperationInProgress.value = false
            onResult(result)
        }
    }

    // Google Drive
    fun getGoogleSignInIntent(): Intent = repository.getGoogleSignInClient().signInIntent

    fun onGoogleSignInResult(data: Intent?, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = repository.handleGoogleSignInResult(data)
            result.onSuccess { account ->
                val email = account.email ?: "Connected Account"
                repository.setGoogleDriveAccount(email)
                onResult(Result.success(email))
            }.onFailure { e ->
                onResult(Result.failure(e))
            }
        }
    }

    fun disconnectGoogleDrive(onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isDriveOperationInProgress.value = true
            val result = repository.disconnectGoogleDrive()
            _isDriveOperationInProgress.value = false
            onResult(result)
        }
    }

    fun toggleAutoDriveBackup(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoDriveBackupEnabled(enabled)
        }
    }

    fun backupToGoogleDrive(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            _isDriveOperationInProgress.value = true
            val result = repository.backupToGoogleDrive()
            _isDriveOperationInProgress.value = false
            onResult(result)
        }
    }

    fun restoreFromGoogleDrive(replaceExisting: Boolean, onResult: (Result<BackupStats>) -> Unit) {
        viewModelScope.launch {
            _isDriveOperationInProgress.value = true
            val result = repository.restoreFromGoogleDrive(replaceExisting)
            _isDriveOperationInProgress.value = false
            onResult(result)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
