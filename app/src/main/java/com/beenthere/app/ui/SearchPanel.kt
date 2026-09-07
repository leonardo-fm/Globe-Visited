package com.beenthere.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusGroup
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beenthere.app.R
import com.beenthere.app.data.AppLanguage
import com.beenthere.app.data.Country
import com.beenthere.app.data.CountryCatalog
import com.beenthere.app.data.Place
import com.beenthere.app.ui.theme.BorderGray
import com.beenthere.app.ui.theme.OnPanel
import com.beenthere.app.ui.theme.OnPanelMuted
import com.beenthere.app.ui.theme.Panel
import com.beenthere.app.ui.theme.PlacePin
import com.beenthere.app.ui.theme.Visited

/**
 * Campo di ricerca e risultati. La ricerca e' interamente nativa: il catalogo
 * dei paesi arriva dal JavaScript una volta sola all'avvio e quello delle citta'
 * dagli asset, quindi il campo resta disabilitato finche' il globo non e' pronto
 * (circa un secondo).
 *
 * Tocco sulla riga: il globo ruota e zooma sul paese o sulla citta'.
 * Tocco sul pallino: segna/desegna visitato, senza muovere il globo.
 *
 * Le citta' stanno sotto i paesi, in una sezione a parte: sono migliaia contro
 * 242, e mescolarle farebbe sparire il paese cercato in mezzo ai suoi capoluoghi.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Country>,
    placeResults: List<Place>,
    catalog: CountryCatalog,
    visited: Set<String>,
    pinnedPlaces: Set<String>,
    language: AppLanguage,
    isReady: Boolean,
    onSelect: (Country) -> Unit,
    onSelectPlace: (Place) -> Unit,
    onToggle: (Country) -> Unit,
    onTogglePlace: (Place) -> Unit,
    onCreatePlace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val shape = RoundedCornerShape(14.dp)

    // Tornando sulla ricerca si ricomincia da capo, invece di trovarci la parola
    // di prima. Si azzera in DUE momenti, e servono entrambi:
    //
    //  - quando il gruppo perde il fuoco, che e' il caso pulito. Il fuoco si
    //    guarda sul GRUPPO e non sul solo campo di testo perche' dentro il
    //    pannello c'e' anche il pallino di ogni riga: se rubasse il fuoco al
    //    campo, un controllo sul solo campo chiuderebbe la lista proprio mentre
    //    la si sta usando;
    //  - quando il campo RIACQUISTA il fuoco. Non e' una ridondanza: toccando
    //    il globo il fuoco se lo prende la WebView, che e' una View Android
    //    dentro una AndroidView, e Compose non sempre se ne accorge - quindi la
    //    perdita di fuoco puo' non arrivare mai. Questo secondo controllo non
    //    dipende da quella notifica.
    var hadFocus by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .onFocusChanged { state ->
                if (state.hasFocus && !hadFocus) onQueryChange("")
                if (!state.hasFocus) onQueryChange("")
                hadFocus = state.hasFocus
            }
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Panel, shape)
                .border(1.dp, if (query.isEmpty()) BorderGray else Visited, shape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = OnPanelMuted,
                modifier = Modifier.size(20.dp)
            )
            Box(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                if (query.isEmpty()) {
                    Text(
                        text = appString(
                            if (isReady) R.string.search_placeholder
                            else R.string.search_placeholder_loading
                        ),
                        color = OnPanelMuted,
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    enabled = isReady,
                    singleLine = true,
                    textStyle = TextStyle(color = OnPanel, fontSize = 15.sp),
                    cursorBrush = SolidColor(Visited),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        val country = results.firstOrNull()
                        val place = placeResults.firstOrNull()
                        when {
                            country != null -> { onSelect(country); keyboard?.hide() }
                            place != null -> { onSelectPlace(place); keyboard?.hide() }
                        }
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = appString(R.string.search_clear),
                    tint = OnPanelMuted,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onQueryChange("") }
                )
            }
        }

        if (query.isNotEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Panel, shape)
                    .border(1.dp, BorderGray, shape)
            ) {
                if (results.isEmpty() && placeResults.isEmpty()) {
                    // Se qui non esce niente, quel posto in catalogo non c'e':
                    // la via d'uscita e' crearlo a mano, e va offerta proprio
                    // qui, dove ci si accorge che manca.
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = appString(R.string.search_no_results),
                            color = OnPanelMuted,
                            fontSize = 14.sp
                        )
                        Text(
                            text = appString(R.string.place_create_here),
                            color = OnPanel,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
                                .clickable {
                                    onCreatePlace()
                                    keyboard?.hide()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(results, key = { "c:" + it.code }) { country ->
                            CountryRow(
                                country = country,
                                language = language,
                                isVisited = country.code in visited,
                                onClick = {
                                    onSelect(country)
                                    keyboard?.hide()
                                },
                                onToggle = { onToggle(country) }
                            )
                            if (country != results.last()) {
                                HorizontalDivider(color = BorderGray.copy(alpha = 0.5f))
                            }
                        }
                        if (placeResults.isNotEmpty()) {
                            item(key = "places-header") {
                                SectionHeader(appString(R.string.search_section_cities))
                            }
                            items(placeResults, key = { "p:" + it.id }) { place ->
                                PlaceRow(
                                    place = place,
                                    country = place.countryCode?.let { catalog[it] },
                                    language = language,
                                    isPinned = place.id in pinnedPlaces,
                                    onClick = {
                                        onSelectPlace(place)
                                        keyboard?.hide()
                                    },
                                    onToggle = { onTogglePlace(place) }
                                )
                                if (place != placeResults.last()) {
                                    HorizontalDivider(color = BorderGray.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Separa i paesi dalle citta' nella stessa lista di risultati. */
@Composable
private fun SectionHeader(text: String) {
    HorizontalDivider(color = BorderGray)
    Text(
        text = text.uppercase(),
        color = OnPanelMuted,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp)
    )
}

/**
 * Riga di una citta'. La bandiera e il nome del paese non sono decorazione: di
 * Springfield ce ne sono sette, e senza il paese non si sa quale si sta
 * toccando. Restano vuoti per le tre citta' il cui ADM0_A3 non corrisponde a
 * nessuna feature del globo.
 */
@Composable
fun PlaceRow(
    place: Place,
    country: Country?,
    language: AppLanguage,
    isPinned: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
    ) {
        // Larghezza fissa anche senza bandiera, cosi' i nomi restano allineati.
        val flag = country?.flag.orEmpty()
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            if (flag.isNotEmpty()) Text(text = flag, fontSize = 17.sp)
        }
        Column(Modifier.weight(1f).padding(start = 6.dp)) {
            Text(
                text = place.name(language),
                color = OnPanel,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (country != null) {
                Text(
                    text = country.name(language),
                    color = OnPanelMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // Stesso pallino dei paesi, altro significato e altro colore: acceso
        // vuol dire "e' sul globo". Un luogo non ha uno stato "visitato" e non
        // tocca il suo paese.
        VisitedDot(
            isVisited = isPinned,
            onToggle = onToggle,
            activeColor = PlacePin,
            onDescription = R.string.unmark_place,
            offDescription = R.string.mark_place
        )
    }
}

@Composable
fun CountryRow(
    country: Country,
    language: AppLanguage,
    isVisited: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Text(
            text = country.name(language),
            color = OnPanel,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = country.code,
            color = OnPanelMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        VisitedDot(isVisited = isVisited, onToggle = onToggle)
    }
}
