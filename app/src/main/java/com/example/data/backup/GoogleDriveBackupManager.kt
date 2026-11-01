package com.example.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class DriveFileMetadata(
    val id: String,
    val name: String,
    val modifiedTime: String? = null,
    val size: Long = 0L
)

class GoogleDriveBackupManager(
    private val context: Context
) {

    companion object {
        const val CLIENT_ID = "422664646881-paqocm1m8kd753a75vlm2dtt19rcr42a.apps.googleusercontent.com"
        const val BACKUP_FILE_NAME = "MyNotes_Backup.json"
        private const val DRIVE_SCOPE_FILE = "https://www.googleapis.com/auth/drive.file"
        private const val DRIVE_SCOPE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        private const val OAUTH_SCOPE_STRING = "oauth2:$DRIVE_SCOPE_FILE $DRIVE_SCOPE_APPDATA"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(CLIENT_ID)
            .requestScopes(Scope(DRIVE_SCOPE_FILE), Scope(DRIVE_SCOPE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return runCatching {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            task.getResult(ApiException::class.java) ?: throw IllegalStateException("Google Sign-In returned null account")
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val client = getGoogleSignInClient()
            client.signOut()
            Unit
        }
    }

    private suspend fun getAccessToken(account: GoogleSignInAccount): String = withContext(Dispatchers.IO) {
        val androidAccount = account.account ?: throw IllegalStateException("Account has no Android account associated")
        GoogleAuthUtil.getToken(context, androidAccount, OAUTH_SCOPE_STRING)
    }

    suspend fun findBackupFile(): Result<DriveFileMetadata?> = withContext(Dispatchers.IO) {
        runCatching {
            val account = getSignedInAccount() ?: throw IllegalStateException("Google Drive not connected. Please connect your Google account.")
            val token = getAccessToken(account)

            val query = "name='$BACKUP_FILE_NAME' and trashed=false"
            val url = "https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,modifiedTime,size)&spaces=drive"

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IOException("Drive API error (${response.code}): $responseBody")
            }

            val json = JSONObject(responseBody)
            val files = json.optJSONArray("files")
            if (files != null && files.length() > 0) {
                val fileObj = files.getJSONObject(0)
                DriveFileMetadata(
                    id = fileObj.getString("id"),
                    name = fileObj.getString("name"),
                    modifiedTime = fileObj.optString("modifiedTime"),
                    size = fileObj.optLong("size", 0L)
                )
            } else {
                null
            }
        }
    }

    suspend fun uploadBackup(jsonContent: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val account = getSignedInAccount() ?: throw IllegalStateException("Google Drive not connected. Please connect your Google account.")
            val token = getAccessToken(account)

            // Check if file already exists
            val existingFile = findBackupFile().getOrNull()

            if (existingFile != null) {
                // Update existing file content
                val updateUrl = "https://www.googleapis.com/upload/drive/v3/files/${existingFile.id}?uploadType=media"
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonContent.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(updateUrl)
                    .header("Authorization", "Bearer $token")
                    .patch(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("Failed to update Drive backup (${response.code}): $body")
                }
                existingFile.id
            } else {
                // Create new file with multipart upload
                val createUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

                val metadataJson = JSONObject().apply {
                    put("name", BACKUP_FILE_NAME)
                    put("mimeType", "application/json")
                }.toString()

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addPart(
                        metadataJson.toRequestBody("application/json; charset=utf-8".toMediaType())
                    )
                    .addPart(
                        jsonContent.toRequestBody("application/json; charset=utf-8".toMediaType())
                    )
                    .build()

                val request = Request.Builder()
                    .url(createUrl)
                    .header("Authorization", "Bearer $token")
                    .post(multipartBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("Failed to upload Drive backup (${response.code}): $body")
                }

                val resultJson = JSONObject(body)
                resultJson.getString("id")
            }
        }
    }

    suspend fun downloadBackup(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val account = getSignedInAccount() ?: throw IllegalStateException("Google Drive not connected. Please connect your Google account.")
            val token = getAccessToken(account)

            val existingFile = findBackupFile().getOrNull()
                ?: throw IllegalStateException("No MyNotes backup found on your Google Drive.")

            val downloadUrl = "https://www.googleapis.com/drive/v3/files/${existingFile.id}?alt=media"

            val request = Request.Builder()
                .url(downloadUrl)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val content = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Failed to download Drive backup (${response.code}): $content")
            }

            content
        }
    }
}
