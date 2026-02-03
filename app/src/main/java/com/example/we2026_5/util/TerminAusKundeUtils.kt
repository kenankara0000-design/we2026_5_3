package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.TerminRegelTyp
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Erstellt ein CustomerIntervall aus Kundendaten (ohne TerminRegel).
 * Nutzt defaultAbholungWochentag, defaultAuslieferungWochentag, intervallTage.
 */
object TerminAusKundeUtils {

    private fun Int.isValidWeekday(): Boolean = this in 0..6

    private fun berechneNaechstenWochentag(startDatum: Long, wochentag: Int): Long {
        if (!wochentag.isValidWeekday()) {
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

    /**
     * Erstellt ein CustomerIntervall aus den Kundendaten.
     * @param customer Kunde mit defaultAbholungWochentag, defaultAuslieferungWochentag, intervallTage
     * @param startDatum Startdatum (z.B. heute)
     * @return CustomerIntervall oder null wenn A/L-Tage fehlen
     */
    fun erstelleIntervallAusKunde(customer: Customer, startDatum: Long = System.currentTimeMillis()): CustomerIntervall? {
        val abholTag = customer.defaultAbholungWochentag.takeIf { it.isValidWeekday() } ?: return null
        val auslieferTag = customer.defaultAuslieferungWochentag.takeIf { it.isValidWeekday() } ?: return null
        val zyklus = customer.intervallTage.coerceIn(1, 365)
        val start = TerminBerechnungUtils.getStartOfDay(startDatum)

        val abholungDatum = berechneNaechstenWochentag(start, abholTag)
        val auslieferungDatum = if (abholTag == auslieferTag) {
            abholungDatum + TimeUnit.DAYS.toMillis(zyklus.toLong())
        } else {
            berechneNaechstenWochentag(start, auslieferTag)
        }

        return CustomerIntervall(
            id = UUID.randomUUID().toString(),
            abholungDatum = abholungDatum,
            auslieferungDatum = auslieferungDatum,
            wiederholen = true,
            intervallTage = zyklus,
            intervallAnzahl = 0,
            erstelltAm = System.currentTimeMillis(),
            terminRegelId = "",
            regelTyp = TerminRegelTyp.WEEKLY,
            tourSlotId = customer.tourSlotId,
            zyklusTage = zyklus
        )
    }
}
