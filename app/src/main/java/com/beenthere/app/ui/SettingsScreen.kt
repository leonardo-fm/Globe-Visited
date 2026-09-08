package com.beenthere.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beenthere.app.R
import com.beenthere.app.data.AppLanguage
import com.beenthere.app.ui.theme.Background
import com.beenthere.app.ui.theme.BorderGray
import com.beenthere.app.ui.theme.OnPanel
import com.beenthere.app.ui.theme.OnPanelMuted
import com.beenthere.app.ui.theme.Panel
import com.beenthere.app.ui.theme.PanelSolid
import com.beenthere.app.ui.theme.Visited

/**
 * Impostazioni: lingua e backup. E' una schermata intera e non un foglio
 * perche' e' l'unico posto dove finiranno le cose che non stanno sul globo, e
 * un foglio smette di bastare appena le voci crescono.
 *
 * Non c'e' una libreria di navigazione e non serve: e' disegnata sopra il
 * globo dentro lo stesso Box, e si chiude con la freccia o col tasto indietro
 * del telefono. Il fondo e' OPACO apposta - il globo dietro continuerebbe a
 * disegnare e a rispondere ai tocchi, e qui non deve.
 */
@Composable
fun SettingsScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            // Il globo sta sotto, nello stesso Box: senza questo i tocchi che
            // cadono negli spazi vuoti della pagina lo farebbero girare dietro
            // alle impostazioni.
            .pointerInput(Unit) {}
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = appString(R.string.settings_back),
                tint = OnPanel,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onClose)
                    .padding(10.dp)
                    .size(22.dp)
            )
            Text(
                text = appString(R.string.settings_title),
                color = OnPanel,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp)
            )
        }

        HorizontalDivider(color = BorderGray)

        Column(Modifier.verticalScroll(rememberScrollState())) {

            SectionHeader(appString(R.string.settings_section_language))
            LanguagePicker(
                language = language,
                onSelect = onLanguageChange,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            SectionHeader(appString(R.string.settings_section_backup))
            SettingsAction(
                label = appString(R.string.backup_export),
                hint = appString(R.string.backup_export_hint),
                onClick = onExport
            )
            SettingsAction(
                label = appString(R.string.backup_import),
                hint = appString(R.string.backup_import_hint),
                onClick = onImport
            )
        }
    }
}

/**
 * L'ingranaggio accanto al contatore. Stessa forma e stesso bordo del
 * contatore: sopra il globo ci sono due soli comandi e devono sembrare
 * parenti.
 */
@Composable
fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(999.dp)
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = appString(R.string.settings_open),
        tint = OnPanel,
        modifier = modifier
            .clip(shape)
            .background(Panel, shape)
            .border(1.dp, BorderGray, shape)
            .clickable(onClick = onClick)
            .padding(8.dp)
            .size(19.dp)
    )
}

/** Le due lingue, a scelta manuale: l'app non segue quella di sistema. */
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

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = OnPanelMuted,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp)
    )
}

/**
 * Una voce con la sua spiegazione sotto. La spiegazione non e' decorazione:
 * "Importa" da solo non dice che sostituisce tutto, e uno se ne accorgerebbe
 * troppo tardi.
 */
@Composable
private fun SettingsAction(label: String, hint: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(shape)
            .border(1.dp, BorderGray, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(text = label, color = OnPanel, fontSize = 15.sp)
        Text(text = hint, color = OnPanelMuted, fontSize = 12.sp, lineHeight = 16.sp)
    }
}
