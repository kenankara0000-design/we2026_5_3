package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils
import com.example.we2026_5.util.firstIntervallOrNull
import com.example.we2026_5.util.tageAzuLOrDefault
import java.util.concurrent.TimeUnit

/**
 * Helper-Klasse für Filter-Logik in TourDataProcessor.
 * Extrahiert alle Filter-Funktionen aus TourDataProcessor.
 */
class TourDataFilter(
    private val categorizer: TourDataCategorizer
) {
    /** L = A + tageAzuL. Zentrale Abstraktion (TerminAusKundeUtils). */
    private fun getTageAzuL(customer: Customer): Int = customer.tageAzuLOrDefault(7)

    /**
     * Berechnet, wann ein Kunde fällig ist.
     */
    fun customerFaelligAm(c: Customer, liste: KundenListe? = null, abDatum: Long = System.currentTimeMillis()): Long {
        // Term-Daten nur aus Kunde (liste nur Gruppierung; kein getNaechstesListeDatum(liste)).
        // Reduziertes Fenster: 3 Tage für Tour-Planner-Performance
        if (c.firstIntervallOrNull() != null || c.listeId.isNotEmpty()) {
            val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = c,
                liste = null,
                startDatum = abDatum,
                tageVoraus = 3
            )
            val naechstesTermin = termine.firstOrNull {
                it.datum >= categorizer.getStartOfDay(abDatum)
            }
            return naechstesTermin?.datum ?: 0L
        }
        return 0L
    }
    
    /**
     * Prüft, ob ein Kunde am angegebenen Datum einen Termin hat.
     */
    fun hatKundeTerminAmDatum(
        customer: Customer,
        liste: KundenListe? = null,
        viewDateStart: Long
    ): Boolean {
        // Ausnahme-Termine: Kunde hat an diesem Tag einen A oder L Ausnahme-Termin
        if (customer.ausnahmeTermine.any { TerminBerechnungUtils.getStartOfDay(it.datum) == viewDateStart }) {
            return true
        }
        // Term-Daten nur aus Kunde (liste nur Gruppierung).
        if (customer.firstIntervallOrNull() != null || customer.listeId.isNotEmpty()) {
            val tageAzuL = getTageAzuL(customer)
            val startDatum = viewDateStart - TimeUnit.DAYS.toMillis(tageAzuL.toLong())
            val tageVoraus = tageAzuL + 2
            val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                liste = null,
                startDatum = startDatum,
                tageVoraus = tageVoraus
            )
            val alleGeloeschteTermine = customer.geloeschteTermine
            return termine.any { termin ->
                val terminStart = TerminBerechnungUtils.getStartOfDay(termin.datum)
                terminStart == viewDateStart &&
                !TerminFilterUtils.istTerminGeloescht(termin.datum, alleGeloeschteTermine)
            }
        }
        
        return false
    }
    
    /**
     * Prüft, ob ein Kunde überfällig ist.
     */
    fun istKundeUeberfaellig(
        customer: Customer,
        liste: KundenListe? = null,
        viewDateStart: Long,
        heuteStart: Long
    ): Boolean {
        val customerDone = customer.abholungErfolgt || customer.auslieferungErfolgt
        val isDone = customerDone
        if (isDone) return false
        
        // Term-Daten nur aus Kunde (liste nur Gruppierung).
        if (customer.firstIntervallOrNull() != null || customer.listeId.isNotEmpty()) {
            if (viewDateStart > heuteStart) {
                return false
            }
            
            // 3-Tage-Fenster + 60 Tage Überfällig (PLAN_TOURPLANNER_PERFORMANCE_3TAGE)
            val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                liste = null,
                startDatum = heuteStart - TimeUnit.DAYS.toMillis(60),
                tageVoraus = 63
            )
            
            return termine.any { termin ->
                val terminStart = categorizer.getStartOfDay(termin.datum)
                val istUeberfaellig = terminStart < heuteStart
                if (terminStart == viewDateStart) return@any false
                val sollAnzeigen = TerminFilterUtils.sollUeberfaelligAnzeigen(
                    terminDatum = termin.datum,
                    anzeigeDatum = viewDateStart,
                    aktuellesDatum = heuteStart
                )
                istUeberfaellig && sollAnzeigen
            }
        }
        return false
    }
    
    /**
     * Prüft, ob ein Intervall am angegebenen Datum fällig ist.
     */
    fun isIntervallFaelligAm(intervall: ListeIntervall, datum: Long): Boolean {
        val datumStart = categorizer.getStartOfDay(datum)
        val abholungStart = categorizer.getStartOfDay(intervall.abholungDatum)
        val auslieferungStart = categorizer.getStartOfDay(intervall.auslieferungDatum)
        
        if (!intervall.wiederholen) {
            return datumStart == abholungStart || datumStart == auslieferungStart
        }
        
        val intervallTage = intervall.intervallTage.coerceIn(1, 365)
        
        if (datumStart >= abholungStart) {
            val tageSeitAbholung = TimeUnit.MILLISECONDS.toDays(datumStart - abholungStart)
            if (tageSeitAbholung <= 365 && tageSeitAbholung % intervallTage == 0L) {
                val erwartetesDatum = abholungStart + TimeUnit.DAYS.toMillis(tageSeitAbholung)
                if (datumStart == erwartetesDatum) {
                    return true
                }
            }
        } else {
            val tageBisAbholung = TimeUnit.MILLISECONDS.toDays(abholungStart - datumStart)
            if (tageBisAbholung <= 365 && datumStart == abholungStart) {
                return true
            }
        }
        
        if (datumStart >= auslieferungStart) {
            val tageSeitAuslieferung = TimeUnit.MILLISECONDS.toDays(datumStart - auslieferungStart)
            if (tageSeitAuslieferung <= 365 && tageSeitAuslieferung % intervallTage == 0L) {
                val erwartetesDatum = auslieferungStart + TimeUnit.DAYS.toMillis(tageSeitAuslieferung)
                if (datumStart == erwartetesDatum) {
                    return true
                }
            }
        } else {
            val tageBisAuslieferung = TimeUnit.MILLISECONDS.toDays(auslieferungStart - datumStart)
            if (tageBisAuslieferung <= 365 && datumStart == auslieferungStart) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Prüft, ob ein Intervall in Zukunft fällig ist.
     */
    fun isIntervallFaelligInZukunft(intervall: ListeIntervall, abDatum: Long): Boolean {
        val abDatumStart = categorizer.getStartOfDay(abDatum)
        val maxZukunft = abDatumStart + TimeUnit.DAYS.toMillis(365)
        
        if (!intervall.wiederholen) {
            val abholungStart = categorizer.getStartOfDay(intervall.abholungDatum)
            val auslieferungStart = categorizer.getStartOfDay(intervall.auslieferungDatum)
            return (abholungStart >= abDatumStart && abholungStart <= maxZukunft) ||
                   (auslieferungStart >= abDatumStart && auslieferungStart <= maxZukunft)
        }
        
        val intervallTage = intervall.intervallTage.coerceIn(1, 365)
        val abholungStart = categorizer.getStartOfDay(intervall.abholungDatum)
        val auslieferungStart = categorizer.getStartOfDay(intervall.auslieferungDatum)
        
        if (abDatumStart <= maxZukunft) {
            var naechsteAbholung = if (abDatumStart >= abholungStart) {
                val tageSeitAbholung = TimeUnit.MILLISECONDS.toDays(abDatumStart - abholungStart)
                val naechsterZyklus = ((tageSeitAbholung / intervallTage) + 1) * intervallTage
                abholungStart + TimeUnit.DAYS.toMillis(naechsterZyklus)
            } else {
                abholungStart
            }
            
            if (naechsteAbholung <= maxZukunft) {
                return true
            }
        }
        
        if (abDatumStart <= maxZukunft) {
            var naechsteAuslieferung = if (abDatumStart >= auslieferungStart) {
                val tageSeitAuslieferung = TimeUnit.MILLISECONDS.toDays(abDatumStart - auslieferungStart)
                val naechsterZyklus = ((tageSeitAuslieferung / intervallTage) + 1) * intervallTage
                auslieferungStart + TimeUnit.DAYS.toMillis(naechsterZyklus)
            } else {
                auslieferungStart
            }
            
            if (naechsteAuslieferung <= maxZukunft) {
                return true
            }
        }
        
        return false
    }
}