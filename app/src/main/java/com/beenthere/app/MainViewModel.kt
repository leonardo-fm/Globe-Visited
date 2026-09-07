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
import kotlinx.coroutines.flow.asStateFlow
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
     * Volo su una coordinata qualsiasi: la riga di una citta' che non e' ancora
     * sul globo. Non c'e' un [Focus] per le citta' perche' non sono feature del
     * GeoJSON e non c'e' nessun poligono da selezionare.
     */
    data class FocusCoords(val lat: Double, val lng: Double) : GlobeCommand

    /** Volo su un luogo gia' piantato, con la sua card: come il tocco sul pin. */
    data class FocusPlace(val id: String) : GlobeCommand

    /** Stato completo dei luoghi, come [SetAll] per i paesi. */
    data class SetPlaces(val places: List<Place>) : GlobeCommand

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
    val placeCatalog: PlaceCatalog = PlaceCatalog.EMPTY,
    /** I luoghi piantati. Il contatore non li conta: quello resta sui paesi. */
    val places: List<Place> = emptyList()
) {
    val isReady: Boolean get() = catalog.size > 0
    val visitedCount: Int get() = visited.count { catalog[it] != null }
    val total: Int get() = catalog.size
    val placeIds: Set<String> get() = places.mapTo(HashSet()) { it.id }

    fun visitedCountries(): List<Country> = catalog.visitedSorted(visited, language)
}

/**
 * Un luogo in corso di creazione. Le coordinate ci sono gia' quando il dialogo
 * nasce da una pressione lunga sul globo, e mancano quando nasce dalla ricerca
 * a vuoto: e' lo stesso dialogo, cambia solo cosa e' gia' compilato.
 */
data class PlaceDraft(
    val lat: Double? = null,
    val lng: Double? = null,
    val countryCode: String? = null,
    val name: String = ""
)

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

    // Il dialogo "nuovo luogo" sta qui e non nella schermata perche' puo'
    // aprirlo il globo, attraverso il ponte, e la schermata non e' l'unica a
    // saperlo.
    private val _draft = MutableStateFlow<PlaceDraft?>(null)
    val draft: StateFlow<PlaceDraft?> = _draft.asStateFlow()

    val uiState: StateFlow<UiState> = combine(
        repository.visited,
        repository.language,
        catalog,
        placeCatalog,
        repository.places
    ) { visited, language, catalog, cities, places ->
        UiState(
            visited = visited,
            language = language,
            catalog = catalog,
            placeCatalog = cities,
            places = places
        )
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
            _commands.emit(GlobeCommand.SetPlaces(repository.places.first()))
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

    /**
     * Tocco sulla riga di una citta'. Se e' gia' sul globo si apre la sua card,
     * come farebbe il tocco sul pin; altrimenti il globo ci vola sopra e basta.
     */
    fun focusPlace(place: Place) {
        val pinned = place.id in uiState.value.placeIds
        viewModelScope.launch {
            _commands.emit(
                if (pinned) GlobeCommand.FocusPlace(place.id)
                else GlobeCommand.FocusCoords(place.lat, place.lng)
            )
        }
    }

    /** Pallino di una citta' nella ricerca: la mette sul globo o la toglie. */
    fun togglePlace(place: Place) {
        val present = place.id in uiState.value.placeIds
        viewModelScope.launch {
            if (present) repository.removePlace(place.id) else repository.addPlace(place)
            _commands.emit(GlobeCommand.SetPlaces(repository.places.first()))
        }
    }

    /**
     * Pressione lunga sul globo: si apre il dialogo con le coordinate gia'
     * riempite. Il pin non nasce finche' l'utente non conferma un nome.
     */
    fun onPlaceRequested(lat: Double, lng: Double, countryCode: String) {
        _draft.value = PlaceDraft(
            lat = lat,
            lng = lng,
            countryCode = countryCode.takeIf { it.isNotBlank() }
        )
    }

    /** "Crea un luogo" dalla ricerca a vuoto: coordinate da scrivere a mano. */
    fun startPlaceDraft(name: String) {
        _draft.value = PlaceDraft(name = name)
    }

    fun cancelPlaceDraft() {
        _draft.value = null
    }

    /**
     * Conferma del dialogo. Il paese lo passa la schermata: dalla pressione
     * lunga arriva gia' dal JavaScript, da coordinate scritte a mano va chiesto
     * al globo, che e' l'unico a saper fare il point-in-polygon.
     */
    fun createPlace(name: String, lat: Double, lng: Double, countryCode: String?) {
        val place = Place(
            id = "u:" + System.currentTimeMillis().toString(36),
            nameIt = name,
            nameEn = name,
            lat = lat,
            lng = lng,
            countryCode = countryCode,
            custom = true
        )
        _draft.value = null
        viewModelScope.launch {
            repository.addPlace(place)
            _commands.emit(GlobeCommand.SetPlaces(repository.places.first()))
            _commands.emit(GlobeCommand.FocusPlace(place.id))
        }
    }

    /**
     * Rimozione nata sul globo (pulsante *Rimuovi* nella card): il JavaScript ha
     * gia' tolto il pin, qui si persiste e basta. Rimandare indietro lo stato
     * ricostruirebbe la geometria dei punti per niente.
     */
    fun onPlaceRemoved(id: String) {
        viewModelScope.launch { repository.removePlace(id) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setLanguage(language)
            _commands.emit(GlobeCommand.SetLanguage(language))
        }
    }
}
