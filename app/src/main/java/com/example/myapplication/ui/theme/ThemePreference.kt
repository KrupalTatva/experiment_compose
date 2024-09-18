package com.example.myapplication.ui.theme
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension property for DataStore
private val Context.dataStore by preferencesDataStore(name = "theme_prefs")

object ThemePreferences {

    private lateinit var dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>

    private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")

    // Initialize the DataStore with a context
    fun init(context: Context) {
        dataStore = context.dataStore
    }

    // Flow to observe theme changes
    val isDarkTheme: Flow<Boolean>
        get() = dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[DARK_THEME_KEY] ?: false // Default to light theme
            }

    // Method to save theme preference
    suspend fun saveThemeToPreferencesStore(isDarkTheme: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = isDarkTheme
        }
    }

    suspend fun toggleTheme() {
        // Get the current theme preference
        val currentTheme = dataStore.data
            .map { preferences -> preferences[DARK_THEME_KEY] ?: false }
            .first() // Use .first() to get the current value synchronously

        // Save the toggled theme value
        dataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = !currentTheme
        }
    }
}

