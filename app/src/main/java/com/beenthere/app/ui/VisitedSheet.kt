package com.beenthere.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beenthere.app.R
import com.beenthere.app.data.AppLanguage
import com.beenthere.app.data.Country
import com.beenthere.app.ui.theme.BorderGray
import com.beenthere.app.ui.theme.OnPanel
import com.beenthere.app.ui.theme.OnPanelMuted
import com.beenthere.app.ui.theme.PanelSolid
import com.beenthere.app.ui.theme.Visited

/**
 * Elenco dei paesi visitati, aperto dal contatore. Ospita anche il selettore
 * della lingua: e' l'unico pannello dell'app, non serve una schermata a parte.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitedSheet(
    countries: List<Country>,
    total: Int,
    language: AppLanguage,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelect: (Country) -> Unit,
    onRemove: (Country) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PanelSolid,
        contentColor = OnPanel
    ) {
        Column(Modifier.navigationBarsPadding()) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = appString(R.string.visited_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnPanel
                    )
                    Text(
                        text = appString(R.string.counter, countries.size, total),
                        fontSize = 13.sp,
                        color = OnPanelMuted
                    )
                }
                LanguagePicker(language = language, onSelect = onLanguageChange)
            }

            HorizontalDivider(color = BorderGray)

            if (countries.isEmpty()) {
                Text(
                    text = appString(R.string.visited_empty),
                    color = OnPanelMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(20.dp)
                )
            } else {
                LazyColumn(Modifier.padding(vertical = 4.dp)) {
                    items(countries, key = { it.code }) { country ->
                        CountryRow(
                            country = country,
                            language = language,
                            isVisited = true,
                            onClick = { onSelect(country) },
                            onToggle = { onRemove(country) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePicker(
    language: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppLanguage.entries.forEach { candidate ->
            val selected = candidate == language
            FilterChip(
                selected = selected,
                onClick = { if (!selected) onSelect(candidate) },
                label = { Text(candidate.tag.uppercase(), fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Visited,
                    selectedLabelColor = PanelSolid,
                    labelColor = OnPanelMuted
                )
            )
        }
    }
}
