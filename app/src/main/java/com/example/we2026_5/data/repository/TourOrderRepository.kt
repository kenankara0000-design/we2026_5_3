package com.example.we2026_5.data.repository

/**
 * Speichert die manuelle Tour-Reihenfolge (Kunden-IDs).
 * API: dateKey = yyyyMMdd. Firebase-Impl speichert pro Wochentag (gleiche Order für jeden Montag etc.).
 */
interface TourOrderRepository {
    /** Reihenfolge für ein Datum lesen (yyyyMMdd). Leere Liste = keine gespeicherte Reihenfolge. */
    fun getOrderForDate(dateKey: String): List<String>

    /** Reihenfolge für ein Datum speichern. */
    fun setOrderForDate(dateKey: String, customerIds: List<String>)
}
