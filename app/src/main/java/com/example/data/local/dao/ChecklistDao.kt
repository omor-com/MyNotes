package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entities.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {

    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY orderIndex ASC, id ASC")
    fun getItemsForNote(noteId: Long): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllRawItems(): List<ChecklistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ChecklistItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ChecklistItemEntity>)

    @Update
    suspend fun updateItem(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun deleteItemsForNote(noteId: Long)

    @Transaction
    suspend fun replaceItemsForNote(noteId: Long, items: List<ChecklistItemEntity>) {
        deleteItemsForNote(noteId)
        val prepared = items.mapIndexed { index, item ->
            item.copy(id = 0, noteId = noteId, orderIndex = index)
        }
        insertItems(prepared)
    }

    @Query("DELETE FROM checklist_items")
    suspend fun clearAllItems()
}
