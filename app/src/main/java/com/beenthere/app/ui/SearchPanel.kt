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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
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
import com.beenthere.app.ui.theme.BorderGray
import com.beenthere.app.ui.theme.OnPanel
import com.beenthere.app.ui.theme.OnPanelMuted
import com.beenthere.app.ui.theme.Panel
import com.beenthere.app.ui.theme.Visited

/**
 * Campo di ricerca e risultati. La ricerca e' interamente nativa: il catalogo
 * arriva dal JavaScript una volta sola all'avvio, quindi il campo resta
 * disabilitato finche' il globo non e' pronto (circa un secondo).
 *
 * Tocco sulla riga: il globo ruota e zooma sul paese.
 * Tocco sul pallino: segna/desegna visitato, senza muovere il globo.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Country>,
    visited: Set<String>,
    language: AppLanguage,
    isReady: Boolean,
    onSelect: (Country) -> Unit,
    onToggle: (Country) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val shape = RoundedCornerShape(14.dp)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {

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
                        results.firstOrNull()?.let {
                            onSelect(it)
                            keyboard?.hide()
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
                if (results.isEmpty()) {
                    Text(
                        text = appString(R.string.search_no_results),
                        color = OnPanelMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(results, key = { it.code }) { country ->
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
                    }
                }
            }
        }
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
