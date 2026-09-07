package com.beenthere.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Quello che sta dentro un file di backup: tutto cio' che l'utente ha messo nell'app. */
data class BackupData(
    val visited: Set<String>,
    val places: List<Place>
)

/**
 * Il file di backup e' l'unica copia dei dati che esce dall'app, e l'unica via
 * per portarseli su un altro telefono: l'app non ha rete e non sincronizza
 * niente.
 *
 * Contiene **paesi e luoghi insieme**. Un backup dei soli pin lascerebbe fuori
 * meta' del lavoro dell'utente, e se ne accorgerebbe solo dopo averne avuto
 * bisogno.
 */
object Backup {

    /**
     * Sta in testa al file apposta: un domani il formato puo' cambiare senza
     * che i file vecchi diventino illeggibili, perche' si sa da cosa si parte.
     */
    const val VERSION = 1

    private const val KIND = "been-there-backup"

    fun encode(visited: Set<String>, places: List<Place>): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
        return JSONObject()
            .put("kind", KIND)
            .put("version", VERSION)
            .put("exportedAt", stamp)
            .put("visited", JSONArray(visited.sorted()))
            // Stesso formato con cui i luoghi stanno in DataStore: un formato
            // solo, non due da tenere allineati.
            .put("places", JSONArray(Place.listToJson(places)))
            .toString(2)
    }

    /**
     * Legge un file scelto dall'utente. Torna null se non e' un backup: si
     * apre da un selettore di file, quindi puo' arrivare qualsiasi cosa, e un
     * file sbagliato non deve cancellare i dati veri.
     */
    fun decode(raw: String): BackupData? = runCatching {
        val root = JSONObject(raw)
        if (root.optString("kind") != KIND) return null
        if (root.optInt("version", 0) > VERSION) return null   // scritto da una versione futura

        val visitedArray = root.optJSONArray("visited") ?: JSONArray()
        val visited = (0 until visitedArray.length())
            .mapNotNull { visitedArray.optString(it).takeIf { s -> s.isNotBlank() } }
            .toSet()

        val places = Place.listFromJson(root.optJSONArray("places")?.toString())
        BackupData(visited = visited, places = places)
    }.getOrElse {
        android.util.Log.e("BeenThere", "backup non leggibile", it)
        null
    }

    /** been-there-2026-09-07.json */
    fun fileName(): String =
        "been-there-" + SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()) + ".json"
}
