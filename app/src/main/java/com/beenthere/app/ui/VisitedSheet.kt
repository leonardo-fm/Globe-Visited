package com.beenthere.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * Elenco dei paesi visitati, aperto dal contatore. Fa solo questo: lingua e
 * backup stavano qui quando erano le uniche due voci di configurazione, e sono
 * passate in [SettingsScreen] quando ha smesso di essere una scusa buona.
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
    onRemove: (Country) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PanelSolid,
        contentColor = OnPanel
    ) {
        Column(Modifier.navigationBarsPadding()) {

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            ) {
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

