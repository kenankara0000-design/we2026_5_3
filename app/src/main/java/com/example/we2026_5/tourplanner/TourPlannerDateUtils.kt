package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

/**
 * Helper-Klasse für Datum-Berechnungen in der TourPlannerActivity
 */
class TourPlannerDateUtils(
    private val listeRepository: KundenListeRepository
) {
    
    fun getStartOfDay(ts: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = ts
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    
    fun calculateAbholungDatum(
        customer: Customer,
        viewDateStart: Long,
        heuteStart: Long
    ): Long {
        if (customer.listeId.isNotBlank()) {
            // Kunde gehört zu einer Liste
            var liste: com.example.we2026_5.KundenListe? = null
            runBlocking {
                liste = listeRepository.getListeById(customer.listeId)
            }
            
            if (liste != null) {
                // Prüfe alle Intervalle der Liste
                liste!!.intervalle.forEach { intervall ->
                    val abholungStart = getStartOfDay(intervall.abholungDatum)
                    
                    if (!intervall.wiederholen) {
                        // Einmaliges Intervall: Prüfe ob Abholungsdatum heute fällig ist
                        if (abholungStart == viewDateStart) {
                            return intervall.abholungDatum
                        }
                    } else {
                        // Wiederholendes Intervall: Berechne korrektes Datum
                        val intervallTage = intervall.intervallTage.coerceIn(1, 365)
                        val intervallTageLong = intervallTage.toLong()
                        
                        if (viewDateStart >= abholungStart) {
                            val tageSeitAbholung = TimeUnit.MILLISECONDS.toDays(viewDateStart - abholungStart)
                            // Berechne Zyklus und erwartetes Datum
                            val zyklus = tageSeitAbholung / intervallTageLong
                            val erwartetesDatum = abholungStart + TimeUnit.DAYS.toMillis(zyklus * intervallTageLong)
                            val erwartetesDatumStart = getStartOfDay(erwartetesDatum)
                            
                            // Prüfe ob viewDateStart genau auf einem Zyklus liegt
                            if (erwartetesDatumStart == viewDateStart && tageSeitAbholung <= 365) {
                                return erwartetesDatum
                            }
                        }
                    }
                }
            }
            return 0L // Nicht fällig an diesem Tag
        } else {
            // NEUE STRUKTUR: Verwende Intervalle-Liste wenn vorhanden (aus TerminRegeln)
            if (customer.intervalle.isNotEmpty()) {
                val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                    customer = customer,
                    startDatum = viewDateStart - TimeUnit.DAYS.toMillis(1),
                    tageVoraus = 2 // Nur 2 Tage (gestern, heute, morgen)
                )
                // Prüfe ob am angezeigten Tag ein Abholungstermin vorhanden ist
                return termine.firstOrNull { 
                    it.typ == com.example.we2026_5.TerminTyp.ABHOLUNG &&
                    TerminBerechnungUtils.getStartOfDay(it.datum) == viewDateStart
                }?.datum ?: 0L
            }
            
            // ALTE STRUKTUR: Rückwärtskompatibilität für Kunden ohne Intervalle
            if (customer.verschobenAufDatum > 0) {
                val verschobenStart = getStartOfDay(customer.verschobenAufDatum)
                if (verschobenStart == viewDateStart) return customer.verschobenAufDatum
            }
            val abholungStart = getStartOfDay(customer.abholungDatum)
            if (abholungStart == viewDateStart) return customer.abholungDatum
            // Für wiederholende Termine
            if (customer.wiederholen && customer.letzterTermin > 0) {
                val intervallTage = customer.intervallTage.coerceIn(1, 365)
                val intervallTageLong = intervallTage.toLong()
                val letzterTerminStart = getStartOfDay(customer.letzterTermin)
                
                if (viewDateStart > letzterTerminStart) {
                    val tageSeitLetztemTermin = TimeUnit.MILLISECONDS.toDays(viewDateStart - letzterTerminStart)
                    val zyklus = tageSeitLetztemTermin / intervallTageLong
                    val erwartetesDatum = letzterTerminStart + TimeUnit.DAYS.toMillis(zyklus * intervallTageLong)
                    val erwartetesDatumStart = getStartOfDay(erwartetesDatum)
                    
                    if (erwartetesDatumStart == viewDateStart && tageSeitLetztemTermin <= 365) {
                        return erwartetesDatum
                    }
                }
            }
            return 0L
        }
    }
    
    fun calculateAuslieferungDatum(
        customer: Customer,
        viewDateStart: Long,
        heuteStart: Long
    ): Long {
        if (customer.listeId.isNotBlank()) {
            // Kunde gehört zu einer Liste
            var liste: com.example.we2026_5.KundenListe? = null
            runBlocking {
                liste = listeRepository.getListeById(customer.listeId)
            }
            
            if (liste != null) {
                // Prüfe alle Intervalle der Liste
                liste!!.intervalle.forEach { intervall ->
                    val auslieferungStart = getStartOfDay(intervall.auslieferungDatum)
                    
                    if (!intervall.wiederholen) {
                        // Einmaliges Intervall: Prüfe ob Auslieferungsdatum heute fällig ist
                        if (auslieferungStart == viewDateStart) {
                            return intervall.auslieferungDatum
                        }
                    } else {
                        // Wiederholendes Intervall: Berechne korrektes Datum
                        val intervallTage = intervall.intervallTage.coerceIn(1, 365)
                        val intervallTageLong = intervallTage.toLong()
                        
                        if (viewDateStart >= auslieferungStart) {
                            val tageSeitAuslieferung = TimeUnit.MILLISECONDS.toDays(viewDateStart - auslieferungStart)
                            // Berechne Zyklus und erwartetes Datum
                            val zyklus = tageSeitAuslieferung / intervallTageLong
                            val erwartetesDatum = auslieferungStart + TimeUnit.DAYS.toMillis(zyklus * intervallTageLong)
                            val erwartetesDatumStart = getStartOfDay(erwartetesDatum)
                            
                            // Prüfe ob viewDateStart genau auf einem Zyklus liegt
                            if (erwartetesDatumStart == viewDateStart && tageSeitAuslieferung <= 365) {
                                return erwartetesDatum
                            }
                        }
                    }
                }
            }
            return 0L // Nicht fällig an diesem Tag
        } else {
            // NEUE STRUKTUR: Verwende Intervalle-Liste wenn vorhanden (aus TerminRegeln)
            if (customer.intervalle.isNotEmpty()) {
                val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                    customer = customer,
                    startDatum = viewDateStart - TimeUnit.DAYS.toMillis(1),
                    tageVoraus = 2 // Nur 2 Tage (gestern, heute, morgen)
                )
                // Prüfe ob am angezeigten Tag ein Auslieferungstermin vorhanden ist
                return termine.firstOrNull { 
                    it.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG &&
                    TerminBerechnungUtils.getStartOfDay(it.datum) == viewDateStart
                }?.datum ?: 0L
            }
            
            // ALTE STRUKTUR: Rückwärtskompatibilität für Kunden ohne Intervalle
            if (customer.verschobenAufDatum > 0) {
                val verschobenStart = getStartOfDay(customer.verschobenAufDatum)
                if (verschobenStart == viewDateStart) return customer.verschobenAufDatum
            }
            val auslieferungStart = getStartOfDay(customer.auslieferungDatum)
            if (auslieferungStart == viewDateStart) return customer.auslieferungDatum
            // Für wiederholende Termine
            if (customer.wiederholen && customer.letzterTermin > 0) {
                val intervallTage = customer.intervallTage.coerceIn(1, 365)
                val intervallTageLong = intervallTage.toLong()
                val letzterTerminStart = getStartOfDay(customer.letzterTermin)
                
                if (viewDateStart > letzterTerminStart) {
                    val tageSeitLetztemTermin = TimeUnit.MILLISECONDS.toDays(viewDateStart - letzterTerminStart)
                    val zyklus = tageSeitLetztemTermin / intervallTageLong
                    val erwartetesDatum = letzterTerminStart + TimeUnit.DAYS.toMillis(zyklus * intervallTageLong)
                    val erwartetesDatumStart = getStartOfDay(erwartetesDatum)
                    
                    if (erwartetesDatumStart == viewDateStart && tageSeitLetztemTermin <= 365) {
                        return erwartetesDatum
                    }
                }
            }
            return 0L
        }
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
}
