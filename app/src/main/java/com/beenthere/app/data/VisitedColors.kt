package com.beenthere.app.data

import androidx.annotation.StringRes
import com.beenthere.app.R

/**
 * I colori offerti per i paesi visitati.
 *
 * Sono una tavolozza chiusa e non un selettore libero: il colore deve staccare
 * sia dal grigio della terra (#8A8A8A) sia dal blu dell'oceano (#1B4B8F), e
 * lasciando scegliere qualunque tinta si finisce col grigio su grigio.
 *
 * Questa e' l'unica definizione: il tema Compose parte da [DEFAULT] e il globo
 * riceve il testo esadecimale attraverso il ponte. Un colore aggiunto qui
 * compare da solo nelle impostazioni.
 */
object VisitedColors {

    data class Choice(val hex: String, @StringRes val label: Int)

    /** L'arancione di sempre: chi non ha mai scelto ha questo. */
    const val DEFAULT = "#FF8C1A"

    val ALL: List<Choice> = listOf(
        Choice(DEFAULT,   R.string.color_orange),
        Choice("#FF4D4D", R.string.color_red),
        Choice("#FFD34E", R.string.color_yellow),
        Choice("#35D07F", R.string.color_green),
        Choice("#22D3EE", R.string.color_cyan),
        Choice("#A78BFA", R.string.color_purple)
    )

    /**
     * Riporta a un colore della tavolozza qualunque cosa arrivi da DataStore.
     * Serve perche' il valore salvato sopravvive agli aggiornamenti dell'app:
     * se un domani un colore viene tolto, chi ce l'aveva non si ritrova con un
     * globo di un colore che il resto della UI non conosce.
     */
    fun sanitize(raw: String?): String {
        val wanted = raw?.trim()?.uppercase() ?: return DEFAULT
        return ALL.firstOrNull { it.hex == wanted }?.hex ?: DEFAULT
    }
}
