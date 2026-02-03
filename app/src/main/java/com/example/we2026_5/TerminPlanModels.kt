package com.example.we2026_5

/**
 * Gemeinsame Modelle für Terminplanung, Touren und flexible Regeln.
 */

/** Regeltyp zur besseren Unterscheidung der Scheduler-Logik. */
enum class TerminRegelTyp {
    WEEKLY,
    FLEXIBLE_CYCLE,
    AD_HOC
}

/** Einfaches Zeitfenster (z. B. 09:00–13:00). */
data class Zeitfenster(
    val start: String = "",
    val ende: String = ""
)

/** Tour-Slot beschreibt, wann welche Stadt angefahren wird. */
data class TourSlot(
    val id: String = java.util.UUID.randomUUID().toString(),
    val wochentag: Int = -1,
    val stadt: String = "",
    val zeitfenster: Zeitfenster? = null
)

/** Ad-hoc-Template für Kunden mit unregelmäßigen Terminen. */
data class AdHocTemplate(
    val abholungWochentag: Int = -1,
    val auslieferungWochentag: Int = -1,
    val uhrzeit: String = ""
)

/** Kundenstatus zur Steuerung von Pause/Fortsetzen. */
enum class CustomerStatus {
    AKTIV,
    PAUSIERT,
    ADHOC
}

/** Historieneintrag für Regel-/Statuswechsel. */
data class RegelHistorieEintrag(
    val regelId: String = "",
    val aktion: String = "",
    val von: Long = 0L,
    val bis: Long = 0L
)

/** Slot-Vorschlag für manuelle Einzeltermin-Erstellung. */
data class TerminSlotVorschlag(
    val datum: Long,
    val typ: TerminTyp,
    val beschreibung: String = "",
    val tourSlotId: String? = null
)
