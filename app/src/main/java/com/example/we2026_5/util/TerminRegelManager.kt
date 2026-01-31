package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.TerminRegel
import java.util.Calendar
import com.example.we2026_5.util.TerminBerechnungUtils

/**
 * Manager für die Anwendung von Termin-Regeln auf Kunden und Listen
 */
object TerminRegelManager {
    
    /**
     * Wendet eine Regel auf einen Kunden an
     * Erstellt ein CustomerIntervall basierend auf der Regel
     */
    fun wendeRegelAufKundeAn(regel: TerminRegel, customer: Customer): CustomerIntervall {
        val heute = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        val abholungDatum: Long
        val auslieferungDatum: Long
        
        if (regel.wochentagBasiert) {
            // Wochentag-basierte Berechnung
            val startDatum = if (regel.startDatum > 0) {
                regel.startDatum
            } else {
                TerminBerechnungUtils.getStartOfDay(heute)
            }
            
            abholungDatum = berechneNaechstenWochentag(
                startDatum = startDatum,
                wochentag = regel.abholungWochentag
            )
            
            auslieferungDatum = berechneNaechstenWochentag(
                startDatum = startDatum,
                wochentag = regel.auslieferungWochentag
            )
        } else {
            // Datum-basierte Berechnung (wie bisher)
            abholungDatum = if (regel.abholungDatum > 0) {
                regel.abholungDatum
            } else {
                TerminBerechnungUtils.getStartOfDay(heute)
            }
            
            auslieferungDatum = if (regel.auslieferungDatum > 0) {
                regel.auslieferungDatum
            } else {
                TerminBerechnungUtils.getStartOfDay(heute)
            }
        }
        
        return CustomerIntervall(
            id = java.util.UUID.randomUUID().toString(),
            abholungDatum = abholungDatum,
            auslieferungDatum = auslieferungDatum,
            wiederholen = regel.wiederholen,
            intervallTage = regel.intervallTage,
            intervallAnzahl = regel.intervallAnzahl,
            erstelltAm = heute,
            terminRegelId = regel.id // Regel-ID speichern
        )
    }
    
    /**
     * Wendet eine Regel auf eine Liste an
     * Erstellt ein ListeIntervall basierend auf der Regel
     */
    fun wendeRegelAufListeAn(regel: TerminRegel, liste: KundenListe): ListeIntervall {
        val heute = System.currentTimeMillis()
        
        val abholungDatum: Long
        val auslieferungDatum: Long
        
        if (regel.wochentagBasiert) {
            // Wochentag-basierte Berechnung
            val startDatum = if (regel.startDatum > 0) {
                regel.startDatum
            } else {
                TerminBerechnungUtils.getStartOfDay(heute)
            }
            
            abholungDatum = berechneNaechstenWochentag(
                startDatum = startDatum,
                wochentag = regel.abholungWochentag
            )
            
            auslieferungDatum = berechneNaechstenWochentag(
                startDatum = startDatum,
                wochentag = regel.auslieferungWochentag
            )
        } else {
            // Datum-basierte Berechnung (wie bisher)
            abholungDatum = if (regel.abholungDatum > 0) {
                regel.abholungDatum
            } else {
                TerminBerechnungUtils.getStartOfDay(heute)
            }
            
            auslieferungDatum = if (regel.auslieferungDatum > 0) {
                regel.auslieferungDatum
            } else {
                TerminBerechnungUtils.getStartOfDay(heute)
            }
        }
        
        return ListeIntervall(
            abholungDatum = abholungDatum,
            auslieferungDatum = auslieferungDatum,
            wiederholen = regel.wiederholen,
            intervallTage = regel.intervallTage,
            intervallAnzahl = regel.intervallAnzahl
        )
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
