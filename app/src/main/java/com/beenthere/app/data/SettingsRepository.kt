package com.beenthere.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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

    // Preferenze di aspetto. Stanno insieme ai dati ma non sono dati: "azzera
    // tutto" cancella paesi e luoghi e queste NON le tocca, perche' nessuno
    // che vuole ricominciare da capo intende anche tornare all'arancione.
    private val pinsVisibleKey = booleanPreferencesKey("pins_visible")
    private val visitedColorKey = stringPreferencesKey("visited_color")

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

    /** Se i pin dei luoghi si disegnano sul globo. Non li cancella: li nasconde. */
    val pinsVisible: Flow<Boolean> = preferences.map { it[pinsVisibleKey] ?: true }

    /**
     * Colore dei paesi visitati. Si salva il testo esadecimale e non l'indice
     * di una tavolozza: se un domani i colori offerti cambiano di ordine, la
     * scelta gia' fatta non diventa un altro colore.
     */
    val visitedColor: Flow<String> =
        preferences.map { VisitedColors.sanitize(it[visitedColorKey]) }

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

    /**
     * Sostituisce integralmente paesi e luoghi: e' il ripristino da un backup.
     * Una `edit` sola, cosi' non esiste un istante in cui i paesi sono quelli
     * nuovi e i luoghi ancora quelli vecchi.
     */
    suspend fun replaceAll(visited: Set<String>, places: List<Place>) {
        context.dataStore.edit { prefs ->
            prefs[visitedKey] = visited
            prefs[placesKey] = Place.listToJson(places)
        }
    }

    suspend fun setPinsVisible(visible: Boolean) {
        context.dataStore.edit { prefs -> prefs[pinsVisibleKey] = visible }
    }

    suspend fun setVisitedColor(hex: String) {
        context.dataStore.edit { prefs -> prefs[visitedColorKey] = VisitedColors.sanitize(hex) }
    }

    /**
     * Cancella paesi e luoghi, e lascia in piedi lingua, colore e pin: e'
     * "ricomincio da capo", non "reinstallo l'app". Una `edit` sola, come
     * [replaceAll].
     */
    suspend fun clearData() {
        context.dataStore.edit { prefs ->
            prefs.remove(visitedKey)
            prefs.remove(placesKey)
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
