package com.example.data.local.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class NoteWithDetails(
    @Embedded
    val note: NoteEntity,

    @Relation(
        parentColumn = "folderId",
        entityColumn = "id"
    )
    val folder: FolderEntity? = null,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val checklistItems: List<ChecklistItemEntity> = emptyList()
) {
    val sortedChecklist: List<ChecklistItemEntity>
        get() = checklistItems.sortedBy { it.orderIndex }
}
