package com.beenthere.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beenthere.app.GlobeCommand
import com.beenthere.app.MainViewModel
import com.beenthere.app.globe.GlobeBridge
import com.beenthere.app.globe.GlobeController
import com.beenthere.app.globe.GlobeWebView
import com.beenthere.app.data.PlaceCatalog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeenThereScreen(viewModel: MainViewModel = viewModel()) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val controller = remember { GlobeController() }
    val bridge = remember {
        GlobeBridge(
            onReady = viewModel::onGlobeReady,
            onToggled = viewModel::onGlobeToggled,
            onRemovePlace = viewModel::onPlaceRemoved,
            onRequestPlace = viewModel::onPlaceRequested
        )
    }

    // Comandi verso il globo. I tocchi nati sul globo non passano di qui: e' gia'
    // colorato, ripetere l'ordine sarebbe solo un ridisegno in piu'.
    LaunchedEffect(controller) {
        viewModel.commands.collect { command ->
            when (command) {
                is GlobeCommand.SetAll -> controller.setVisited(command.codes)
                is GlobeCommand.SetOne -> controller.setCountryVisited(command.code, command.isVisited)
                is GlobeCommand.Focus -> controller.focusCountry(command.code)
                is GlobeCommand.FocusCoords -> controller.flyToCoords(command.lat, command.lng)
                is GlobeCommand.FocusPlace -> controller.focusPlace(command.id)
                is GlobeCommand.SetPlaces -> controller.setPlaces(command.places)
                is GlobeCommand.SetLanguage -> controller.setLanguage(command.language.tag)
            }
        }
    }

    var query by remember { mutableStateOf("") }
    var sheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    // Memoizzata sulle sole chiavi che contano: un toggle sul globo non
    // rifa' la ricerca.
    val results = remember(query, state.catalog, state.language) {
        state.catalog.search(query, state.language)
    }
    // I pin creati a mano non stanno in cities.json: si cercano fra i luoghi
    // dell'utente e vanno per primi. Senza, un pin piantato per sbaglio in
    // mezzo al Pacifico non si ritroverebbe piu'.
    val placeResults = remember(query, state.placeCatalog, state.language, state.places) {
        val mine = PlaceCatalog.searchAmong(
            state.places.filter { it.custom }, query, state.language
        )
        (mine + state.placeCatalog.search(query, state.language)).distinctBy { it.id }
    }
    val pinnedPlaces = remember(state.places) { state.placeIds }
    val draft by viewModel.draft.collectAsStateWithLifecycle()

    ProvideAppLanguage(state.language) {
        Box(Modifier.fillMaxSize()) {

            GlobeWebView(
                controller = controller,
                bridge = bridge,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    // Solo bordi e barra di stato: includere l'IME farebbe
                    // saltare il pannello quando si apre la tastiera.
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Top
                        )
                    )
                    .padding(12.dp)
                    .widthIn(max = 520.dp)
            ) {
                CounterChip(
                    visitedCount = state.visitedCount,
                    total = state.total,
                    isReady = state.isReady,
                    onClick = { sheetOpen = true }
                )

                SearchPanel(
                    query = query,
                    onQueryChange = { query = it },
                    results = results,
                    placeResults = placeResults,
                    catalog = state.catalog,
                    visited = state.visited,
                    pinnedPlaces = pinnedPlaces,
                    language = state.language,
                    isReady = state.isReady,
                    onSelect = { country ->
                        viewModel.focus(country.code)
                        query = ""
                    },
                    onSelectPlace = { place ->
                        viewModel.focusPlace(place)
                        query = ""
                    },
                    onToggle = { country -> viewModel.toggleVisited(country.code) },
                    onTogglePlace = { place -> viewModel.togglePlace(place) },
                    onCreatePlace = { viewModel.startPlaceDraft(query.trim()) }
                )
            }

            draft?.let { current ->
                AddPlaceDialog(
                    draft = current,
                    onDismiss = viewModel::cancelPlaceDraft,
                    onConfirm = { name, lat, lng ->
                        if (current.lat == lat && current.lng == lng) {
                            // Coordinate non toccate: il paese lo sa gia' il
                            // JavaScript, che le ha calcolate sotto il dito.
                            viewModel.createPlace(name, lat, lng, current.countryCode)
                        } else {
                            // Scritte o corrette a mano: il point-in-polygon vive
                            // nel globo, quindi glielo si chiede.
                            controller.resolveCountry(lat, lng) { code ->
                                viewModel.createPlace(name, lat, lng, code)
                            }
                        }
                        query = ""
                    }
                )
            }

            if (sheetOpen) {
                VisitedSheet(
                    countries = state.visitedCountries(),
                    total = state.total,
                    language = state.language,
                    sheetState = sheetState,
                    onDismiss = { sheetOpen = false },
                    onSelect = { country ->
                        viewModel.focus(country.code)
                        scope.launch {
                            sheetState.hide()
                            sheetOpen = false
                        }
                    },
                    onRemove = { country -> viewModel.toggleVisited(country.code) },
                    onLanguageChange = viewModel::setLanguage
                )
            }
        }
    }
}
