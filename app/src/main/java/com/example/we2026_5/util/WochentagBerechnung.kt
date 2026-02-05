package com.example.we2026_5.util

import java.util.Calendar

/**
 * Zentrale Wochentags-Berechnung (0=Mo … 6=So).
 * Nächstes Vorkommen eines Wochentags ab Startdatum.
 */
object WochentagBerechnung {

    fun isValidWeekday(wochentag: Int?): Boolean = wochentag != null && wochentag in 0..6

    /**
     * Berechnet das nächste Vorkommen eines Wochentags ab einem Startdatum (inkl. Starttag).
     * @param startDatum Zeitstempel (wird auf Tagesanfang normalisiert)
     * @param wochentag 0=Mo … 6=So
     */
    fun naechsterWochentagAb(startDatum: Long, wochentag: Int): Long {
        if (!isValidWeekday(wochentag)) {
            return TerminBerechnungUtils.getStartOfDay(startDatum)
        }
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDatum
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val aktuellerWochentagCalendar = calendar.get(Calendar.DAY_OF_WEEK)
        val zielWochentagCalendar = if (wochentag == 6) 1 else (wochentag + 2)
        val tageBisZiel = if (zielWochentagCalendar >= aktuellerWochentagCalendar) {
            zielWochentagCalendar - aktuellerWochentagCalendar
        } else {
            (zielWochentagCalendar - aktuellerWochentagCalendar) + 7
        }
        calendar.add(Calendar.DAY_OF_YEAR, tageBisZiel)
        return calendar.timeInMillis
    }
}
