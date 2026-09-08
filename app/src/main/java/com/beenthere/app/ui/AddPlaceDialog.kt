package com.beenthere.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.beenthere.app.PlaceDraft
import com.beenthere.app.R
import com.beenthere.app.ui.theme.BorderGray
import com.beenthere.app.ui.theme.OnPanel
import com.beenthere.app.ui.theme.OnPanelMuted
import com.beenthere.app.ui.theme.PanelSolid
import com.beenthere.app.ui.theme.LocalVisitedColor

/**
 * Nome + coordinate, e basta: un luogo non ha data ne' nota.
 *
 * Il campo delle coordinate accetta la stringa **intera** copiata da un'altra
 * app ("41.9028, 12.4964"). Non e' un vezzo: l'app non ha rete, quindi le
 * coordinate arrivano per forza da fuori, e obbligare a spezzarle in due caselle
 * renderebbe questa strada inservibile.
 */
@Composable
fun AddPlaceDialog(
    draft: PlaceDraft,
    onDismiss: () -> Unit,
    onConfirm: (name: String, lat: Double, lng: Double) -> Unit
) {
    var name by remember(draft) { mutableStateOf(draft.name) }
    var coords by remember(draft) {
        mutableStateOf(
            if (draft.lat != null && draft.lng != null) formatCoords(draft.lat, draft.lng) else ""
        )
    }
    var showError by remember(draft) { mutableStateOf(false) }

    val parsed = parseCoords(coords)
    val canConfirm = name.isNotBlank() && parsed != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PanelSolid,
        titleContentColor = OnPanel,
        textContentColor = OnPanel,
        title = { Text(appString(R.string.place_new_title), fontSize = 17.sp) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(appString(R.string.place_name)) },
                    placeholder = { Text(appString(R.string.place_name_hint)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = coords,
                    onValueChange = { coords = it; showError = false },
                    singleLine = true,
                    isError = showError,
                    label = { Text(appString(R.string.place_coords)) },
                    placeholder = { Text(appString(R.string.place_coords_hint)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
                if (showError) {
                    Text(
                        text = appString(R.string.place_coords_invalid),
                        color = LocalVisitedColor.current,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val c = parseCoords(coords)
                    if (name.isBlank() || c == null) showError = true
                    else onConfirm(name.trim(), c.first, c.second)
                }
            ) {
                Text(
                    appString(R.string.place_create),
                    color = if (canConfirm) LocalVisitedColor.current else OnPanelMuted
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(appString(R.string.cancel), color = OnPanelMuted)
            }
        }
    )
}

@Composable
internal fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = OnPanel,
    unfocusedTextColor = OnPanel,
    focusedBorderColor = LocalVisitedColor.current,
    unfocusedBorderColor = BorderGray,
    focusedLabelColor = LocalVisitedColor.current,
    unfocusedLabelColor = OnPanelMuted,
    cursorColor = LocalVisitedColor.current
)

private fun formatCoords(lat: Double, lng: Double): String {
    fun r(v: Double) = (Math.round(v * 10_000.0) / 10_000.0).toString()
    return r(lat) + ", " + r(lng)
}

private val NUMBER = Regex("-?\\d+(?:\\.\\d+)?")

/**
 * Due numeri da una stringa qualsiasi. Si prova prima col punto decimale, e i
 * separatori intorno non contano ("41.9, 12.5", "41.9 12.5", "lat 41.9 lng
 * 12.5" vanno tutti bene); se cosi' non escono esattamente due numeri si
 * riprova considerando la virgola come separatore decimale, per chi scrive
 * "41,9 12,5". Fuori intervallo si rifiuta: un errore di battitura non deve
 * piantare un pin dall'altra parte del mondo.
 */
fun parseCoords(text: String): Pair<Double, Double>? {
    var parts = NUMBER.findAll(text).map { it.value }.toList()
    if (parts.size != 2) parts = NUMBER.findAll(text.replace(',', '.')).map { it.value }.toList()
    if (parts.size != 2) return null
    val lat = parts[0].toDoubleOrNull() ?: return null
    val lng = parts[1].toDoubleOrNull() ?: return null
    if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) return null
    return lat to lng
}
