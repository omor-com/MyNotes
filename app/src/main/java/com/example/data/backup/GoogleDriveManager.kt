package com.example.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

data class DriveFileMetadata(
    val id: String,
    val name: String,
    val modifiedTime: String? = null,
    val size: Long = 0L
)

/**
 * Service class handling Google Drive API v3 authentication, file uploads,
 * and restore operations for MyNotes cloud backup.
 */
class GoogleDriveManager(
    private val context: Context
) {

    companion object {
        const val BACKUP_FILE_NAME = "MyNotes_Backup.json"
        val SCOPES: List<String> = listOf(
            DriveScopes.DRIVE_FILE,
            DriveScopes.DRIVE_APPDATA
        )
    }

    /**
     * Builds and returns the GoogleSignInClient configured for Google Drive access.
     * Note: We request Drive scopes and email without forcing requestIdToken,
     * which prevents CommonStatusCodes.DEVELOPER_ERROR (code 10).
     */
    fun getGoogleSignInClient(): GoogleSignInClient {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()

        // Add Drive Scopes
        SCOPES.forEach { scopeUri ->
            gsoBuilder.requestScopes(Scope(scopeUri))
        }

        return GoogleSignIn.getClient(context, gsoBuilder.build())
    }

    /**
     * Gets currently signed-in Google account if available and permissions are intact.
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val hasScopes = GoogleSignIn.hasPermissions(
            account,
            *SCOPES.map { Scope(it) }.toTypedArray()
        )
        return if (hasScopes) account else account
    }

    /**
     * Handles the Intent returned by Google Sign-In activity result.
     * Maps error codes like 10 (DEVELOPER_ERROR) to clear, actionable guidance.
     */
    fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return runCatching {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                task.getResult(ApiException::class.java)
                    ?: throw IllegalStateException("Google Sign-In returned empty account")
            } catch (apiEx: ApiException) {
                val message = when (apiEx.statusCode) {
                    CommonStatusCodes.DEVELOPER_ERROR -> {
                        "Developer Error (10): Package name 'com.mynotesadfree.app' and SHA-1 fingerprint (51:2C:25:0D:...) must match your Google Cloud Console OAuth Client."
                    }
                    CommonStatusCodes.SIGN_IN_REQUIRED -> "Sign-in was cancelled or required."
                    CommonStatusCodes.NETWORK_ERROR -> "Network error during Google Sign-In. Check your internet connection."
                    CommonStatusCodes.INTERNAL_ERROR -> "Google Play Services internal error (${apiEx.statusCode}). Please update Google Play Services."
                    else -> "Google Sign-In error (${apiEx.statusCode}): ${apiEx.localizedMessage ?: "Unknown error"}"
                }
                throw Exception(message, apiEx)
            }
        }
    }

    /**
     * Signs out the user from Google Sign-In.
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val client = getGoogleSignInClient()
            client.signOut()
            client.revokeAccess()
            Unit
        }
    }

    /**
     * Creates an authorized Drive service client using the user's account credential.
     */
    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            SCOPES
        ).apply {
            selectedAccount = account.account
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("MyNotes").build()
    }

    /**
     * Uploads or updates the notes backup file in Google Drive.
     * If an existing backup file exists, it updates it in-place. Otherwise, it creates a new one.
     * Returns the Drive File ID.
     */
    suspend fun uploadBackup(jsonContent: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val account = getSignedInAccount()
                ?: throw IllegalStateException("No Google account signed in. Please connect Google Drive first.")

            val driveService = getDriveService(account)

            // Search for existing backup file
            val query = "name = '$BACKUP_FILE_NAME' and trashed = false"
            val fileList: FileList = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, modifiedTime, size)")
                .execute()

            val existingFile = fileList.files?.firstOrNull()
            val mediaContent = ByteArrayContent(
                "application/json",
                jsonContent.toByteArray(Charsets.UTF_8)
            )

            if (existingFile != null) {
                // Update existing backup file
                val fileMetadata = File()
                val updatedFile = driveService.files()
                    .update(existingFile.id, fileMetadata, mediaContent)
                    .setFields("id, name, modifiedTime")
                    .execute()
                updatedFile.id ?: existingFile.id
            } else {
                // Create new backup file
                val fileMetadata = File().apply {
                    name = BACKUP_FILE_NAME
                    mimeType = "application/json"
                    description = "MyNotes Android Application Backup"
                }
                val createdFile = driveService.files()
                    .create(fileMetadata, mediaContent)
                    .setFields("id, name, modifiedTime")
                    .execute()
                createdFile.id ?: throw IOException("Google Drive returned empty file ID after creation")
            }
        }
    }

    /**
     * Downloads the latest backup file content from Google Drive as a JSON string.
     */
    suspend fun downloadBackup(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val account = getSignedInAccount()
                ?: throw IllegalStateException("No Google account signed in. Please connect Google Drive first.")

            val driveService = getDriveService(account)

            val query = "name = '$BACKUP_FILE_NAME' and trashed = false"
            val fileList = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, modifiedTime, size)")
                .execute()

            val existingFile = fileList.files?.firstOrNull()
                ?: throw IOException("No backup file named '$BACKUP_FILE_NAME' found in your Google Drive.")

            val outputStream = ByteArrayOutputStream()
            driveService.files().get(existingFile.id).executeMediaAndDownloadTo(outputStream)
            outputStream.toString(Charsets.UTF_8.name())
        }
    }

    /**
     * Checks if a backup file exists in Google Drive and returns metadata.
     */
    suspend fun checkBackupFile(): Result<DriveFileMetadata?> = withContext(Dispatchers.IO) {
        runCatching {
            val account = getSignedInAccount() ?: return@runCatching null
            val driveService = getDriveService(account)

            val query = "name = '$BACKUP_FILE_NAME' and trashed = false"
            val fileList = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, modifiedTime, size)")
                .execute()

            val file = fileList.files?.firstOrNull() ?: return@runCatching null
            DriveFileMetadata(
                id = file.id,
                name = file.name,
                modifiedTime = file.modifiedTime?.toStringRfc3339(),
                size = (file.size as? Number)?.toLong() ?: 0L
            )
        }
    }
}
