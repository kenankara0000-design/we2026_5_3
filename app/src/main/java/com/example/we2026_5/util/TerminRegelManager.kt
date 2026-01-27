package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.TerminRegel
import java.util.Calendar

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
        
        // Startdatum für Abholung berechnen
        val abholungDatum = if (regel.abholungDatum > 0) {
            regel.abholungDatum
        } else {
            // Wenn 0, dann heute
            getStartOfDay(heute)
        }
        
        // Startdatum für Auslieferung berechnen
        val auslieferungDatum = if (regel.auslieferungDatum > 0) {
            regel.auslieferungDatum
        } else {
            // Wenn 0, dann heute
            getStartOfDay(heute)
        }
        
        return CustomerIntervall(
            id = java.util.UUID.randomUUID().toString(),
            abholungDatum = abholungDatum,
            auslieferungDatum = auslieferungDatum,
            wiederholen = regel.wiederholen,
            intervallTage = regel.intervallTage,
            intervallAnzahl = regel.intervallAnzahl,
            erstelltAm = heute
        )
    }
    
    /**
     * Wendet eine Regel auf eine Liste an
     * Erstellt ein ListeIntervall basierend auf der Regel
     */
    fun wendeRegelAufListeAn(regel: TerminRegel, liste: KundenListe): ListeIntervall {
        val heute = System.currentTimeMillis()
        
        // Startdatum für Abholung berechnen
        val abholungDatum = if (regel.abholungDatum > 0) {
            regel.abholungDatum
        } else {
            // Wenn 0, dann heute
            getStartOfDay(heute)
        }
        
        // Startdatum für Auslieferung berechnen
        val auslieferungDatum = if (regel.auslieferungDatum > 0) {
            regel.auslieferungDatum
        } else {
            // Wenn 0, dann heute
            getStartOfDay(heute)
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
     * Hilfsfunktion: Start des Tages (00:00:00)
     */
    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
