package com.beenthere.app.data

import org.json.JSONArray
import java.text.Normalizer
import java.util.Locale

/**
 * Un paese del dataset. Il [code] arriva dal JavaScript, che lo calcola con la
 * funzione unica di identificazione (ISO_A3 -> ADM0_A3 -> nome normalizzato).
 * E' anche la chiave salvata in DataStore.
 */
data class Country(
    val code: String,
    val nameIt: String,
    val nameEn: String
) {
    fun name(language: AppLanguage): String = when (language) {
        AppLanguage.IT -> nameIt
        AppLanguage.EN -> nameEn
    }
}

enum class AppLanguage(val tag: String) {
    IT("it"),
    EN("en");

    companion object {
        val DEFAULT = IT
        fun fromTag(tag: String?): AppLanguage =
            AppLanguage.entries.firstOrNull { it.tag == tag } ?: DEFAULT
    }
}

/**
 * Catalogo completo, passato una volta sola dal JavaScript all'avvio.
 * Tiene pronte le stringhe normalizzate: la ricerca gira a ogni tasto premuto e
 * non deve rifare la normalizzazione di 242 nomi ogni volta.
 */
class CountryCatalog(val countries: List<Country>) {

    private data class Entry(val country: Country, val haystackIt: String, val haystackEn: String)

    private val entries: List<Entry> = countries.map {
        Entry(
            country = it,
            haystackIt = normalizeForSearch(it.nameIt + " " + it.code),
            haystackEn = normalizeForSearch(it.nameEn + " " + it.code)
        )
    }

    private val byCode: Map<String, Country> = countries.associateBy { it.code }

    val size: Int get() = countries.size

    operator fun get(code: String): Country? = byCode[code]

    /**
     * Cerca sul nome nella lingua selezionata piu' il codice ISO. I nomi
     * nell'altra lingua non fanno match: e' una scelta esplicita, si cambia
     * mettendo entrambi gli haystack nel confronto qui sotto.
     */
    fun search(query: String, language: AppLanguage, limit: Int = 25): List<Country> {
        val q = normalizeForSearch(query.trim())
        if (q.isEmpty()) return emptyList()

        val wordStart = ArrayList<Country>()
        val inside = ArrayList<Country>()
        for (entry in entries) {
            val hay = if (language == AppLanguage.IT) entry.haystackIt else entry.haystackEn
            val pos = hay.indexOf(q)
            if (pos < 0) continue
            // L'inizio di una parola qualsiasi pesa piu' di un match a meta' parola:
            // "ita" deve dare Italia prima di Mauritania.
            if (pos == 0 || !hay[pos - 1].isLetterOrDigit()) wordStart += entry.country else inside += entry.country
        }
        val cmp = compareBy<Country> { normalizeForSearch(it.name(language)) }
        wordStart.sortWith(cmp)
        inside.sortWith(cmp)
        return (wordStart + inside).take(limit)
    }

    /** Visitati, in ordine alfabetico secondo la lingua corrente. */
    fun visitedSorted(visited: Set<String>, language: AppLanguage): List<Country> =
        countries.filter { it.code in visited }
            .sortedBy { normalizeForSearch(it.name(language)) }

    companion object {
        val EMPTY = CountryCatalog(emptyList())

        /** Legge il JSON prodotto da BeenThere.catalog() lato WebView. */
        fun parse(json: String): CountryCatalog {
            val array = JSONArray(json)
            val list = ArrayList<Country>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val code = obj.optString("code").takeIf { it.isNotBlank() } ?: continue
                val english = obj.optString("nameEn").takeIf { it.isNotBlank() } ?: code
                // Dove NAME_IT manca nel dataset, l'etichetta italiana ricade sull'inglese.
                val italian = obj.optString("nameIt").takeIf { it.isNotBlank() } ?: english
                list += Country(code = code, nameIt = italian, nameEn = english)
            }
            return CountryCatalog(list)
        }
    }
}

private val COMBINING_MARKS = Regex("\\p{Mn}+")

/** Minuscole senza accenti: "Costa d'Avorio" e "costa d avorio" devono incontrarsi. */
fun normalizeForSearch(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)
