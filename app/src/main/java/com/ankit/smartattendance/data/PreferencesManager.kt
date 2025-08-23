package com.ankit.smartattendance.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create a DataStore instance at the top level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        // Define a key for storing the theme as a String
        val THEME_KEY = stringPreferencesKey("app_theme")
    }

    // A flow to observe changes to the theme preference
    val themeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            // Return the saved theme or "System Default" if none is saved
            preferences[THEME_KEY] ?: "System Default"
        }

    // A suspend function to save the selected theme
    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }
}
