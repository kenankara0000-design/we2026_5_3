package com.example.we2026_5

/**
 * Repräsentiert ein Intervall innerhalb einer KundenListe
 * Jedes Intervall hat eigene Abholungs- und Auslieferungsdaten
 */
data class ListeIntervall(
    val abholungDatum: Long = 0, // Timestamp des Abholungsdatums
    val auslieferungDatum: Long = 0, // Timestamp des Auslieferungsdatums
    val wiederholen: Boolean = false, // Ob das Intervall wiederholt wird
    val intervallTage: Int = 7, // Intervall in Tagen (1-365), nur relevant wenn wiederholen=true
    val intervallAnzahl: Int = 0 // Anzahl der Wiederholungen (0 = unbegrenzt), nur relevant wenn wiederholen=true
)
