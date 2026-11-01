# MyNotes — UI/UX Design Blueprint
**Native Android App · Kotlin · Jetpack Compose · Material 3 · Version 1.0**

## 1. Product Vision
MyNotes is a premium minimalist note-taking app designed to feel clean, elegant, fast, and distraction-free. It should be comfortable for everyday writing and work fully offline.

## 2. Technology Stack
- **Language:** Kotlin
- **IDE:** Android Studio
- **UI:** Jetpack Compose
- **Design system:** Material 3
- **Local database:** Room
- **Settings storage:** DataStore
- **Architecture:** MVVM
- **Minimum touch target:** 48 × 48 dp

## 3. Color Palette

### Light Theme (Default)
| Role | Hex |
|---|---|
| Background | `#F8F7F3` |
| Surface / Cards | `#FFFFFF` |
| Primary | `#5B61D6` |
| Main Text | `#25252B` |
| Secondary Text | `#85858D` |
| Accent Surface | `#E9E7FF` |
| Divider / Outline | `#E7E5E0` |
| Search Field | `#EDECE7` |

### Dark Theme
| Role | Hex |
|---|---|
| Background | `#141419` |
| Surface / Cards | `#202024` |
| Primary | `#A5A0FF` |
| Main Text | `#F2F0F7` |
| Secondary Text | `#A5A3AD` |
| Accent Surface | `#302E45` |
| Divider / Outline | `#37363D` |

**Dark mode note:** Use charcoal surfaces rather than pure black, for a softer and more comfortable reading experience.

## 4. Typography
| Use | Font | Size | Weight |
|---|---|---:|---|
| App name | Google Sans (or fallback) | 22sp | Bold |
| Screen heading | Google Sans | 24sp | Bold |
| Note title | Google Sans | 18sp | SemiBold |
| Note body | Roboto | 16sp | Regular |
| Button | Roboto | 14sp | Medium |
| Metadata / date | Roboto | 12sp | Regular |

Support Bengali and English text with appropriate font fallback.

## 5. Home Screen

### Layout
1. **Top bar:** MyNotes wordmark and Settings icon
2. **Greeting:** “Your thoughts, organized.”
3. **Search field:** Rounded field with search icon and hint “Search your notes…”
4. **Pinned section:** Pinned notes displayed first
5. **Recent notes:** Clean note cards
6. **Floating Action Button (FAB):** Plus icon to create a note
7. **Bottom navigation:** Notes · Folders · Settings

### Note Card
- Background: `#FFFFFF` in Light Theme; dark surface in Dark Theme
- Corner radius: **16dp**
- Shadow: none or very subtle
- Title: **16sp SemiBold**
- Preview: **14sp Regular**
- Date: **12sp**, secondary text color
- Pin icon: small outline/filled icon
- Optional individual soft pastel note colors

## 6. Note Editor

### Toolbar
- Back button
- Small save status: “Saved” / saving indicator
- Pin toggle
- More options menu (three-dot icon)

### Content
- Title field: **24sp Bold**
- Body field: **16sp Regular**
- Formatting toolbar: above keyboard or at bottom
- Auto-save while typing; no manual Save button required

### Rich Text Formatting
- Bold, italic, underline, strikethrough
- Text color and highlight
- Bullet list and numbered list
- Checklist insertion

## 7. Folders and Tags

### Folder design
- Folder name
- Note count
- Custom pastel color
- Create, rename, and delete folder
- Move notes between folders

### Pastel folder colors
| Name | Hex |
|---|---|
| Peach | `#FCE7D6` |
| Soft Blue | `#DDE9FC` |
| Lavender | `#E7E0FA` |
| Mint | `#DDF3E4` |
| Pink | `#F8E1EA` |
| Butter | `#F4EDCF` |

Tags should appear as small rounded pill-shaped chips. Users can add, remove, and filter by tags.

## 8. Checklist / To-do Notes
- Rounded-square checkbox
- Checked color: Primary
- Completed item: muted text with strikethrough
- Add, edit, delete, and reorder checklist items
- Save checklist changes automatically

## 9. Search and Organization
- Search by title and note content
- Instant search results
- Search/filter by folder and tag
- Pin and favorite notes
- Grid and list view
- Sort by modified date, created date, or title
- Archive and Trash sections

## 10. Settings Screen
- Appearance: Light / Dark / System Default
- Default note color
- Default view: Grid / List
- Font size
- Sort order
- Backup and Restore
- About MyNotes
- App version

## 11. Buttons and Components

### Primary button
- Background: `#5B61D6` (Light Theme)
- Text: white, medium weight
- Height: **52dp**
- Corner radius: **14dp**

### Secondary button
- Transparent or surface background
- Primary-colored text
- Optional subtle outline

### Search field
- Background: `#EDECE7`
- Corner radius: **14dp**
- Leading search icon
- Comfortable horizontal padding

### Component dimensions
| Component | Specification |
|---|---|
| Note card radius | 16dp |
| Dialog radius | 24dp |
| Button radius | 14dp |
| Input radius | 14dp |
| FAB | 56dp |
| Minimum touch target | 48dp |

Use rounded outline icons (Material Symbols / Material icons). Keep icon sizes and stroke weights consistent.

## 12. Motion and Interaction
| Interaction | Animation |
|---|---|
| Open note | Subtle fade + slide |
| Create note | Gentle scale / fade |
| Delete note | Swipe + fade out |
| Pin note | Small icon animation |
| Theme change | Smooth crossfade |
| Screen transition | 200–300 ms |
| Button press | Material ripple |

Animations should be restrained and performant, including on low-end Android devices.

## 13. Accessibility and Responsiveness
- Edge-to-edge Android UI
- Support system font scaling
- Adequate text/background contrast
- Descriptive content labels for icons
- Responsive layouts for different screen sizes
- Respect system animation settings
- Avoid relying on color alone to communicate state

## 14. Version 1.0 Feature Scope
The first release includes these ten core features:

1. Create, edit, and delete notes
2. Automatic saving
3. Rich text editor
4. Folders and tags
5. Search
6. Pin and favorites
7. Light and dark themes
8. Checklist / to-do notes
9. Offline storage
10. Backup and restore

## 15. Final Design Principles
- Minimal, premium, and distraction-free
- Material 3 with a consistent design language
- 8dp spacing system
- Consistent rounded corners
- No unnecessary ads or clutter
- Offline-first behavior
- Fast launch and smooth scrolling
- Both themes must be carefully designed
- Prioritize readability and ease of use

---

**This document is the primary UI/UX design guideline for MyNotes Version 1.0.** All screens should follow these design tokens and principles when implemented in Kotlin and Jetpack Compose.
