package com.example.ui.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.ChecklistItemEntity
import com.example.data.local.entities.FolderEntity
import com.example.data.local.entities.NoteEntity
import com.example.data.local.entities.RichSpan
import com.example.data.local.entities.SpanType
import com.example.data.local.entities.TagEntity
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class SaveStatus {
    SAVED,
    SAVING,
    IDLE
}

data class NoteEditorUiState(
    val noteId: Long = 0,
    val title: String = "",
    val bodyFieldValue: TextFieldValue = TextFieldValue(""),
    val spans: List<RichSpan> = emptyList(),
    val contentType: String = NoteEntity.CONTENT_TYPE_RICH_TEXT,
    val folderId: Long? = null,
    val currentFolder: FolderEntity? = null,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val colorHex: String? = null,
    val selectedTags: List<TagEntity> = emptyList(),
    val allFolders: List<FolderEntity> = emptyList(),
    val allTags: List<TagEntity> = emptyList(),
    val checklistItems: List<ChecklistItemEntity> = emptyList(),
    val saveStatus: SaveStatus = SaveStatus.SAVED,
    val isLoaded: Boolean = false
)

@OptIn(FlowPreview::class)
class NoteEditorViewModel(
    private val repository: NoteRepository,
    private val initialNoteId: Long,
    private val initialFolderId: Long? = null,
    private val initialContentType: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private val saveTriggerFlow = MutableStateFlow(0L)
    private var isInitializing = true
    private var autoSaveJob: Job? = null

    init {
        loadData()
        setupAutoSaveDebounce()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Load folders and tags
            val folders = repository.allFolders.first()
            val tags = repository.allTags.first()

            if (initialNoteId > 0) {
                val noteWithDetails = repository.getNote(initialNoteId).first()
                if (noteWithDetails != null) {
                    val note = noteWithDetails.note
                    val loadedSpans = RichSpan.listFromJson(note.spansJson)
                    val checklist = if (note.contentType == NoteEntity.CONTENT_TYPE_CHECKLIST) {
                        repository.getChecklistForNote(note.id).first()
                    } else emptyList()

                    _uiState.value = NoteEditorUiState(
                        noteId = note.id,
                        title = note.title,
                        bodyFieldValue = TextFieldValue(note.content, TextRange(note.content.length)),
                        spans = loadedSpans,
                        contentType = note.contentType,
                        folderId = note.folderId,
                        currentFolder = noteWithDetails.folder,
                        isPinned = note.isPinned,
                        isFavorite = note.isFavorite,
                        colorHex = note.colorHex,
                        selectedTags = noteWithDetails.tags,
                        allFolders = folders,
                        allTags = tags,
                        checklistItems = checklist,
                        saveStatus = SaveStatus.SAVED,
                        isLoaded = true
                    )
                }
            } else {
                // Creating a new note
                val targetFolder = if (initialFolderId != null && initialFolderId > 0) {
                    folders.find { it.id == initialFolderId }
                } else null

                val defaultType = if (initialContentType == NoteEntity.CONTENT_TYPE_CHECKLIST) {
                    NoteEntity.CONTENT_TYPE_CHECKLIST
                } else {
                    NoteEntity.CONTENT_TYPE_RICH_TEXT
                }

                val initialChecklist = if (defaultType == NoteEntity.CONTENT_TYPE_CHECKLIST) {
                    listOf(ChecklistItemEntity(noteId = 0, text = "", isChecked = false, orderIndex = 0))
                } else emptyList()

                _uiState.value = NoteEditorUiState(
                    noteId = 0,
                    title = "",
                    bodyFieldValue = TextFieldValue(""),
                    spans = emptyList(),
                    contentType = defaultType,
                    folderId = targetFolder?.id,
                    currentFolder = targetFolder,
                    isPinned = false,
                    isFavorite = false,
                    colorHex = null,
                    selectedTags = emptyList(),
                    allFolders = folders,
                    allTags = tags,
                    checklistItems = initialChecklist,
                    saveStatus = SaveStatus.SAVED,
                    isLoaded = true
                )
            }
            isInitializing = false
        }
    }

    private fun setupAutoSaveDebounce() {
        viewModelScope.launch {
            saveTriggerFlow
                .debounce(500)
                .distinctUntilChanged()
                .collect { triggerTime ->
                    if (triggerTime > 0 && !isInitializing) {
                        performSave()
                    }
                }
        }
    }

    private fun triggerAutoSave() {
        if (!isInitializing) {
            _uiState.value = _uiState.value.copy(saveStatus = SaveStatus.SAVING)
            saveTriggerFlow.value = System.currentTimeMillis()
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.value = _uiState.value.copy(title = newTitle)
        triggerAutoSave()
    }

    fun onBodyFieldChange(newValue: TextFieldValue) {
        _uiState.value = _uiState.value.copy(bodyFieldValue = newValue)
        triggerAutoSave()
    }

    fun togglePin() {
        val next = !_uiState.value.isPinned
        _uiState.value = _uiState.value.copy(isPinned = next)
        triggerAutoSave()
    }

    fun toggleFavorite() {
        val next = !_uiState.value.isFavorite
        _uiState.value = _uiState.value.copy(isFavorite = next)
        triggerAutoSave()
    }

    fun setNoteColor(hex: String?) {
        _uiState.value = _uiState.value.copy(colorHex = hex)
        triggerAutoSave()
    }

    fun setFolder(folder: FolderEntity?) {
        _uiState.value = _uiState.value.copy(
            folderId = folder?.id,
            currentFolder = folder
        )
        triggerAutoSave()
    }

    fun setSelectedTags(tagIds: List<Long>) {
        val tags = _uiState.value.allTags.filter { tagIds.contains(it.id) }
        _uiState.value = _uiState.value.copy(selectedTags = tags)
        viewModelScope.launch {
            val currentNoteId = _uiState.value.noteId
            if (currentNoteId > 0) {
                repository.setTagsForNote(currentNoteId, tagIds)
            } else {
                performSave()
            }
        }
    }

    fun createTag(name: String) {
        viewModelScope.launch {
            val newId = repository.createTag(name)
            val updatedAllTags = repository.allTags.first()
            val newTag = updatedAllTags.find { it.id == newId }
            val currentSelected = _uiState.value.selectedTags.toMutableList()
            if (newTag != null && !currentSelected.any { it.id == newId }) {
                currentSelected.add(newTag)
            }
            _uiState.value = _uiState.value.copy(
                allTags = updatedAllTags,
                selectedTags = currentSelected
            )
            triggerAutoSave()
        }
    }

    // Rich Text Formatting
    fun toggleSpan(type: SpanType, value: String?) {
        val selection = _uiState.value.bodyFieldValue.selection
        val text = _uiState.value.bodyFieldValue.text
        if (text.isEmpty()) return

        val start = selection.min
        val end = selection.max

        val targetRange = if (start < end) {
            start to end
        } else {
            // If no text is selected, apply to the word around cursor or line
            findWordBounds(text, start)
        }

        if (targetRange.first >= targetRange.second) return

        val currentSpans = _uiState.value.spans.toMutableList()
        val existingIndex = currentSpans.indexOfFirst {
            it.start == targetRange.first && it.end == targetRange.second && it.type == type
        }

        if (existingIndex >= 0) {
            // Remove existing style
            currentSpans.removeAt(existingIndex)
        } else {
            // Add new style
            currentSpans.add(
                RichSpan(
                    start = targetRange.first,
                    end = targetRange.second,
                    type = type,
                    value = value
                )
            )
        }

        _uiState.value = _uiState.value.copy(spans = currentSpans)
        triggerAutoSave()
    }

    private fun findWordBounds(text: String, cursor: Int): Pair<Int, Int> {
        if (text.isEmpty()) return 0 to 0
        val safeCursor = cursor.coerceIn(0, text.length)
        var start = safeCursor
        var end = safeCursor

        while (start > 0 && !text[start - 1].isWhitespace()) {
            start--
        }
        while (end < text.length && !text[end].isWhitespace()) {
            end++
        }
        return start to end
    }

    fun insertBulletList() {
        val current = _uiState.value.bodyFieldValue
        val text = current.text
        val cursor = current.selection.start.coerceIn(0, text.length)

        // Find start of line
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let {
            if (it == -1) 0 else it + 1
        }

        val prefix = "• "
        val updatedText = StringBuilder(text).insert(lineStart, prefix).toString()
        val newCursor = cursor + prefix.length

        _uiState.value = _uiState.value.copy(
            bodyFieldValue = TextFieldValue(updatedText, TextRange(newCursor))
        )
        triggerAutoSave()
    }

    fun insertNumberedList() {
        val current = _uiState.value.bodyFieldValue
        val text = current.text
        val cursor = current.selection.start.coerceIn(0, text.length)

        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let {
            if (it == -1) 0 else it + 1
        }

        val prefix = "1. "
        val updatedText = StringBuilder(text).insert(lineStart, prefix).toString()
        val newCursor = cursor + prefix.length

        _uiState.value = _uiState.value.copy(
            bodyFieldValue = TextFieldValue(updatedText, TextRange(newCursor))
        )
        triggerAutoSave()
    }

    fun convertToTodoList() {
        val currentType = _uiState.value.contentType
        if (currentType == NoteEntity.CONTENT_TYPE_CHECKLIST) {
            // Switch to rich text
            val checklist = _uiState.value.checklistItems
            val text = checklist.joinToString("\n") { it.text }
            _uiState.value = _uiState.value.copy(
                contentType = NoteEntity.CONTENT_TYPE_RICH_TEXT,
                bodyFieldValue = TextFieldValue(text, TextRange(text.length))
            )
        } else {
            // Switch to checklist
            val text = _uiState.value.bodyFieldValue.text
            val lines = text.split("\n").filter { it.isNotBlank() }
            val items = if (lines.isEmpty()) {
                listOf(ChecklistItemEntity(noteId = _uiState.value.noteId, text = "", isChecked = false, orderIndex = 0))
            } else {
                lines.mapIndexed { index, line ->
                    val cleanLine = line.removePrefix("• ").removePrefix("- ").trim()
                    ChecklistItemEntity(
                        noteId = _uiState.value.noteId,
                        text = cleanLine,
                        isChecked = false,
                        orderIndex = index
                    )
                }
            }
            _uiState.value = _uiState.value.copy(
                contentType = NoteEntity.CONTENT_TYPE_CHECKLIST,
                checklistItems = items
            )
        }
        triggerAutoSave()
    }

    // Checklist item operations
    fun onChecklistItemChange(index: Int, item: ChecklistItemEntity) {
        val currentList = _uiState.value.checklistItems.toMutableList()
        if (index in currentList.indices) {
            currentList[index] = item
            _uiState.value = _uiState.value.copy(checklistItems = currentList)
            triggerAutoSave()
        }
    }

    fun onAddChecklistItem(text: String) {
        val currentList = _uiState.value.checklistItems.toMutableList()
        currentList.add(
            ChecklistItemEntity(
                noteId = _uiState.value.noteId,
                text = text,
                isChecked = false,
                orderIndex = currentList.size
            )
        )
        _uiState.value = _uiState.value.copy(checklistItems = currentList)
        triggerAutoSave()
    }

    fun onDeleteChecklistItem(index: Int) {
        val currentList = _uiState.value.checklistItems.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _uiState.value = _uiState.value.copy(checklistItems = currentList)
            triggerAutoSave()
        }
    }

    fun onMoveChecklistItem(fromIndex: Int, toIndex: Int) {
        val currentList = _uiState.value.checklistItems.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _uiState.value = _uiState.value.copy(checklistItems = currentList)
            triggerAutoSave()
        }
    }

    fun performSave(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val state = _uiState.value
            val title = state.title.trim()
            val content = state.bodyFieldValue.text
            val spansJson = RichSpan.listToJson(state.spans)
            val noteId = state.noteId

            // Do not save completely empty new notes unless they have checklist items
            if (noteId == 0L && title.isBlank() && content.isBlank() && state.checklistItems.all { it.text.isBlank() }) {
                _uiState.value = _uiState.value.copy(saveStatus = SaveStatus.SAVED)
                onComplete?.invoke()
                return@launch
            }

            _uiState.value = _uiState.value.copy(saveStatus = SaveStatus.SAVING)

            val noteEntity = NoteEntity(
                id = noteId,
                title = title,
                content = content,
                spansJson = spansJson,
                contentType = state.contentType,
                folderId = state.folderId,
                isPinned = state.isPinned,
                isFavorite = state.isFavorite,
                isArchived = false,
                isDeleted = false,
                colorHex = state.colorHex,
                updatedAt = System.currentTimeMillis()
            )

            val savedNoteId = if (noteId > 0) {
                repository.updateNote(noteEntity)
                noteId
            } else {
                val newId = repository.createNote(noteEntity)
                _uiState.value = _uiState.value.copy(noteId = newId)
                newId
            }

            // Save tags
            repository.setTagsForNote(savedNoteId, state.selectedTags.map { it.id })

            // Save checklist if applicable
            if (state.contentType == NoteEntity.CONTENT_TYPE_CHECKLIST) {
                repository.replaceChecklistItems(savedNoteId, state.checklistItems)
            }

            delay(200) // Brief smooth delay for indicator
            _uiState.value = _uiState.value.copy(saveStatus = SaveStatus.SAVED)
            onComplete?.invoke()
        }
    }

    fun deleteNote(onComplete: () -> Unit) {
        viewModelScope.launch {
            val noteId = _uiState.value.noteId
            if (noteId > 0) {
                repository.setDeleted(noteId, true)
            }
            onComplete()
        }
    }

    fun duplicateNote(onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val newNoteId = repository.createNote(
                NoteEntity(
                    title = if (state.title.isNotBlank()) "${state.title} (Copy)" else "Untitled Copy",
                    content = state.bodyFieldValue.text,
                    spansJson = RichSpan.listToJson(state.spans),
                    contentType = state.contentType,
                    folderId = state.folderId,
                    isPinned = false,
                    isFavorite = false,
                    colorHex = state.colorHex,
                    updatedAt = System.currentTimeMillis()
                )
            )

            repository.setTagsForNote(newNoteId, state.selectedTags.map { it.id })
            if (state.contentType == NoteEntity.CONTENT_TYPE_CHECKLIST) {
                repository.replaceChecklistItems(newNoteId, state.checklistItems)
            }

            onComplete(newNoteId)
        }
    }
}
