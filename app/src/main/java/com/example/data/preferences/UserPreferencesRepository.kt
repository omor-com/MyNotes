package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ViewMode {
    GRID,
    LIST
}

enum class FontSizePreference(val title: String, val scale: Float) {
    SMALL("Small", 0.9f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.15f)
}

enum class SortOrder(val label: String) {
    MODIFIED_DESC("Date Modified (Newest)"),
    MODIFIED_ASC("Date Modified (Oldest)"),
    CREATED_DESC("Date Created (Newest)"),
    TITLE_ASC("Title (A-Z)")
}

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme_preference")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val DEFAULT_NOTE_COLOR = stringPreferencesKey("default_note_color")
    }

    val themePreference: Flow<ThemePreference> = context.dataStore.data.map { preferences ->
        val value = preferences[PreferencesKeys.THEME] ?: ThemePreference.SYSTEM.name
        runCatching { ThemePreference.valueOf(value) }.getOrDefault(ThemePreference.SYSTEM)
    }

    val viewMode: Flow<ViewMode> = context.dataStore.data.map { preferences ->
        val value = preferences[PreferencesKeys.VIEW_MODE] ?: ViewMode.GRID.name
        runCatching { ViewMode.valueOf(value) }.getOrDefault(ViewMode.GRID)
    }

    val fontSize: Flow<FontSizePreference> = context.dataStore.data.map { preferences ->
        val value = preferences[PreferencesKeys.FONT_SIZE] ?: FontSizePreference.MEDIUM.name
        runCatching { FontSizePreference.valueOf(value) }.getOrDefault(FontSizePreference.MEDIUM)
    }

    val sortOrder: Flow<SortOrder> = context.dataStore.data.map { preferences ->
        val value = preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.MODIFIED_DESC.name
        runCatching { SortOrder.valueOf(value) }.getOrDefault(SortOrder.MODIFIED_DESC)
    }

    val defaultNoteColor: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_NOTE_COLOR]
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIEW_MODE] = mode.name
        }
    }

    suspend fun setFontSize(fontSize: FontSizePreference) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE] = fontSize.name
        }
    }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = order.name
        }
    }

    suspend fun setDefaultNoteColor(colorHex: String?) {
        context.dataStore.edit { preferences ->
            if (colorHex == null) {
                preferences.remove(PreferencesKeys.DEFAULT_NOTE_COLOR)
            } else {
                preferences[PreferencesKeys.DEFAULT_NOTE_COLOR] = colorHex
            }
        }
    }
}
