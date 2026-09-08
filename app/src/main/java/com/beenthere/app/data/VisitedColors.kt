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

    private val HEX = Regex("^#?([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$")

    /**
     * Da quello che l'utente ha scritto a un `#RRGGBB` maiuscolo, o null se non
     * e' un colore. Accetta anche la forma corta a tre cifre e il cancelletto
     * mancante, perche' e' quello che uno scrive davvero.
     *
     * E' l'unico punto in cui si decide se un codice va bene: chi lo usa non
     * deve piu' controllare niente.
     */
    fun normalize(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        val digits = HEX.find(text)?.groupValues?.get(1) ?: return null
        val full = if (digits.length == 3) {
            digits.map { "$it$it" }.joinToString("")
        } else {
            digits
        }
        return "#" + full.uppercase()
    }

    /**
     * Quello che arriva da DataStore, reso sicuro. Non e' piu' un controllo di
     * appartenenza alla tavolozza - da quando il colore si puo' scrivere a
     * mano, qualunque esadecimale valido e' legittimo - ma resta indispensabile:
     * garantisce a chi legge che la stringa sia sempre convertibile in colore,
     * e senza questa promessa il parse esploderebbe all'avvio.
     */
    fun sanitize(raw: String?): String = normalize(raw) ?: DEFAULT
}
