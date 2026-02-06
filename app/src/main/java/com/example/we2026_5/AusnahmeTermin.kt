package com.example.we2026_5

/**
 * Ein Ausnahme-Termin (einmalig A oder L an einem Datum).
 * Hat keinen Einfluss auf die regulären A/L-Termine.
 */
data class AusnahmeTermin(
    /** Tagesanfang (Start-of-day) des Termins. */
    val datum: Long = 0L,
    /** "A" = Abholung, "L" = Auslieferung. */
    val typ: String = "A"
)
