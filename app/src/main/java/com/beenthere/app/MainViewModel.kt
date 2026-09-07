package com.beenthere.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beenthere.app.data.AppLanguage
import com.beenthere.app.data.Country
import com.beenthere.app.data.CountryCatalog
import com.beenthere.app.data.Place
import com.beenthere.app.data.PlaceCatalog
import com.beenthere.app.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

/** Comandi diretti al globo. Non c'e' comando per i tocchi nati sul globo stesso. */
sealed interface GlobeCommand {
    data class SetAll(val codes: Set<String>) : GlobeCommand
    data class SetOne(val code: String, val isVisited: Boolean) : GlobeCommand
    data class Focus(val code: String) : GlobeCommand

    /**
     * Volo su una coordinata qualsiasi: la riga di una citta' nella ricerca.
     * Non c'e' un [Focus] per le citta' perche' non sono feature del GeoJSON e
     * il globo non ha niente da selezionare.
     */
    data class FocusCoords(val lat: Double, val lng: Double) : GlobeCommand

    /**
     * La pagina disegna da se' il popup del paese selezionato, quindi anche lei
     * ha bisogno di sapere in che lingua scrivere nome e pulsante.
     */
    data class SetLanguage(val language: AppLanguage) : GlobeCommand
}

data class UiState(
    val visited: Set<String> = emptySet(),
    val language: AppLanguage = AppLanguage.DEFAULT,
    val catalog: CountryCatalog = CountryCatalog.EMPTY,
    val places: PlaceCatalog = PlaceCatalog.EMPTY
) {
    val isReady: Boolean get() = catalog.size > 0
    val visitedCount: Int get() = visited.count { catalog[it] != null }
    val total: Int get() = catalog.size

    fun visitedCountries(): List<Country> = catalog.visitedSorted(visited, language)
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private val catalog = MutableStateFlow(CountryCatalog.EMPTY)

    // Il file delle citta' sta negli asset e lo legge Kotlin, non la WebView:
    // sono ~300 KB che altrimenti attraverserebbero il ponte JS->Kotlin per
    // niente. Dal globo arriva solo l'aggancio ADM0_A3 -> chiave del paese,
    // dentro il catalogo dei paesi.
    private val rawCities = MutableStateFlow<JSONArray?>(null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            rawCities.value = PlaceCatalog.readAsset(application)
        }
    }

    // Le citta' diventano usabili solo quando ci sono anche i paesi: prima
    // resterebbero senza bandiera e senza nome del paese. Fuori dal main
    // thread, sono 4.200 voci da normalizzare per la ricerca.
    //
    // Eagerly, non WhileSubscribed: e' una cache che dipende solo dai due
    // cataloghi e non da chi guarda. Con WhileSubscribed, tornando sull'app
    // dopo cinque secondi in background si rifarebbe tutta la normalizzazione
    // per niente.
    private val placeCatalog: StateFlow<PlaceCatalog> =
        combine(catalog, rawCities) { countries, raw -> PlaceCatalog.build(raw, countries) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, PlaceCatalog.EMPTY)

    // Buffer piccolo ma non zero: i comandi partono anche mentre lo schermo
    // non sta collezionando (per esempio durante una rotazione).
    private val _commands = MutableSharedFlow<GlobeCommand>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val commands = _commands.asSharedFlow()

    val uiState: StateFlow<UiState> = combine(
        repository.visited,
        repository.language,
        catalog,
        placeCatalog
    ) { visited, language, catalog, places ->
        UiState(visited = visited, language = language, catalog = catalog, places = places)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    /**
     * Globo pronto: arriva il catalogo completo e si spinge lo stato salvato.
     * Puo' succedere piu' volte (ricreazione della WebView): e' idempotente.
     */
    fun onGlobeReady(catalogJson: String) {
        val parsed = runCatching { CountryCatalog.parse(catalogJson) }.getOrElse {
            android.util.Log.e("BeenThere", "catalogo non leggibile", it)
            CountryCatalog.EMPTY
        }
        catalog.value = parsed
        viewModelScope.launch {
            // Si legge direttamente dal repository: uiState potrebbe non avere
            // ancora ricevuto la prima emissione di DataStore, e in quel caso si
            // spingerebbe un insieme vuoto (o la lingua di default) sul globo.
            _commands.emit(GlobeCommand.SetLanguage(repository.language.first()))
            _commands.emit(GlobeCommand.SetAll(repository.visited.first()))
        }
    }

    /**
     * Tocco nato sul globo: il JavaScript ha gia' colorato, qui si persiste e
     * basta. Rimandare indietro lo stato causerebbe un ridisegno inutile.
     */
    fun onGlobeToggled(code: String, isVisited: Boolean) {
        viewModelScope.launch { repository.setVisited(code, isVisited) }
    }

    /** Toggle dalla UI nativa: si persiste e si aggiorna il globo. */
    fun toggleVisited(code: String) {
        val next = code !in uiState.value.visited
        viewModelScope.launch {
            repository.setVisited(code, next)
            _commands.emit(GlobeCommand.SetOne(code, next))
        }
    }

    fun focus(code: String) {
        viewModelScope.launch { _commands.emit(GlobeCommand.Focus(code)) }
    }

    /** Tocco sulla riga di una citta': il globo ci vola sopra, e basta. */
    fun focusPlace(place: Place) {
        viewModelScope.launch { _commands.emit(GlobeCommand.FocusCoords(place.lat, place.lng)) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setLanguage(language)
            _commands.emit(GlobeCommand.SetLanguage(language))
        }
    }
}
