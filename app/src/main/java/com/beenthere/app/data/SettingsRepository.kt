package com.beenthere.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "been_there")

/**
 * Unica sorgente di verita' dei paesi visitati. La WebView non persiste nulla:
 * il suo localStorage resta spento (vedi index.html, DEMO_PERSISTENCE).
 */
class SettingsRepository(private val context: Context) {

    private val visitedKey = stringSetPreferencesKey("visited_countries")
    private val languageKey = stringPreferencesKey("language")

    private val preferences: Flow<Preferences> = context.dataStore.data
        .catch { error ->
            // Un file corrotto o illeggibile non deve impedire l'avvio dell'app:
            // si riparte da preferenze vuote.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    val visited: Flow<Set<String>> = preferences.map { it[visitedKey].orEmpty() }

    val language: Flow<AppLanguage> = preferences.map { AppLanguage.fromTag(it[languageKey]) }

    suspend fun setVisited(code: String, isVisited: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[visitedKey].orEmpty()
            prefs[visitedKey] = if (isVisited) current + code else current - code
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs -> prefs[languageKey] = language.tag }
    }
}
