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

    // Chiave nuova, non una migrazione di visited_countries: i luoghi sono una
    // cosa a se' - segnare New York non segna gli Stati Uniti - e tenerli
    // separati vuol dire che l'aggiunta dei luoghi non puo' rovinare i paesi
    // gia' salvati.
    private val placesKey = stringPreferencesKey("places")

    private val preferences: Flow<Preferences> = context.dataStore.data
        .catch { error ->
            // Un file corrotto o illeggibile non deve impedire l'avvio dell'app:
            // si riparte da preferenze vuote.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    val visited: Flow<Set<String>> = preferences.map { it[visitedKey].orEmpty() }

    val language: Flow<AppLanguage> = preferences.map { AppLanguage.fromTag(it[languageKey]) }

    /** I luoghi piantati dall'utente: citta' del catalogo e, dalla fase 3, pin a mano. */
    val places: Flow<List<Place>> = preferences.map { Place.listFromJson(it[placesKey]) }

    suspend fun setVisited(code: String, isVisited: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[visitedKey].orEmpty()
            prefs[visitedKey] = if (isVisited) current + code else current - code
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs -> prefs[languageKey] = language.tag }
    }

    /** Aggiunge un luogo, o lo sostituisce se un luogo con quell'id c'e' gia'. */
    suspend fun addPlace(place: Place) {
        context.dataStore.edit { prefs ->
            val current = Place.listFromJson(prefs[placesKey]).filter { it.id != place.id }
            prefs[placesKey] = Place.listToJson(current + place)
        }
    }

    suspend fun removePlace(id: String) {
        context.dataStore.edit { prefs ->
            val current = Place.listFromJson(prefs[placesKey])
            if (current.none { it.id == id }) return@edit
            prefs[placesKey] = Place.listToJson(current.filter { it.id != id })
        }
    }
}
