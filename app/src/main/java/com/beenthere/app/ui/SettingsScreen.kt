package com.beenthere.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beenthere.app.R
import com.beenthere.app.data.AppLanguage
import com.beenthere.app.data.VisitedColors
import com.beenthere.app.ui.theme.Background
import com.beenthere.app.ui.theme.BorderGray
import com.beenthere.app.ui.theme.LocalVisitedColor
import com.beenthere.app.ui.theme.OnPanel
import com.beenthere.app.ui.theme.OnPanelMuted
import com.beenthere.app.ui.theme.Panel
import com.beenthere.app.ui.theme.PanelSolid

/**
 * Impostazioni: lingua, aspetto del globo, backup, azzeramento e crediti. E'
 * una schermata intera e non un foglio perche' e' l'unico posto dove finisce
 * quello che non sta sul globo, e un foglio smette di bastare appena le voci
 * crescono.
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
    visitedColor: String,
    onVisitedColorChange: (String) -> Unit,
    pinsVisible: Boolean,
    onPinsVisibleChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClearData: () -> Unit,
    appVersion: String,
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

            SectionHeader(appString(R.string.settings_section_globe))
            SettingLabel(appString(R.string.settings_color))
            ColorPicker(selected = visitedColor, onSelect = onVisitedColorChange)
            SettingsToggle(
                label = appString(R.string.settings_pins),
                hint = appString(R.string.settings_pins_hint),
                checked = pinsVisible,
                onCheckedChange = onPinsVisibleChange
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

            SectionHeader(appString(R.string.settings_section_data))
            SettingsAction(
                label = appString(R.string.data_clear),
                hint = appString(R.string.data_clear_hint),
                onClick = onClearData,
                // L'unica voce che cancella: il rosso e' l'unico avviso che si
                // vede prima di toccarla.
                labelColor = DangerRed
            )

            SectionHeader(appString(R.string.settings_section_about))
            AboutBlock(appVersion)
        }
    }
}

/** Rosso degli avvisi. Non sta nel tema: e' l'unico punto dell'app che lo usa. */
internal val DangerRed = Color(0xFFFF6B6B)

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
    val accent = LocalVisitedColor.current
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppLanguage.entries.forEach { candidate ->
            val selected = candidate == language
            FilterChip(
                selected = selected,
                onClick = { if (!selected) onSelect(candidate) },
                label = { Text(candidate.tag.uppercase(), fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent,
                    selectedLabelColor = PanelSolid,
                    labelColor = OnPanelMuted
                )
            )
        }
    }
}

/**
 * I colori disponibili, come pastiglie. Il segno di spunta sta DENTRO la
 * pastiglia scelta e non accanto: su sei cerchi vicini un contorno da solo si
 * legge male, e il nome del colore non c'e' - il colore e' il nome.
 */
@Composable
private fun ColorPicker(selected: String, onSelect: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        VisitedColors.ALL.forEach { choice ->
            val isSelected = choice.hex == selected
            val swatch = Color(android.graphics.Color.parseColor(choice.hex))
            val description = appString(choice.label)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .clickable { if (!isSelected) onSelect(choice.hex) }
                    .semantics { contentDescription = description }
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        // Sul fondo pieno del colore: lo stesso nero del globo,
                        // che resta leggibile su tutte e sei le tinte.
                        tint = Background,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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

/** Etichetta di un controllo che non e' ne' un bottone ne' un interruttore. */
@Composable
private fun SettingLabel(text: String) {
    Text(
        text = text,
        color = OnPanel,
        fontSize = 15.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 2.dp)
    )
}

/**
 * Una voce con la sua spiegazione sotto. La spiegazione non e' decorazione:
 * "Importa" da solo non dice che sostituisce tutto, e uno se ne accorgerebbe
 * troppo tardi.
 */
@Composable
private fun SettingsAction(
    label: String,
    hint: String,
    onClick: () -> Unit,
    labelColor: Color = OnPanel
) {
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
        Text(text = label, color = labelColor, fontSize = 15.sp)
        Text(text = hint, color = OnPanelMuted, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

/** Come [SettingsAction], ma con un interruttore invece di un tocco. */
@Composable
private fun SettingsToggle(
    label: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val accent = LocalVisitedColor.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(shape)
            .border(1.dp, BorderGray, shape)
            // Tutta la riga commuta, non il solo interruttore: e' un bersaglio
            // di 40dp contro uno di 20.
            .clickable { onCheckedChange(!checked) }
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        ) {
            Text(text = label, color = OnPanel, fontSize = 15.sp)
            Text(text = hint, color = OnPanelMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PanelSolid,
                checkedTrackColor = accent,
                checkedBorderColor = accent,
                uncheckedThumbColor = OnPanelMuted,
                uncheckedTrackColor = PanelSolid,
                uncheckedBorderColor = BorderGray
            )
        )
    }
}

/**
 * Versione e provenienza dei dati. I confini e le citta' sono di Natural Earth
 * e il globo gira su librerie di altri: dirlo non e' cortesia, e' il minimo
 * dovuto a chi ha fatto quel lavoro.
 */
@Composable
private fun AboutBlock(appVersion: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 28.dp)
    ) {
        Text(
            text = appString(R.string.about_version, appVersion),
            color = OnPanel,
            fontSize = 14.sp
        )
        Text(
            text = appString(R.string.about_credits),
            color = OnPanelMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Text(
            text = appString(R.string.about_offline),
            color = OnPanelMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}
