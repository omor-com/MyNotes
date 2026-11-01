package com.example.data.repository

import android.content.Intent
import android.net.Uri
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupStats
import com.example.data.backup.GoogleDriveBackupManager
import com.example.data.local.dao.ChecklistDao
import com.example.data.local.dao.FolderDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.TagDao
import com.example.data.local.entities.ChecklistItemEntity
import com.example.data.local.entities.FolderEntity
import com.example.data.local.entities.FolderWithCount
import com.example.data.local.entities.NoteEntity
import com.example.data.local.entities.NoteWithDetails
import com.example.data.local.entities.TagEntity
import com.example.data.preferences.FontSizePreference
import com.example.data.preferences.SortOrder
import com.example.data.preferences.ThemePreference
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.ViewMode
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class NoteRepository(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val tagDao: TagDao,
    private val checklistDao: ChecklistDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val backupManager: BackupManager,
    private val googleDriveBackupManager: GoogleDriveBackupManager
) {

    // Notes
    val activeNotes: Flow<List<NoteWithDetails>> = noteDao.getAllActiveNotes()
    val trashNotes: Flow<List<NoteWithDetails>> = noteDao.getTrashNotes()

    fun getNote(noteId: Long): Flow<NoteWithDetails?> = noteDao.getNoteWithDetails(noteId)

    suspend fun getRawNoteById(noteId: Long): NoteEntity? = noteDao.getNoteById(noteId)

    suspend fun createNote(note: NoteEntity): Long {
        val id = noteDao.insertNote(note)
        triggerAutoDriveBackupIfNeeded()
        return id
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note)
        triggerAutoDriveBackupIfNeeded()
    }

    suspend fun setDeleted(noteId: Long, isDeleted: Boolean) = noteDao.setDeleted(noteId, isDeleted)

    suspend fun setPinned(noteId: Long, isPinned: Boolean) = noteDao.setPinned(noteId, isPinned)

    suspend fun setFavorite(noteId: Long, isFavorite: Boolean) = noteDao.setFavorite(noteId, isFavorite)

    suspend fun setFolder(noteId: Long, folderId: Long?) = noteDao.setFolder(noteId, folderId)

    suspend fun setNoteColor(noteId: Long, colorHex: String?) = noteDao.setNoteColor(noteId, colorHex)

    suspend fun deletePermanently(noteId: Long) = noteDao.deleteNotePermanently(noteId)

    suspend fun emptyTrash() = noteDao.emptyTrash()

    fun searchNotes(query: String): Flow<List<NoteWithDetails>> = noteDao.searchNotes(query)

    fun getNotesInFolder(folderId: Long): Flow<List<NoteWithDetails>> = noteDao.getNotesInFolder(folderId)

    // Folders
    val foldersWithCount: Flow<List<FolderWithCount>> = folderDao.getFoldersWithCount()
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()

    suspend fun getFolderById(id: Long): FolderEntity? = folderDao.getFolderById(id)

    suspend fun createFolder(name: String, colorHex: String): Long =
        folderDao.insertFolder(FolderEntity(name = name, colorHex = colorHex))

    suspend fun updateFolder(folder: FolderEntity) = folderDao.updateFolder(folder)

    suspend fun deleteFolder(folderId: Long) = folderDao.deleteFolder(folderId)

    // Tags
    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()

    suspend fun createTag(name: String): Long {
        val existing = tagDao.getTagByName(name.trim())
        if (existing != null) return existing.id
        return tagDao.insertTag(TagEntity(name = name.trim()))
    }

    suspend fun deleteTag(tagId: Long) = tagDao.deleteTag(tagId)

    suspend fun setTagsForNote(noteId: Long, tagIds: List<Long>) = tagDao.setTagsForNote(noteId, tagIds)

    // Checklist
    fun getChecklistForNote(noteId: Long): Flow<List<ChecklistItemEntity>> = checklistDao.getItemsForNote(noteId)

    suspend fun updateChecklistItem(item: ChecklistItemEntity) = checklistDao.updateItem(item)

    suspend fun insertChecklistItem(item: ChecklistItemEntity) = checklistDao.insertItem(item)

    suspend fun deleteChecklistItem(itemId: Long) = checklistDao.deleteItem(itemId)

    suspend fun replaceChecklistItems(noteId: Long, items: List<ChecklistItemEntity>) =
        checklistDao.replaceItemsForNote(noteId, items)

    // Preferences
    val themePreference: Flow<ThemePreference> = preferencesRepository.themePreference
    val viewMode: Flow<ViewMode> = preferencesRepository.viewMode
    val fontSize: Flow<FontSizePreference> = preferencesRepository.fontSize
    val sortOrder: Flow<SortOrder> = preferencesRepository.sortOrder
    val defaultNoteColor: Flow<String?> = preferencesRepository.defaultNoteColor

    suspend fun setThemePreference(theme: ThemePreference) = preferencesRepository.setThemePreference(theme)
    suspend fun setViewMode(mode: ViewMode) = preferencesRepository.setViewMode(mode)
    suspend fun setFontSize(fontSize: FontSizePreference) = preferencesRepository.setFontSize(fontSize)
    suspend fun setSortOrder(order: SortOrder) = preferencesRepository.setSortOrder(order)
    suspend fun setDefaultNoteColor(colorHex: String?) = preferencesRepository.setDefaultNoteColor(colorHex)

    // Local Backup & Restore
    suspend fun exportBackup(uri: Uri): Result<BackupStats> = backupManager.exportBackup(uri)
    suspend fun importBackup(uri: Uri, replaceExisting: Boolean): Result<BackupStats> =
        backupManager.importBackup(uri, replaceExisting)

    // Google Drive Backup & Restore
    val googleDriveAccount: Flow<String?> = preferencesRepository.googleDriveAccount
    val autoDriveBackupEnabled: Flow<Boolean> = preferencesRepository.autoDriveBackupEnabled
    val lastDriveBackupTimestamp: Flow<Long> = preferencesRepository.lastDriveBackupTimestamp

    fun getGoogleSignInClient(): GoogleSignInClient = googleDriveBackupManager.getGoogleSignInClient()

    fun handleGoogleSignInResult(data: Intent?): Result<GoogleSignInAccount> =
        googleDriveBackupManager.handleSignInResult(data)

    suspend fun setGoogleDriveAccount(email: String?) = preferencesRepository.setGoogleDriveAccount(email)

    suspend fun setAutoDriveBackupEnabled(enabled: Boolean) = preferencesRepository.setAutoDriveBackupEnabled(enabled)

    suspend fun disconnectGoogleDrive(): Result<Unit> {
        val result = googleDriveBackupManager.signOut()
        if (result.isSuccess) {
            preferencesRepository.setGoogleDriveAccount(null)
        }
        return result
    }

    suspend fun backupToGoogleDrive(): Result<String> {
        val json = backupManager.generateBackupJsonString()
        val uploadResult = googleDriveBackupManager.uploadBackup(json)
        if (uploadResult.isSuccess) {
            preferencesRepository.setLastDriveBackupTimestamp(System.currentTimeMillis())
        }
        return uploadResult
    }

    suspend fun restoreFromGoogleDrive(replaceExisting: Boolean): Result<BackupStats> {
        val downloadResult = googleDriveBackupManager.downloadBackup()
        return if (downloadResult.isSuccess) {
            val jsonContent = downloadResult.getOrThrow()
            backupManager.importBackupFromJsonString(jsonContent, replaceExisting)
        } else {
            Result.failure(downloadResult.exceptionOrNull() ?: Exception("Failed to download Drive backup"))
        }
    }

    suspend fun checkDailyDriveBackupNeeded(): Boolean {
        val account = preferencesRepository.googleDriveAccount.first() ?: return false
        val autoBackup = preferencesRepository.autoDriveBackupEnabled.first()
        if (!autoBackup) return false

        val lastBackup = preferencesRepository.lastDriveBackupTimestamp.first()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()

        return (now - lastBackup) > oneDayMillis
    }

    private var lastAutoBackupTime = 0L

    suspend fun triggerAutoDriveBackupIfNeeded() {
        val account = preferencesRepository.googleDriveAccount.first() ?: return
        val autoBackup = preferencesRepository.autoDriveBackupEnabled.first()
        if (!autoBackup) return

        val now = System.currentTimeMillis()
        // Cooldown: at least 2 minutes between auto-backups to prevent API rate limiting while typing
        if (now - lastAutoBackupTime < 2 * 60 * 1000L) return

        lastAutoBackupTime = now
        runCatching {
            backupToGoogleDrive()
        }
    }
}
