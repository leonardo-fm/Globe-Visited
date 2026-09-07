package com.beenthere.app.data

import android.content.Context
import org.json.JSONArray

/**
 * Un luogo: una citta' del catalogo, o - dalla fase 3 - un pin messo a mano.
 *
 * E' indipendente dal paese: piantare un pin a New York non segna gli Stati
 * Uniti, e segnare gli Stati Uniti non tocca i pin. [countryCode] serve solo a
 * mostrare bandiera e nome del paese accanto alla citta', e resta nullo per i
 * pochi luoghi che non si agganciano a nessuna feature del globo.
 */
data class Place(
    val id: String,
    val nameIt: String,
    val nameEn: String,
    val lat: Double,
    val lng: Double,
    val countryCode: String?
) {
    fun name(language: AppLanguage): String = when (language) {
        AppLanguage.IT -> nameIt
        AppLanguage.EN -> nameEn
    }
}

/**
 * Catalogo delle citta' preconfezionate, letto da `assets/cities.json`.
 *
 * Il file arriva da Natural Earth 1:10m populated places (capitali, capoluoghi
 * e tutto quello che supera i 100.000 abitanti; vedi `tools/build-cities.mjs`)
 * e porta l'`ADM0_A3` del paese, non la chiave dell'app. L'aggancio lo fa
 * [build] con [CountryCatalog.byNaturalEarthCode], cosi' la chiave resta quella
 * calcolata dal JavaScript - l'unica identificazione dei paesi dell'app.
 *
 * L'ordine del file e' per popolazione decrescente e la ricerca lo conserva:
 * "New York" deve uscire prima di "New York Mills".
 */
class PlaceCatalog(val places: List<Place>) {

    private data class Entry(val place: Place, val haystackIt: String, val haystackEn: String)

    private val entries: List<Entry> = places.map {
        Entry(
            place = it,
            haystackIt = normalizeForSearch(it.nameIt),
            haystackEn = normalizeForSearch(it.nameEn)
        )
    }

    val size: Int get() = places.size

    /**
     * Stesso criterio di [CountryCatalog.search] - inizio di parola prima di
     * meta' parola - ma senza riordinare alfabeticamente: dentro ogni gruppo
     * resta l'ordine del file, che e' per popolazione.
     */
    fun search(query: String, language: AppLanguage, limit: Int = 25): List<Place> {
        val q = normalizeForSearch(query.trim())
        if (q.isEmpty()) return emptyList()

        val wordStart = ArrayList<Place>()
        val inside = ArrayList<Place>()
        for (entry in entries) {
            val hay = if (language == AppLanguage.IT) entry.haystackIt else entry.haystackEn
            val pos = hay.indexOf(q)
            if (pos < 0) continue
            if (pos == 0 || !hay[pos - 1].isLetterOrDigit()) wordStart += entry.place else inside += entry.place
            if (wordStart.size >= limit) break
        }
        return (wordStart + inside).take(limit)
    }

    companion object {
        val EMPTY = PlaceCatalog(emptyList())

        const val ASSET = "cities.json"

        /**
         * Legge il file grezzo dagli asset. Va chiamata fuori dal main thread:
         * sono ~300 KB e ~4.200 voci.
         */
        fun readAsset(context: Context): JSONArray? = runCatching {
            JSONArray(context.assets.open(ASSET).bufferedReader().use { it.readText() })
        }.getOrElse {
            android.util.Log.e("BeenThere", "cities.json non leggibile", it)
            null
        }

        /**
         * Unisce il file grezzo al catalogo dei paesi. Finche' il globo non ha
         * consegnato i paesi non si costruisce niente: senza di loro le citta'
         * resterebbero senza bandiera e senza paese, e la ricerca mostrerebbe
         * righe monche.
         */
        fun build(raw: JSONArray?, countries: CountryCatalog): PlaceCatalog {
            if (raw == null || countries.size == 0) return EMPTY
            val list = ArrayList<Place>(raw.length())
            for (i in 0 until raw.length()) {
                val obj = raw.optJSONObject(i) ?: continue
                val id = obj.optString("i").takeIf { it.isNotBlank() } ?: continue
                val english = obj.optString("n").takeIf { it.isNotBlank() } ?: continue
                // "t" c'e' solo dove il nome italiano differisce dall'inglese:
                // su 4.205 citta' succede 735 volte, e ripeterlo sempre
                // gonfierebbe il file per niente.
                val italian = obj.optString("t").takeIf { it.isNotBlank() } ?: english
                val lat = obj.optDouble("y", Double.NaN)
                val lng = obj.optDouble("x", Double.NaN)
                if (lat.isNaN() || lng.isNaN()) continue
                // Tre citta' su 4.205 (Gibilterra, Svalbard, Tokelau) portano un
                // ADM0_A3 che sul 50m non esiste come feature: restano senza
                // paese, quindi senza bandiera, ma cercabili come tutte le altre.
                val country = obj.optString("a").takeIf { it.isNotBlank() }
                    ?.let { countries.byNaturalEarthCode(it) }
                list += Place(
                    id = id,
                    nameIt = italian,
                    nameEn = english,
                    lat = lat,
                    lng = lng,
                    countryCode = country?.code
                )
            }
            return PlaceCatalog(list)
        }
    }
}
