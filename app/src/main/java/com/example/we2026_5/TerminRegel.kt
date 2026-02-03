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

    // erweiterte Planung
    val regelTyp: TerminRegelTyp = TerminRegelTyp.WEEKLY,
    val zyklusTage: Int = 7, // Freies Feld für 7, 14, 21, ...
    val defaultZeitfenster: Zeitfenster? = null,
    val pauseStart: Long = 0,
    val pauseEnde: Long = 0,
    val aktiv: Boolean = true,
    val tourSlotId: String = "",

    // Wochentag-basierte Termine: mehrere Abhol- und Auslieferungstage
    val wochentagBasiert: Boolean = false,
    val startDatum: Long = 0, // Startdatum für Berechnung (0 = heute)
    val abholungWochentag: Int = -1, // Legacy: einzelner Tag (0=Mo..6=So), -1=nicht gesetzt
    val auslieferungWochentag: Int = -1,
    val abholungWochentage: List<Int>? = null, // 0=Montag..6=Sonntag, mehrere erlaubt
    val auslieferungWochentage: List<Int>? = null,
    val startWocheOption: String = "diese",
    /** Täglich: Termine jeden Tag ab Startdatum (Abholung + Auslieferung am selben Tag). */
    val taeglich: Boolean = false,

    // Ad-hoc-spezifische Einstellungen
    val adHocTemplate: AdHocTemplate? = null,
    val historie: List<RegelHistorieEintrag> = emptyList()
)
