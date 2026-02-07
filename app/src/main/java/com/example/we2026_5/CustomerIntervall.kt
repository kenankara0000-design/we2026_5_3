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
    val intervallAnzahl: Int = 0, // Anzahl der Wiederholungen (0 = unbegrenzt), nur relevant wenn wiederholen=true
    val erstelltAm: Long = System.currentTimeMillis(),
    val terminRegelId: String = "", // ID der Termin-Regel, aus der dieses Intervall erstellt wurde (optional)
    val regelTyp: TerminRegelTyp = TerminRegelTyp.WEEKLY,
    val tourSlotId: String = "",
    val zyklusTage: Int = intervallTage,
    /** Nur bei regelTyp == MONTHLY_WEEKDAY: Woche im Monat (1=erste, 2=zweite, 3=dritte, 4=vierte, 5=letzte). */
    val monthWeekOfMonth: Int = 0,
    /** Nur bei regelTyp == MONTHLY_WEEKDAY: Wochentag 0=Mo .. 6=So. */
    val monthWeekday: Int = -1
)
