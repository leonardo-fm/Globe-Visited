package com.beenthere.app.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beenthere.app.BackupNotice
import com.beenthere.app.GlobeCommand
import com.beenthere.app.MainViewModel
import com.beenthere.app.R
import com.beenthere.app.BuildConfig
import com.beenthere.app.data.Backup
import com.beenthere.app.globe.GlobeBridge
import com.beenthere.app.globe.GlobeController
import com.beenthere.app.globe.GlobeWebView
import com.beenthere.app.data.PlaceCatalog
import com.beenthere.app.ui.theme.OnPanel
import com.beenthere.app.ui.theme.OnPanelMuted
import com.beenthere.app.ui.theme.PanelSolid
import com.beenthere.app.ui.theme.LocalVisitedColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun BeenThereScreen(viewModel: MainViewModel = viewModel()) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val controller = remember { GlobeController() }
    val bridge = remember {
        GlobeBridge(
            onReady = viewModel::onGlobeReady,
            onToggled = viewModel::onGlobeToggled,
            onRemovePlace = viewModel::onPlaceRemoved,
            onRequestPlace = viewModel::onPlaceRequested,
            onAddPlace = viewModel::onPlaceAdded
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
                is GlobeCommand.FocusPlace -> controller.focusPlace(command.id)
                is GlobeCommand.PreviewPlace -> controller.previewPlace(command.place)
                is GlobeCommand.SetPlaces -> controller.setPlaces(command.places)
                is GlobeCommand.SetLanguage -> controller.setLanguage(command.language.tag)
                is GlobeCommand.SetVisitedColor -> controller.setVisitedColor(command.hex)
            }
        }
    }

    var query by remember { mutableStateOf("") }
    // Aperta/chiusa la lista dei risultati, a parte dal testo: si tocca il globo
    // e la lista sparisce, ma la parola scritta resta nel campo da correggere o
    // cancellare a mano.
    var resultsOpen by remember { mutableStateOf(false) }
    var sheetOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    // Conferma dell'azzeramento. Sta qui e non dentro le impostazioni perche'
    // il dialogo va disegnato in cima a tutto, come quello dell'import.
    var confirmClear by remember { mutableStateOf(false) }
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
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()

    // Selettore di file di sistema: nessun permesso da dichiarare, e il file
    // puo' finire dove vuole l'utente, Drive compreso.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportTo) }

    // Tipo aperto di proposito: molti gestori di file non mostrano i .json se
    // si filtra su application/json, e un backup che non si riesce a
    // selezionare non serve a niente. La validita' la controlla Backup.decode.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importFrom) }

    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val noticeText = notice?.let {
        appString(
            when (it) {
                BackupNotice.EXPORT_OK -> R.string.backup_export_ok
                BackupNotice.EXPORT_FAIL -> R.string.backup_export_fail
                BackupNotice.IMPORT_FAIL -> R.string.backup_import_fail
                BackupNotice.IMPORT_OK -> R.string.backup_import_ok
                BackupNotice.CLEARED -> R.string.data_cleared
            }
        )
    }
    LaunchedEffect(notice) {
        if (noticeText != null) {
            Toast.makeText(context, noticeText, Toast.LENGTH_SHORT).show()
            viewModel.clearNotice()
        }
    }

    // Il colore scelto arriva qui a tutta la UI nativa: contatore, bordo della
    // ricerca, pallini, cursore. Il globo lo riceve per conto suo, via comando.
    val accent = remember(state.visitedColor) {
        parseOrDefault(state.visitedColor)
    }

    ProvideAppLanguage(state.language) {
      CompositionLocalProvider(LocalVisitedColor provides accent) {
        Box(Modifier.fillMaxSize()) {

            // Toccare il globo chiude la lista dei risultati e abbassa la
            // tastiera, lasciando il testo scritto dov'e'. E' il segnale
            // affidabile: il fuoco no, perche' se lo prende la WebView e
            // Compose spesso non se ne accorge (vedi SearchPanel).
            //
            // Il gesto si guarda nella passata INITIAL, che scende dal padre al
            // figlio, cosi' lo vediamo prima che la WebView se lo prenda; e non
            // lo consumiamo, quindi il globo continua a girare e a rispondere ai
            // tocchi come sempre. Il riquadro sta sotto la Column del pannello:
            // i tocchi sulla barra e sulle righe non arrivano fin qui, perche'
            // Box consegna al figlio piu' in alto e si ferma li'.
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial
                            )
                            resultsOpen = false
                            keyboard?.hide()
                        }
                    }
            ) {
                GlobeWebView(
                    controller = controller,
                    bridge = bridge,
                    modifier = Modifier.fillMaxSize()
                )
            }

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
                // Contatore a sinistra, ingranaggio a destra: si allinea al
                // bordo destro della barra di ricerca qui sotto, che e' quella
                // a dare la larghezza alla colonna.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CounterChip(
                        visitedCount = state.visitedCount,
                        total = state.total,
                        isReady = state.isReady,
                        onClick = { sheetOpen = true }
                    )
                    SettingsButton(onClick = { settingsOpen = true })
                }

                SearchPanel(
                    query = query,
                    // Scrivere riapre sempre la lista: se hai chiuso toccando il
                    // globo e poi correggi una lettera, i risultati tornano.
                    onQueryChange = { query = it; resultsOpen = true },
                    resultsOpen = resultsOpen,
                    onResultsOpenChange = { resultsOpen = it },
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

            pendingImport?.let { data ->
                AlertDialog(
                    onDismissRequest = viewModel::cancelImport,
                    containerColor = PanelSolid,
                    titleContentColor = OnPanel,
                    textContentColor = OnPanel,
                    title = { Text(appString(R.string.backup_import_title)) },
                    text = {
                        Text(
                            appString(
                                R.string.backup_import_body,
                                data.visited.size,
                                data.places.size
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = viewModel::confirmImport) {
                            Text(appString(R.string.backup_import_confirm), color = LocalVisitedColor.current)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::cancelImport) {
                            Text(appString(R.string.cancel), color = OnPanelMuted)
                        }
                    }
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
                    onRemove = { country -> viewModel.toggleVisited(country.code) }
                )
            }

            // Ultima nel Box, quindi sopra tutto il resto: e' una schermata
            // intera, non un pannello che galleggia sul globo.
            if (settingsOpen) {
                SettingsScreen(
                    language = state.language,
                    onLanguageChange = viewModel::setLanguage,
                    visitedColor = state.visitedColor,
                    onVisitedColorChange = viewModel::setVisitedColor,
                    pinsVisible = state.pinsVisible,
                    onPinsVisibleChange = viewModel::setPinsVisible,
                    onExport = { exportLauncher.launch(Backup.fileName()) },
                    // Tipo aperto di proposito, vedi importLauncher: il file
                    // scritto e' un .json, ma molti gestori di file lo
                    // nasconderebbero se qui filtrassimo su application/json.
                    onImport = { importLauncher.launch(arrayOf("*/*")) },
                    onClearData = { confirmClear = true },
                    appVersion = BuildConfig.VERSION_NAME,
                    onClose = { settingsOpen = false }
                )
            }

            // Sopra le impostazioni, da cui nasce. Dice quanto sta per sparire:
            // "azzera tutto" senza numeri non si sa mai bene cosa costi.
            if (confirmClear) {
                AlertDialog(
                    onDismissRequest = { confirmClear = false },
                    containerColor = PanelSolid,
                    titleContentColor = OnPanel,
                    textContentColor = OnPanel,
                    title = { Text(appString(R.string.data_clear_title)) },
                    text = {
                        Text(
                            appString(
                                R.string.data_clear_body,
                                state.visitedCount,
                                state.places.size
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmClear = false
                            viewModel.clearAllData()
                        }) {
                            Text(appString(R.string.data_clear_confirm), color = DangerRed)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmClear = false }) {
                            Text(appString(R.string.cancel), color = OnPanelMuted)
                        }
                    }
                )
            }
        }
      }
    }
}
