# MyNotes Android App (V1.0)

A polished, fully functional, offline-first native Android note-taking application built with Kotlin, Jetpack Compose, and Material 3 design principles.

## Features

1. **Create, Edit & Delete Notes**
   - Full note creation, rich text body, and checklist support.
   - Deletion moves notes to Trash with immediate Undo Snackbar.
   - Automatic modification timestamp tracking.

2. **Automatic Saving**
   - Debounced background saving while typing (no manual Save button required).
   - Real-time save status indicator (`Saving…` / `Saved`).

3. **Rich Text Formatting**
   - Bold, Italic, Underline, and Strikethrough.
   - Highlight markers with custom pastel highlights.
   - Text color customization.
   - Bullet lists and numbered lists.
   - Structured JSON preservation of spans in Room database.

4. **Folders & Tags**
   - Create, rename, and delete folders with pastel accent colors.
   - Assign and move notes between folders with folder chip indicators.
   - Multi-tag management with quick filtering.

5. **Fast Offline Search**
   - Instant search across note titles and content.
   - Support for English and Bengali script.
   - Instant clear and filter tabs (All, Pinned, Favorites).

6. **Pin & Favorites**
   - Pinned notes dedicated section at the top of the Home feed.
   - Favorite indicator and quick filter.

7. **Light & Dark Themes**
   - Faithful implementation of the MyNotes UI/UX Blueprint.
   - Light theme (`#F8F7F3`), dark charcoal theme (`#141419`), and System Default.
   - Persistent theme selection via DataStore Preferences.

8. **Checklists & To-Do Notes**
   - Dedicated checklist mode with interactive checkboxes.
   - Add, edit, reorder, and delete checklist items.
   - Strikethrough and muted formatting for completed tasks.

9. **100% Offline Storage**
   - Built on Room Database with SQLite.
   - Zero login required; fully private and offline.

10. **Backup & Restore**
    - JSON backup export via Android Storage Access Framework (SAF).
    - Backup restoration with validation and conflict resolution (Merge vs Replace).

## Tech Stack & Architecture

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel) + Repository Pattern
- **Persistence:** Room Database (SQLite) + DataStore Preferences
- **Asynchrony:** Kotlin Coroutines & Reactive `StateFlow`
- **Navigation:** Jetpack Navigation Compose
- **Typography:** Plus Jakarta Sans & Noto Sans Bengali local bundled fonts
