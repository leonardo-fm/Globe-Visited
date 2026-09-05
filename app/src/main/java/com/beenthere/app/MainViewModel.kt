package com.beenthere.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beenthere.app.data.AppLanguage
import com.beenthere.app.data.Country
import com.beenthere.app.data.CountryCatalog
import com.beenthere.app.data.SettingsRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Comandi diretti al globo. Non c'e' comando per i tocchi nati sul globo stesso. */
sealed interface GlobeCommand {
    data class SetAll(val codes: Set<String>) : GlobeCommand
    data class SetOne(val code: String, val isVisited: Boolean) : GlobeCommand
    data class Focus(val code: String) : GlobeCommand

    /**
     * La pagina disegna da se' il popup del paese selezionato, quindi anche lei
     * ha bisogno di sapere in che lingua scrivere nome e pulsante.
     */
    data class SetLanguage(val language: AppLanguage) : GlobeCommand
}

data class UiState(
    val visited: Set<String> = emptySet(),
    val language: AppLanguage = AppLanguage.DEFAULT,
    val catalog: CountryCatalog = CountryCatalog.EMPTY
) {
    val isReady: Boolean get() = catalog.size > 0
    val visitedCount: Int get() = visited.count { catalog[it] != null }
    val total: Int get() = catalog.size

    fun visitedCountries(): List<Country> = catalog.visitedSorted(visited, language)
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private val catalog = MutableStateFlow(CountryCatalog.EMPTY)

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
        catalog
    ) { visited, language, catalog ->
        UiState(visited = visited, language = language, catalog = catalog)
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

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setLanguage(language)
            _commands.emit(GlobeCommand.SetLanguage(language))
        }
    }
}
