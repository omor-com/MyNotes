package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entities.NoteTagCrossRef
import com.example.data.local.entities.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Query("SELECT * FROM tags")
    suspend fun getAllRawTags(): List<TagEntity>

    @Query("SELECT * FROM note_tag_cross_ref")
    suspend fun getAllRawCrossRefs(): List<NoteTagCrossRef>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(ref: NoteTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<NoteTagCrossRef>)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId AND tagId = :tagId")
    suspend fun deleteCrossRef(noteId: Long, tagId: Long)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun clearTagsForNote(noteId: Long)

    @Transaction
    suspend fun setTagsForNote(noteId: Long, tagIds: List<Long>) {
        clearTagsForNote(noteId)
        for (tagId in tagIds) {
            insertCrossRef(NoteTagCrossRef(noteId = noteId, tagId = tagId))
        }
    }

    @Query("DELETE FROM tags")
    suspend fun clearAllTags()

    @Query("DELETE FROM note_tag_cross_ref")
    suspend fun clearAllCrossRefs()
}
