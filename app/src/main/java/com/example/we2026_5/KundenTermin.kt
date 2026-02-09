package com.example.we2026_5

/**
 * Ein Kunden-Termin: vom Kunden vorgegebener Abholungstermin (A) oder Liefertermin (L) an einem Datum.
 * L wird in der Regel aus A + tageAzuL berechnet und automatisch angelegt.
 * Getrennt von Ausnahme-Terminen (einmalige Sonderfälle).
 */
data class KundenTermin(
    /** Tagesanfang (Start-of-day) des Termins. */
    val datum: Long = 0L,
    /** "A" = Abholung, "L" = Auslieferung. */
    val typ: String = "A"
)
