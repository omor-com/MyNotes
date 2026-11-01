package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.local.dao.ChecklistDao
import com.example.data.local.dao.FolderDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.TagDao
import com.example.data.local.entities.ChecklistItemEntity
import com.example.data.local.entities.FolderEntity
import com.example.data.local.entities.NoteEntity
import com.example.data.local.entities.NoteTagCrossRef
import com.example.data.local.entities.TagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class BackupStats(
    val noteCount: Int,
    val folderCount: Int,
    val tagCount: Int,
    val checklistItemCount: Int
)

class BackupManager(
    private val context: Context,
    private val noteDao: NoteDao,
    private val folderDao: FolderDao,
    private val tagDao: TagDao,
    private val checklistDao: ChecklistDao
) {

    suspend fun exportBackup(uri: Uri): Result<BackupStats> = withContext(Dispatchers.IO) {
        runCatching {
            val notes = noteDao.getAllRawNotes()
            val folders = folderDao.getAllRawFolders()
            val tags = tagDao.getAllRawTags()
            val crossRefs = tagDao.getAllRawCrossRefs()
            val checklistItems = checklistDao.getAllRawItems()

            val rootJson = JSONObject().apply {
                put("app", "MyNotes")
                put("version", 1)
                put("exportedAt", System.currentTimeMillis())

                // Folders
                val foldersArray = JSONArray()
                folders.forEach { f ->
                    foldersArray.put(JSONObject().apply {
                        put("id", f.id)
                        put("name", f.name)
                        put("colorHex", f.colorHex)
                        put("createdAt", f.createdAt)
                        put("updatedAt", f.updatedAt)
                    })
                }
                put("folders", foldersArray)

                // Tags
                val tagsArray = JSONArray()
                tags.forEach { t ->
                    tagsArray.put(JSONObject().apply {
                        put("id", t.id)
                        put("name", t.name)
                    })
                }
                put("tags", tagsArray)

                // Notes
                val notesArray = JSONArray()
                notes.forEach { n ->
                    notesArray.put(JSONObject().apply {
                        put("id", n.id)
                        put("title", n.title)
                        put("content", n.content)
                        put("spansJson", n.spansJson)
                        put("contentType", n.contentType)
                        if (n.folderId != null) put("folderId", n.folderId)
                        put("isPinned", n.isPinned)
                        put("isFavorite", n.isFavorite)
                        put("isArchived", n.isArchived)
                        put("isDeleted", n.isDeleted)
                        if (n.colorHex != null) put("colorHex", n.colorHex)
                        put("createdAt", n.createdAt)
                        put("updatedAt", n.updatedAt)
                    })
                }
                put("notes", notesArray)

                // NoteTagCrossRefs
                val crossRefsArray = JSONArray()
                crossRefs.forEach { cr ->
                    crossRefsArray.put(JSONObject().apply {
                        put("noteId", cr.noteId)
                        put("tagId", cr.tagId)
                    })
                }
                put("crossRefs", crossRefsArray)

                // ChecklistItems
                val checklistArray = JSONArray()
                checklistItems.forEach { c ->
                    checklistArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("noteId", c.noteId)
                        put("text", c.text)
                        put("isChecked", c.isChecked)
                        put("orderIndex", c.orderIndex)
                    })
                }
                put("checklistItems", checklistArray)
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Could not open output stream for URI")

            BackupStats(
                noteCount = notes.size,
                folderCount = folders.size,
                tagCount = tags.size,
                checklistItemCount = checklistItems.size
            )
        }
    }

    suspend fun importBackup(uri: Uri, replaceExisting: Boolean = false): Result<BackupStats> = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
            } ?: throw IllegalStateException("Could not read backup file")

            val rootJson = JSONObject(content)
            val app = rootJson.optString("app")
            if (app != "MyNotes" && !rootJson.has("notes")) {
                throw IllegalArgumentException("The selected file is not a valid MyNotes backup.")
            }

            if (replaceExisting) {
                noteDao.clearAllNotes()
                folderDao.clearAllFolders()
                tagDao.clearAllTags()
                tagDao.clearAllCrossRefs()
                checklistDao.clearAllItems()
            }

            // Map old IDs to newly generated IDs to prevent key collisions
            val folderIdMap = mutableMapOf<Long, Long>()
            val tagIdMap = mutableMapOf<Long, Long>()
            val noteIdMap = mutableMapOf<Long, Long>()

            // 1. Import Folders
            val foldersArray = rootJson.optJSONArray("folders") ?: JSONArray()
            var importedFolders = 0
            for (i in 0 until foldersArray.length()) {
                val fObj = foldersArray.getJSONObject(i)
                val oldId = fObj.optLong("id")
                val name = fObj.optString("name", "Folder")
                val colorHex = fObj.optString("colorHex", "#DDE9FC")
                val createdAt = fObj.optLong("createdAt", System.currentTimeMillis())
                val updatedAt = fObj.optLong("updatedAt", System.currentTimeMillis())

                val newId = folderDao.insertFolder(
                    FolderEntity(
                        name = name,
                        colorHex = colorHex,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                )
                folderIdMap[oldId] = newId
                importedFolders++
            }

            // 2. Import Tags
            val tagsArray = rootJson.optJSONArray("tags") ?: JSONArray()
            var importedTags = 0
            for (i in 0 until tagsArray.length()) {
                val tObj = tagsArray.getJSONObject(i)
                val oldId = tObj.optLong("id")
                val name = tObj.optString("name", "Tag")

                val existing = tagDao.getTagByName(name)
                val tagId = existing?.id ?: tagDao.insertTag(TagEntity(name = name))
                tagIdMap[oldId] = tagId
                importedTags++
            }

            // 3. Import Notes
            val notesArray = rootJson.optJSONArray("notes") ?: JSONArray()
            var importedNotes = 0
            for (i in 0 until notesArray.length()) {
                val nObj = notesArray.getJSONObject(i)
                val oldId = nObj.optLong("id")
                val title = nObj.optString("title", "")
                val noteContent = nObj.optString("content", "")
                val spansJson = nObj.optString("spansJson", "")
                val contentType = nObj.optString("contentType", NoteEntity.CONTENT_TYPE_RICH_TEXT)
                val oldFolderId = if (nObj.has("folderId")) nObj.optLong("folderId") else null
                val newFolderId = oldFolderId?.let { folderIdMap[it] }
                val isPinned = nObj.optBoolean("isPinned", false)
                val isFavorite = nObj.optBoolean("isFavorite", false)
                val isArchived = nObj.optBoolean("isArchived", false)
                val isDeleted = nObj.optBoolean("isDeleted", false)
                val colorHex = if (nObj.has("colorHex")) nObj.optString("colorHex") else null
                val createdAt = nObj.optLong("createdAt", System.currentTimeMillis())
                val updatedAt = nObj.optLong("updatedAt", System.currentTimeMillis())

                val newNoteId = noteDao.insertNote(
                    NoteEntity(
                        title = title,
                        content = noteContent,
                        spansJson = spansJson,
                        contentType = contentType,
                        folderId = newFolderId,
                        isPinned = isPinned,
                        isFavorite = isFavorite,
                        isArchived = isArchived,
                        isDeleted = isDeleted,
                        colorHex = colorHex,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                )
                noteIdMap[oldId] = newNoteId
                importedNotes++
            }

            // 4. Import Note-Tag Cross Refs
            val crossRefsArray = rootJson.optJSONArray("crossRefs") ?: JSONArray()
            for (i in 0 until crossRefsArray.length()) {
                val crObj = crossRefsArray.getJSONObject(i)
                val oldNoteId = crObj.optLong("noteId")
                val oldTagId = crObj.optLong("tagId")
                val newNoteId = noteIdMap[oldNoteId]
                val newTagId = tagIdMap[oldTagId]
                if (newNoteId != null && newTagId != null) {
                    tagDao.insertCrossRef(NoteTagCrossRef(noteId = newNoteId, tagId = newTagId))
                }
            }

            // 5. Import Checklist Items
            val checklistArray = rootJson.optJSONArray("checklistItems") ?: JSONArray()
            var importedChecklist = 0
            for (i in 0 until checklistArray.length()) {
                val cObj = checklistArray.getJSONObject(i)
                val oldNoteId = cObj.optLong("noteId")
                val newNoteId = noteIdMap[oldNoteId]
                if (newNoteId != null) {
                    checklistDao.insertItem(
                        ChecklistItemEntity(
                            noteId = newNoteId,
                            text = cObj.optString("text", ""),
                            isChecked = cObj.optBoolean("isChecked", false),
                            orderIndex = cObj.optInt("orderIndex", i)
                        )
                    )
                    importedChecklist++
                }
            }

            BackupStats(
                noteCount = importedNotes,
                folderCount = importedFolders,
                tagCount = importedTags,
                checklistItemCount = importedChecklist
            )
        }
    }
}
