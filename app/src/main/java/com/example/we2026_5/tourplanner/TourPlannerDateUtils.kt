package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils
import com.example.we2026_5.util.tageAzuLOrDefault
import java.util.concurrent.TimeUnit

/**
 * Helper-Klasse für Datum-Berechnungen in der TourPlannerActivity.
 * getListen liefert Listen aus dem ViewModel (ohne runBlocking).
 */
class TourPlannerDateUtils(
    private val getListen: () -> List<KundenListe>
) {
    /** L = A + tageAzuL. Zentrale Abstraktion (TerminAusKundeUtils). */
    private fun getTageAzuL(customer: Customer): Int = customer.tageAzuLOrDefault(7)

    /** Delegiert an TerminBerechnungUtils (Single Source of Truth). */
    fun getStartOfDay(ts: Long): Long = TerminBerechnungUtils.getStartOfDay(ts)
    
    fun calculateAbholungDatum(
        customer: Customer,
        viewDateStart: Long,
        heuteStart: Long
    ): Long {
        // Term-Daten nur aus Kunde (liste nur Gruppierung; kein liste.intervalle).
        val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
            customer = customer,
            liste = null,
            startDatum = viewDateStart,
            tageVoraus = 1
        )
        val resultA = termine.firstOrNull {
            it.typ == TerminTyp.ABHOLUNG &&
            TerminBerechnungUtils.getStartOfDay(it.datum) == viewDateStart
        }?.datum ?: 0L
        if (resultA > 0L) return resultA
        return 0L
    }
    
    fun calculateAuslieferungDatum(
        customer: Customer,
        viewDateStart: Long,
        heuteStart: Long
    ): Long {
        // Term-Daten nur aus Kunde (liste nur Gruppierung; kein liste.intervalle).
        val tageAzuL = getTageAzuL(customer)
        val aDatumStart = viewDateStart - TimeUnit.DAYS.toMillis(tageAzuL.toLong())
        val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
            customer = customer,
            liste = null,
            startDatum = aDatumStart,
            tageVoraus = 1
        )
        val aTermin = termine.firstOrNull {
            it.typ == TerminTyp.ABHOLUNG &&
            TerminBerechnungUtils.getStartOfDay(it.datum) == aDatumStart
        }
        if (aTermin != null)
            return aTermin.datum + TimeUnit.DAYS.toMillis(tageAzuL.toLong())
        return 0L
    }
    
    fun isIntervallFaelligAm(intervall: ListeIntervall, datum: Long): Boolean {
        val intervallStart = getStartOfDay(intervall.abholungDatum)
        val datumStart = getStartOfDay(datum)
        
        if (!intervall.wiederholen) {
            return intervallStart == datumStart
        }
        
        val intervallTage = intervall.intervallTage.coerceIn(1, 365)
        val intervallTageLong = intervallTage.toLong()
        
        if (datumStart >= intervallStart) {
            val tageSeitStart = TimeUnit.MILLISECONDS.toDays(datumStart - intervallStart)
            val zyklus = tageSeitStart / intervallTageLong
            val erwartetesDatum = intervallStart + TimeUnit.DAYS.toMillis(zyklus * intervallTageLong)
            val erwartetesDatumStart = getStartOfDay(erwartetesDatum)
            
            return erwartetesDatumStart == datumStart && tageSeitStart <= 365
        }
        
        return false
    }
    
    fun getFaelligAmDatumFuerAbholung(customer: Customer, heuteStart: Long): Long {
        // Prüfe ob Kunde überfällig ist
        val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
            customer = customer,
            startDatum = heuteStart - TimeUnit.DAYS.toMillis(365),
            tageVoraus = 730
        )
        
        // Finde den ersten überfälligen Abholungstermin
        val ueberfaelligeAbholung = termine.firstOrNull { termin ->
            termin.typ == com.example.we2026_5.TerminTyp.ABHOLUNG &&
            TerminFilterUtils.istUeberfaellig(termin.datum, heuteStart, customer.abholungErfolgt)
        }
        
        return ueberfaelligeAbholung?.datum ?: 0L
    }
    
    fun getFaelligAmDatumFuerAuslieferung(customer: Customer, heuteStart: Long): Long {
        // Prüfe ob Kunde überfällig ist
        val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
            customer = customer,
            startDatum = heuteStart - TimeUnit.DAYS.toMillis(365),
            tageVoraus = 730
        )
        
        // Finde den ersten überfälligen Auslieferungstermin
        val ueberfaelligeAuslieferung = termine.firstOrNull { termin ->
            termin.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG &&
            TerminFilterUtils.istUeberfaellig(termin.datum, heuteStart, customer.auslieferungErfolgt)
        }
        
        return ueberfaelligeAuslieferung?.datum ?: 0L
    }
    
    /**
     * Nächstes Tour-Datum für einen Kunden (für "Nächste Tour" auf der Karte).
     * Term-Daten nur aus Kunde (liste nur Gruppierung).
     * Überspringt Termine während des Urlaubs – zeigt erst das Datum nach dem Urlaub.
     */
    fun getNaechstesTourDatum(customer: Customer): Long {
        val heuteStart = getStartOfDay(System.currentTimeMillis())
        val geloeschte = customer.geloeschteTermine
        val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
            customer = customer,
            liste = null,
            startDatum = heuteStart,
            tageVoraus = 365
        )
        val naechstes = termine.firstOrNull {
            it.datum >= heuteStart &&
            !TerminFilterUtils.istTerminGeloescht(it.datum, geloeschte) &&
            !TerminFilterUtils.istTerminInUrlaubEintraege(it.datum, customer)
        }
        return naechstes?.datum ?: 0L
    }
}
