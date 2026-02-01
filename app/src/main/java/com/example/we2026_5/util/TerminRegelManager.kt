package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.TerminRegel
import java.util.Calendar
import com.example.we2026_5.util.TerminBerechnungUtils

/**
 * Manager für die Anwendung von Termin-Regeln auf Kunden und Listen.
 * Unterstützt mehrere Abhol- und Auslieferungswochentage (paarweise).
 */
object TerminRegelManager {

    private fun getAbholungWochentage(regel: TerminRegel): List<Int> {
        val list = regel.abholungWochentage?.filter { it in 0..6 }?.distinct()?.sorted()
        if (!list.isNullOrEmpty()) return list
        if (regel.abholungWochentag in 0..6) return listOf(regel.abholungWochentag)
        return emptyList()
    }

    private fun getAuslieferungWochentage(regel: TerminRegel): List<Int> {
        val list = regel.auslieferungWochentage?.filter { it in 0..6 }?.distinct()?.sorted()
        if (!list.isNullOrEmpty()) return list
        if (regel.auslieferungWochentag in 0..6) return listOf(regel.auslieferungWochentag)
        return emptyList()
    }
    
    private const val TAEGLICH_ANZAHL_TAGE = 365

    /**
     * Täglich: Erstellt pro Tag ab Startdatum ein Intervall.
     * - Termine beginnen mit dem Startdatum (erster Termin = erster Tag).
     * - Abholung und Auslieferung sind am selben Tag (abholungDatum = auslieferungDatum = Tag-Start).
     * - Die Erledigung erlaubt Auslieferung nur, wenn Abholung gemacht wurde (siehe CustomerButtonVisibilityHelper / ErledigungSheet).
     */
    private fun wendeTaeglichAufKundeAn(regel: TerminRegel): List<CustomerIntervall> {
        val heute = System.currentTimeMillis()
        val startDatum = if (regel.startDatum > 0) regel.startDatum else TerminBerechnungUtils.getStartOfDay(heute)
        val cal = Calendar.getInstance()
        cal.timeInMillis = startDatum
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return (0 until TAEGLICH_ANZAHL_TAGE).map {
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            CustomerIntervall(
                id = java.util.UUID.randomUUID().toString(),
                abholungDatum = dayStart,
                auslieferungDatum = dayStart,
                wiederholen = false,
                intervallTage = 0,
                intervallAnzahl = 0,
                erstelltAm = heute,
                terminRegelId = regel.id
            )
        }
    }

    /**
     * Wendet eine Regel auf einen Kunden an.
     * Erstellt pro (Abhol-Wochentag, Auslieferungs-Wochentag)-Paar ein CustomerIntervall.
     */
    fun wendeRegelAufKundeAn(regel: TerminRegel, customer: Customer): List<CustomerIntervall> {
        if (regel.taeglich) return wendeTaeglichAufKundeAn(regel)

        val heute = System.currentTimeMillis()
        val abholTage = getAbholungWochentage(regel)
        val auslTage = getAuslieferungWochentage(regel)

        if (abholTage.isEmpty() || auslTage.isEmpty()) {
            // Fallback: Datum-basiert oder einzelne Legacy-Felder
            val abholungDatum = if (regel.abholungDatum > 0) regel.abholungDatum else TerminBerechnungUtils.getStartOfDay(heute)
            val auslieferungDatum = if (regel.auslieferungDatum > 0) regel.auslieferungDatum else TerminBerechnungUtils.getStartOfDay(heute)
            return listOf(CustomerIntervall(
                id = java.util.UUID.randomUUID().toString(),
                abholungDatum = abholungDatum,
                auslieferungDatum = auslieferungDatum,
                wiederholen = regel.wiederholen,
                intervallTage = regel.intervallTage,
                intervallAnzahl = regel.intervallAnzahl,
                erstelltAm = heute,
                terminRegelId = regel.id
            ))
        }

        val startDatum = if (regel.startDatum > 0) regel.startDatum else TerminBerechnungUtils.getStartOfDay(heute)
        val count = minOf(abholTage.size, auslTage.size)
        return (0 until count).map { i ->
            val abholungDatum = berechneNaechstenWochentag(startDatum, abholTage[i])
            val auslieferungDatum = if (abholTage[i] == auslTage[i]) {
                // Gleicher Wochentag (z.B. MO+MO): erste Auslieferung 7 Tage nach Abholung
                abholungDatum + 7 * 24 * 60 * 60 * 1000L
            } else {
                berechneNaechstenWochentag(startDatum, auslTage[i])
            }
            CustomerIntervall(
                id = java.util.UUID.randomUUID().toString(),
                abholungDatum = abholungDatum,
                auslieferungDatum = auslieferungDatum,
                wiederholen = regel.wiederholen,
                intervallTage = regel.intervallTage,
                intervallAnzahl = regel.intervallAnzahl,
                erstelltAm = heute,
                terminRegelId = regel.id
            )
        }
    }
    
