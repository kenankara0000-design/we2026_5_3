package com.example.we2026_5.util

import java.util.Calendar
import java.util.TimeZone

/**
 * Zentrale Zeitzone für alle Termin- und Datumslogik in der App.
 * Weltweit wird immer Europe/Berlin genutzt – unabhängig vom Standort des Geräts.
 */
object AppTimeZone {
    /** Zeitzone Europe/Berlin für Termine/Datum (Speicherung, Interpretation, Anzeige). */
    val timeZone: TimeZone = TimeZone.getTimeZone("Europe/Berlin")

    /** Erzeugt einen Calendar in App-Zeitzone (Berlin). Für Termin-/Datumslogik verwenden. */
    fun newCalendar(): Calendar = Calendar.getInstance(timeZone)
}
