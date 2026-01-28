package com.example.we2026_5

/**
 * Repräsentiert eine wiederverwendbare Termin-Regel
 * Kann auf Kunden oder Listen angewendet werden
 */
data class TerminRegel(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "", // z.B. "Wöchentlich Montag", "Alle 2 Wochen", "Monatlich"
    val beschreibung: String = "", // Optionale Beschreibung
    val abholungDatum: Long = 0, // Startdatum für Abholung (0 = heute)
    val auslieferungDatum: Long = 0, // Startdatum für Auslieferung (0 = heute)
    val wiederholen: Boolean = false, // Ob das Intervall wiederholt wird
    val intervallTage: Int = 7, // Intervall in Tagen (1-365), nur relevant wenn wiederholen=true
    val intervallAnzahl: Int = 0, // Anzahl der Wiederholungen (0 = unbegrenzt), nur relevant wenn wiederholen=true
    val erstelltAm: Long = System.currentTimeMillis(),
    val geaendertAm: Long = System.currentTimeMillis(),
    val verwendungsanzahl: Int = 0, // Wie oft wurde diese Regel bereits verwendet
    
    // Wochentag-basierte Termine (NEU)
    val wochentagBasiert: Boolean = false, // true = Wochentag-basiert, false = Datum-basiert
    val startDatum: Long = 0, // Startdatum für Wochentag-Berechnung (0 = heute)
    val abholungWochentag: Int = -1, // 0=Montag, 1=Dienstag, ..., 6=Sonntag, -1=nicht gesetzt
    val auslieferungWochentag: Int = -1, // 0=Montag, 1=Dienstag, ..., 6=Sonntag, -1=nicht gesetzt
    val startWocheOption: String = "diese" // "diese" = diese Woche, "naechste" = nächste Woche
)
