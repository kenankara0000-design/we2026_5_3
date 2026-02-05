package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils
import java.util.concurrent.TimeUnit

/**
 * Helper-Klasse für Datum-Berechnungen in der TourPlannerActivity.
 * getListen liefert Listen aus dem ViewModel (ohne runBlocking).
 */
class TourPlannerDateUtils(
    private val getListen: () -> List<KundenListe>
) {
    /** L = A + tageAzuL. Aus erstem Intervall oder Default 7. */
    private fun getTageAzuL(customer: Customer): Int =
        customer.intervalle.firstOrNull()?.let {
            if (it.abholungDatum > 0 && it.auslieferungDatum > 0)
                TimeUnit.MILLISECONDS.toDays(it.auslieferungDatum - it.abholungDatum).toInt().coerceIn(0, 365)
            else null
        } ?: 7

    /** Delegiert an TerminBerechnungUtils (Single Source of Truth). */
    fun getStartOfDay(ts: Long): Long = TerminBerechnungUtils.getStartOfDay(ts)
    
    fun calculateAbholungDatum(
        customer: Customer,
        viewDateStart: Long,
        heuteStart: Long
    ): Long {
        if (customer.listeId.isNotBlank()) {
            val liste = getListen().find { it.id == customer.listeId }
            if (liste != null) {
                liste.intervalle.forEach { intervall ->
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
            // A am viewDateStart: nur diesen einen Tag berechnen (kein Fenster).
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

            // ALTE STRUKTUR: Rückwärtskompatibilität für Kunden ohne Intervalle
            val verschobenAmViewDate = customer.verschobeneTermine.firstOrNull { getStartOfDay(it.verschobenAufDatum) == viewDateStart }
            if (verschobenAmViewDate != null) return verschobenAmViewDate.verschobenAufDatum
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
            val liste = getListen().find { it.id == customer.listeId }
            if (liste != null) {
                liste.intervalle.forEach { intervall ->
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
            // L = A + tageAzuL: L am viewDateStart existiert genau dann, wenn A am (viewDateStart − tageAzuL) existiert.
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

            // ALTE STRUKTUR: Rückwärtskompatibilität für Kunden ohne Intervalle
            val verschobenAmViewDateAusl = customer.verschobeneTermine.firstOrNull { getStartOfDay(it.verschobenAufDatum) == viewDateStart }
            if (verschobenAmViewDateAusl != null) return verschobenAmViewDateAusl.verschobenAufDatum
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
    
    /**
     * Nächstes Tour-Datum für einen Kunden (für "Nächste Tour" auf der Karte).
     * Berücksichtigt Listen-Kunden: Termin-Regel der Liste wird verwendet.
     * Überspringt Termine während des Urlaubs – zeigt erst das Datum nach dem Urlaub.
     */
    fun getNaechstesTourDatum(customer: Customer): Long {
        val heuteStart = getStartOfDay(System.currentTimeMillis())
        val liste = if (customer.listeId.isNotBlank()) getListen().find { it.id == customer.listeId } else null
        val geloeschte = if (liste != null) customer.geloeschteTermine + liste.geloeschteTermine else customer.geloeschteTermine
        val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
            customer = customer,
            liste = liste,
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