    /**
     * Täglich: Pro Tag ab Startdatum ein ListeIntervall; Abholung und Auslieferung am selben Tag.
     * Auslieferung nur nach Abholung (wird in der Erledigung berücksichtigt).
     */
    private fun wendeTaeglichAufListeAn(regel: TerminRegel): List<ListeIntervall> {
        val heute = System.currentTimeMillis()
        val startDatum = if (regel.startDatum > 0) regel.startDatum else TerminBerechnungUtils.getStartOfDay(heute)
        val cal = Calendar.getInstance()
        cal.timeInMillis = startDatum
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return (0 until TAEGLICH_ANZAHL_TAGE).map {
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            ListeIntervall(
                abholungDatum = dayStart,
                auslieferungDatum = dayStart,
                wiederholen = false,
                intervallTage = 0,
                intervallAnzahl = 0
            )
        }
    }

    /**
     * Wendet eine Regel auf eine Liste an.
     * Erstellt pro (Abhol-, Auslieferungs-)Wochentag-Paar ein ListeIntervall.
     */
    fun wendeRegelAufListeAn(regel: TerminRegel, liste: KundenListe): List<ListeIntervall> {
        if (regel.taeglich) return wendeTaeglichAufListeAn(regel)

        val heute = System.currentTimeMillis()
        val abholTage = getAbholungWochentage(regel)
        val auslTage = getAuslieferungWochentage(regel)

        if (abholTage.isEmpty() || auslTage.isEmpty()) {
            val abholungDatum = if (regel.abholungDatum > 0) regel.abholungDatum else TerminBerechnungUtils.getStartOfDay(heute)
            val auslieferungDatum = if (regel.auslieferungDatum > 0) regel.auslieferungDatum else TerminBerechnungUtils.getStartOfDay(heute)
            return listOf(ListeIntervall(
                abholungDatum = abholungDatum,
                auslieferungDatum = auslieferungDatum,
                wiederholen = regel.wiederholen,
                intervallTage = regel.intervallTage,
                intervallAnzahl = regel.intervallAnzahl
            ))
        }

        val startDatum = if (regel.startDatum > 0) regel.startDatum else TerminBerechnungUtils.getStartOfDay(heute)
        val count = minOf(abholTage.size, auslTage.size)
        return (0 until count).map { i ->
            val abholungDatum = berechneNaechstenWochentag(startDatum, abholTage[i])
            val auslieferungDatum = if (abholTage[i] == auslTage[i]) {
                // Gleicher Wochentag (z.B. MO+MO): erste Auslieferung 7 Tage nach Abholung
                abholungDatum + 7 * 24 * 60 * 60 * 1000L
            } else {
                berechneNaechstenWochentag(startDatum, auslTage[i])
            }
            ListeIntervall(
                abholungDatum = abholungDatum,
                auslieferungDatum = auslieferungDatum,
                wiederholen = regel.wiederholen,
                intervallTage = regel.intervallTage,
                intervallAnzahl = regel.intervallAnzahl
            )
        }
    }
    
    /**
     * Berechnet das nächste Vorkommen eines Wochentags ab einem Startdatum
     * Automatische Berechnung: Nächstes Vorkommen ab Startdatum (kann heute sein, wenn Startdatum heute ist)
     * @param startDatum Startdatum für die Berechnung
     * @param wochentag Wochentag (0=Montag, 1=Dienstag, ..., 6=Sonntag)
     * @return Timestamp des nächsten Wochentags ab Startdatum
     */
    private fun berechneNaechstenWochentag(
        startDatum: Long,
        wochentag: Int
    ): Long {
        if (wochentag < 0 || wochentag > 6) {
            return TerminBerechnungUtils.getStartOfDay(startDatum)
        }
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDatum
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Calendar.DAY_OF_WEEK: 1=Sonntag, 2=Montag, ..., 7=Samstag
        // Unser System: 0=Montag, 1=Dienstag, ..., 6=Sonntag
        val aktuellerWochentagCalendar = calendar.get(Calendar.DAY_OF_WEEK)
        
        // Ziel-Wochentag zu Calendar-Format: 0(Montag)->2, 1(Dienstag)->3, ..., 6(Sonntag)->1
        val zielWochentagCalendar = if (wochentag == 6) 1 else (wochentag + 2)
        
        // Berechne Tage bis zum Ziel-Wochentag (nächstes Vorkommen ab Startdatum)
        val tageBisZiel = if (zielWochentagCalendar >= aktuellerWochentagCalendar) {
            // Ziel-Wochentag ist noch diese Woche
            zielWochentagCalendar - aktuellerWochentagCalendar
        } else {
            // Ziel-Wochentag ist nächste Woche
            (zielWochentagCalendar - aktuellerWochentagCalendar) + 7
        }
        
        calendar.add(Calendar.DAY_OF_YEAR, tageBisZiel)
        return calendar.timeInMillis
    }
}
