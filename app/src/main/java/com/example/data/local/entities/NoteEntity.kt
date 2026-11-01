package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("folderId"),
        Index("isDeleted"),
        Index("isPinned"),
        Index("isFavorite"),
        Index("updatedAt")
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val spansJson: String = "",
    val contentType: String = CONTENT_TYPE_RICH_TEXT, // "RICH_TEXT" or "CHECKLIST"
    val folderId: Long? = null,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val colorHex: String? = null, // Optional pastel note card tint
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val CONTENT_TYPE_RICH_TEXT = "RICH_TEXT"
        const val CONTENT_TYPE_CHECKLIST = "CHECKLIST"
    }
}
