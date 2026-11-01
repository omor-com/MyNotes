package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.AppDatabase
import com.example.data.local.entities.ChecklistItemEntity
import com.example.data.local.entities.FolderEntity
import com.example.data.local.entities.NoteEntity
import com.example.data.local.entities.RichSpan
import com.example.data.local.entities.SpanType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MyNotes", appName)
    }

    @Test
    fun `insert and retrieve note with folder and checklist`() = runBlocking {
        val folderId = db.folderDao().insertFolder(
            FolderEntity(name = "Personal", colorHex = "#DDE9FC")
        )

        val noteId = db.noteDao().insertNote(
            NoteEntity(
                title = "Test Note",
                content = "Test Content",
                spansJson = """[{"start":0,"end":4,"type":"BOLD"}]""",
                contentType = NoteEntity.CONTENT_TYPE_CHECKLIST,
                folderId = folderId,
                isPinned = true
            )
        )

        db.checklistDao().insertItem(
            ChecklistItemEntity(
                noteId = noteId,
                text = "Item 1",
                isChecked = false,
                orderIndex = 0
            )
        )

        val notes = db.noteDao().getAllActiveNotes().first()
        assertEquals(1, notes.size)
        val noteWithDetails = notes[0]
        assertEquals("Test Note", noteWithDetails.note.title)
        assertEquals("Personal", noteWithDetails.folder?.name)
        assertEquals(1, noteWithDetails.checklistItems.size)
        assertEquals("Item 1", noteWithDetails.checklistItems[0].text)
    }

    @Test
    fun `rich span serialization and annotated string conversion`() {
        val spans = listOf(
            RichSpan(start = 0, end = 5, type = SpanType.BOLD),
            RichSpan(start = 6, end = 11, type = SpanType.HIGHLIGHT, value = "#FFF176")
        )
        val json = RichSpan.listToJson(spans)
        val parsed = RichSpan.listFromJson(json)

        assertEquals(2, parsed.size)
        assertEquals(SpanType.BOLD, parsed[0].type)
        assertEquals(SpanType.HIGHLIGHT, parsed[1].type)
        assertEquals("#FFF176", parsed[1].value)

        val annotated = RichSpan.toAnnotatedString("Hello World", parsed)
        assertEquals("Hello World", annotated.text)
        assertTrue(annotated.spanStyles.isNotEmpty())
    }
}
