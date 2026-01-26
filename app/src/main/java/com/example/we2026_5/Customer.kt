package com.example.we2026_5

import java.util.concurrent.TimeUnit

data class Customer(
    val id: String = "",
    val name: String = "",
    val adresse: String = "",
    val telefon: String = "",
    val notizen: String = "",
    // Kunden-Art und Liste
    val kundenArt: String = "Gewerblich", // "Privat" oder "Gewerblich"
    val listeId: String = "", // ID der Liste (nur für Privat-Kunden)
    // Termine
    val abholungDatum: Long = 0, // Erstes Abholungsdatum
    val auslieferungDatum: Long = 0, // Erstes Auslieferungsdatum
    val wiederholen: Boolean = false, // Ob Termine wiederholt werden sollen
    // Wiederholungs-Intervall (nur wenn wiederholen = true)
    val intervallTage: Int = 7,
    val letzterTermin: Long = 0,
    val wochentag: Int = 0, // 0=Montag, 1=Dienstag, ..., 6=Sonntag (wird von Liste übernommen für Privat-Kunden)
    val reihenfolge: Int = 1, // Reihenfolge der Abholung an diesem Tag (1, 2, 3, ...)
    // Status
    val abholungErfolgt: Boolean = false,
    val auslieferungErfolgt: Boolean = false,
    val urlaubVon: Long = 0,
    val urlaubBis: Long = 0,
    val verschobenAufDatum: Long = 0,
    val fotoUrls: List<String> = listOf(),
    val istImUrlaub: Boolean = false
) {
    /**
     * Berechnet das nächste Fälligkeitsdatum basierend auf letzterTermin und Intervall.
     * Berücksichtigt verschobenAufDatum.
     * Wenn wiederholen = false, wird abholungDatum zurückgegeben.
     */
    fun getFaelligAm(): Long {
        if (!wiederholen) {
            // Einmaliger Termin: Abholungsdatum verwenden
            return if (verschobenAufDatum > 0) verschobenAufDatum else abholungDatum
        }
        // Wiederholender Termin: Alte Logik
        if (verschobenAufDatum > 0) return verschobenAufDatum
        return letzterTermin + TimeUnit.DAYS.toMillis(intervallTage.toLong())
    }
}