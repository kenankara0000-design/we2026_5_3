package com.example.we2026_5

/**
 * Repräsentiert einen verschobenen Termin
 * Speichert das ursprüngliche Datum und das neue Datum
 */
data class VerschobenerTermin(
    val originalDatum: Long, // Ursprüngliches Termin-Datum
    val verschobenAufDatum: Long, // Neues Datum
    val intervallId: String? = null, // ID des Intervalls (null = alle Intervalle)
    val typ: TerminTyp = TerminTyp.ABHOLUNG // Abholung oder Auslieferung
)

enum class TerminTyp {
    ABHOLUNG, // Abholungstermin
    AUSLIEFERUNG // Auslieferungstermin
}
