package com.example.data.local.entities

import androidx.room.Embedded

data class FolderWithCount(
    @Embedded
    val folder: FolderEntity,
    val noteCount: Int
)
