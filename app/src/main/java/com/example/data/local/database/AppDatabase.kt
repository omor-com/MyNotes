package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ChecklistDao
import com.example.data.local.dao.FolderDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.TagDao
import com.example.data.local.entities.ChecklistItemEntity
import com.example.data.local.entities.FolderEntity
import com.example.data.local.entities.NoteEntity
import com.example.data.local.entities.NoteTagCrossRef
import com.example.data.local.entities.TagEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        NoteEntity::class,
        FolderEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        ChecklistItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun checklistDao(): ChecklistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mynotes_database.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed initial starter folders & tags
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        val personalFolderId = database.folderDao().insertFolder(
                            FolderEntity(name = "Personal", colorHex = "#DDE9FC")
                        )
                        val workFolderId = database.folderDao().insertFolder(
                            FolderEntity(name = "Work", colorHex = "#FCE7D6")
                        )
                        database.folderDao().insertFolder(
                            FolderEntity(name = "Ideas", colorHex = "#E7E0FA")
                        )

                        val importantTagId = database.tagDao().insertTag(
                            TagEntity(name = "Important")
                        )
                        val todoTagId = database.tagDao().insertTag(
                            TagEntity(name = "To-Do")
                        )

                        // Sample starter note explaining features
                        val welcomeNoteId = database.noteDao().insertNote(
                            NoteEntity(
                                title = "Welcome to MyNotes ✨",
                                content = "Welcome to your new notes app! Everything is stored safely on your device.\n\n• Rich formatting: bold, italic, highlights, lists\n• Checklist notes for to-dos\n• Organize with folders and tags\n• Offline-first and private",
                                spansJson = """[{"start":0,"end":24,"type":"BOLD"},{"start":11,"end":22,"type":"HIGHLIGHT","value":"#E9E7FF"}]""",
                                contentType = NoteEntity.CONTENT_TYPE_RICH_TEXT,
                                folderId = personalFolderId,
                                isPinned = true,
                                isFavorite = true,
                                colorHex = "#E7E0FA"
                            )
                        )
                        database.tagDao().insertCrossRef(
                            NoteTagCrossRef(noteId = welcomeNoteId, tagId = importantTagId)
                        )

                        // Sample starter checklist note
                        val checklistNoteId = database.noteDao().insertNote(
                            NoteEntity(
                                title = "Weekly Goals & Tasks",
                                content = "",
                                spansJson = "",
                                contentType = NoteEntity.CONTENT_TYPE_CHECKLIST,
                                folderId = workFolderId,
                                isPinned = false,
                                isFavorite = false,
                                colorHex = "#DDF3E4"
                            )
                        )
                        database.tagDao().insertCrossRef(
                            NoteTagCrossRef(noteId = checklistNoteId, tagId = todoTagId)
                        )
                        database.checklistDao().insertItems(
                            listOf(
                                ChecklistItemEntity(noteId = checklistNoteId, text = "Organize my notes into folders", isChecked = true, orderIndex = 0),
                                ChecklistItemEntity(noteId = checklistNoteId, text = "Test rich text highlighting", isChecked = false, orderIndex = 1),
                                ChecklistItemEntity(noteId = checklistNoteId, text = "Try exporting a JSON backup", isChecked = false, orderIndex = 2)
                            )
                        )
                    }
                }
            }
        }
    }
}
