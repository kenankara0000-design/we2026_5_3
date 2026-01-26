package com.example.we2026_5

/**
 * Repräsentiert ein Intervall für einen einzelnen Kunden (Gewerblich)
 * Jedes Intervall hat eigene Abholungs- und Auslieferungsdaten
 * Ein Kunde kann mehrere Intervalle haben
 */
data class CustomerIntervall(
    val id: String = java.util.UUID.randomUUID().toString(),
    val abholungDatum: Long = 0, // Timestamp des Abholungsdatums
    val auslieferungDatum: Long = 0, // Timestamp des Auslieferungsdatums
    val wiederholen: Boolean = false, // Ob das Intervall wiederholt wird
    val intervallTage: Int = 7, // Intervall in Tagen (1-365), nur relevant wenn wiederholen=true
    val erstelltAm: Long = System.currentTimeMillis()
)
