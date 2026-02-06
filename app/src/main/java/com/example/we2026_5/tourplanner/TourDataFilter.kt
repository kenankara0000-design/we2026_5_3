package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils
import java.util.concurrent.TimeUnit

/**
 * Helper-Klasse für Filter-Logik in TourDataProcessor.
 * Extrahiert alle Filter-Funktionen aus TourDataProcessor.
 */
class TourDataFilter(
    private val categorizer: TourDataCategorizer
) {
    /** L = A + tageAzuL. Aus erstem Intervall oder Default 7. */
    private fun getTageAzuL(customer: Customer): Int =
        customer.intervalle.firstOrNull()?.let {
            if (it.abholungDatum > 0 && it.auslieferungDatum > 0)
                TimeUnit.MILLISECONDS.toDays(it.auslieferungDatum - it.abholungDatum).toInt().coerceIn(0, 365)
            else null
        } ?: 7

    /**
     * Berechnet, wann ein Kunde fällig ist.
     */
    fun customerFaelligAm(c: Customer, liste: KundenListe? = null, abDatum: Long = System.currentTimeMillis()): Long {
        // Term-Daten nur aus Kunde (liste nur Gruppierung; kein getNaechstesListeDatum(liste)).
        if (c.intervalle.isNotEmpty() || c.listeId.isNotEmpty()) {
            val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = c,
                liste = null,
                startDatum = abDatum,
                tageVoraus = 365
            )
            val naechstesTermin = termine.firstOrNull {
                it.datum >= categorizer.getStartOfDay(abDatum)
            }
            return naechstesTermin?.datum ?: 0L
        }
        
        // Für Kunden ohne Intervalle und ohne listeId: Alte Einzel-Felder
        if (!c.wiederholen) {
            val abholungStart = categorizer.getStartOfDay(c.abholungDatum)
            val auslieferungStart = categorizer.getStartOfDay(c.auslieferungDatum)
            val abDatumStart = categorizer.getStartOfDay(abDatum)
            
            val verschobenEntry = TerminFilterUtils.istTerminVerschoben(c.abholungDatum, c.verschobeneTermine)
            if (verschobenEntry != null) {
                val verschobenStart = TerminBerechnungUtils.getStartOfDay(verschobenEntry.verschobenAufDatum)
                if (c.geloeschteTermine.contains(verschobenStart)) {
                    return 0L
                }
                if (abDatumStart == verschobenStart) return verschobenEntry.verschobenAufDatum
                if (abDatumStart < verschobenStart) return verschobenEntry.verschobenAufDatum
                return 0L
            }
            
            val abholungGeloescht = c.geloeschteTermine.contains(abholungStart)
            val auslieferungGeloescht = c.geloeschteTermine.contains(auslieferungStart)
            
            if (abholungGeloescht && auslieferungGeloescht) return 0L
            
            if (abDatumStart == abholungStart && !abholungGeloescht) {
                return c.abholungDatum
            }
            
            if (abDatumStart == auslieferungStart && !auslieferungGeloescht) {
                return c.auslieferungDatum
            }
            
            if (abDatumStart > abholungStart && abDatumStart < auslieferungStart) {
                return if (!auslieferungGeloescht) c.auslieferungDatum else 0L
            }
            if (abDatumStart > auslieferungStart && abDatumStart < abholungStart) {
                return if (!abholungGeloescht) c.abholungDatum else 0L
            }
            
            if (abDatumStart < abholungStart && abDatumStart < auslieferungStart) {
                return if (!abholungGeloescht) c.abholungDatum else 
                       if (!auslieferungGeloescht) c.auslieferungDatum else 0L
            }
            
            if (abDatumStart > abholungStart && abDatumStart > auslieferungStart) {
                return 0L
            }
            
            if (!abholungGeloescht && !auslieferungGeloescht) {
                return minOf(c.abholungDatum, c.auslieferungDatum)
            }
            if (!abholungGeloescht) return c.abholungDatum
            if (!auslieferungGeloescht) return c.auslieferungDatum
            return 0L
        }
        
        // Wiederholender Termin: Alte Logik
        val faelligAm = c.getFaelligAm()
        val faelligAmStart = categorizer.getStartOfDay(faelligAm)
        if (c.geloeschteTermine.contains(faelligAmStart)) {
            if (c.wiederholen && c.letzterTermin > 0) {
                return c.letzterTermin + TimeUnit.DAYS.toMillis(c.intervallTage.toLong())
            }
            return faelligAm + TimeUnit.DAYS.toMillis(1)
        }
        return faelligAm
    }
    
    /**
     * Prüft, ob ein Kunde am angegebenen Datum einen Termin hat.
     */
    fun hatKundeTerminAmDatum(
        customer: Customer,
        liste: KundenListe? = null,
        viewDateStart: Long
    ): Boolean {
        // Term-Daten nur aus Kunde (liste nur Gruppierung).
        if (customer.intervalle.isNotEmpty() || customer.listeId.isNotEmpty()) {
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
        
        // ALTE STRUKTUR: Rückwärtskompatibilität
        val faelligAm = customerFaelligAm(customer, null, viewDateStart)
        val faelligAmStart = categorizer.getStartOfDay(faelligAm)
        return faelligAmStart == viewDateStart && faelligAm > 0
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
        if (customer.intervalle.isNotEmpty() || customer.listeId.isNotEmpty()) {
            if (viewDateStart > heuteStart) {
                return false
            }
            
            val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                liste = null,
                startDatum = heuteStart - TimeUnit.DAYS.toMillis(365),
                tageVoraus = 730
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
        
        // ALTE STRUKTUR: Rückwärtskompatibilität
        val faelligAm = customerFaelligAm(customer, null, viewDateStart)
        val abholungStart = categorizer.getStartOfDay(customer.abholungDatum)
        val auslieferungStart = categorizer.getStartOfDay(customer.auslieferungDatum)
        val abholungUeberfaellig = !customer.abholungErfolgt && customer.abholungDatum > 0 && 
                                   abholungStart < heuteStart
        val auslieferungUeberfaellig = !customer.auslieferungErfolgt && customer.auslieferungDatum > 0 && 
                                      auslieferungStart < heuteStart
        val wiederholendUeberfaellig = customer.wiederholen && faelligAm < heuteStart && faelligAm > 0
        
        return (abholungUeberfaellig && viewDateStart >= abholungStart && viewDateStart <= heuteStart) ||
               (auslieferungUeberfaellig && viewDateStart >= auslieferungStart && viewDateStart <= heuteStart) ||
               (wiederholendUeberfaellig && viewDateStart >= faelligAm && viewDateStart <= heuteStart)
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