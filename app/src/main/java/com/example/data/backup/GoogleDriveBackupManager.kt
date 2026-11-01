package com.example.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient

/**
 * Facade preserving existing API surface and delegating to GoogleDriveManager.
 */
class GoogleDriveBackupManager(
    private val context: Context
) {
    private val driveManager = GoogleDriveManager(context)

    companion object {
        const val CLIENT_ID = "422664646881-paqocm1m8kd753a75vlm2dtt19rcr42a.apps.googleusercontent.com"
        const val BACKUP_FILE_NAME = GoogleDriveManager.BACKUP_FILE_NAME
    }

    fun getGoogleSignInClient(): GoogleSignInClient = driveManager.getGoogleSignInClient()

    fun getSignedInAccount(): GoogleSignInAccount? = driveManager.getSignedInAccount()

    fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount> = driveManager.handleSignInResult(data)

    suspend fun signOut(): Result<Unit> = driveManager.signOut()

    suspend fun uploadBackup(jsonContent: String): Result<String> = driveManager.uploadBackup(jsonContent)

    suspend fun downloadBackup(): Result<String> = driveManager.downloadBackup()

    suspend fun checkBackupFile(): Result<DriveFileMetadata?> = driveManager.checkBackupFile()
}
