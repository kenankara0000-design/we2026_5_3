package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.UrlaubEintrag
import com.example.we2026_5.VerschobenerTermin

/**
 * Utility-Klasse für Termin-Filter-Logik.
 * Extrahiert Filter-Funktionen aus TerminBerechnungUtils.
 */
object TerminFilterUtils {
    
    /**
     * Prüft ob ein Termin verschoben wurde
     */
    fun istTerminVerschoben(
        terminDatum: Long,
        verschobeneTermine: List<VerschobenerTermin>,
        intervallId: String? = null
    ): VerschobenerTermin? {
        val terminStart = TerminBerechnungUtils.getStartOfDay(terminDatum)
        return verschobeneTermine.firstOrNull { verschoben ->
            val originalStart = TerminBerechnungUtils.getStartOfDay(verschoben.originalDatum)
            originalStart == terminStart && 
            (verschoben.intervallId == null || verschoben.intervallId == intervallId)
        }
    }
    
    /**
     * Prüft ob ein Termin gelöscht wurde
     */
    fun istTerminGeloescht(terminDatum: Long, geloeschteTermine: List<Long>): Boolean {
        val terminStart = TerminBerechnungUtils.getStartOfDay(terminDatum)
        return geloeschteTermine.contains(terminStart)
    }
    
    /**
     * Prüft ob ein Termin im Urlaub liegt (ein Zeitraum)
     */
    fun istTerminImUrlaub(terminDatum: Long, urlaubVon: Long, urlaubBis: Long): Boolean {
        if (urlaubVon == 0L || urlaubBis == 0L) return false
        return terminDatum in urlaubVon..urlaubBis
    }

    /**
     * Effektive Urlaubseinträge: Liste oder ein Eintrag aus urlaubVon/urlaubBis.
     */
    fun getEffectiveUrlaubEintraege(customer: Customer): List<UrlaubEintrag> {
        return if (customer.urlaubEintraege.isNotEmpty()) customer.urlaubEintraege
        else if (customer.urlaubVon > 0L && customer.urlaubBis > 0L) listOf(UrlaubEintrag(customer.urlaubVon, customer.urlaubBis))
        else emptyList()
    }

    /**
     * Prüft ob ein Termin in einem der Urlaubseinträge des Kunden liegt.
     */
    fun istTerminInUrlaubEintraege(terminDatum: Long, customer: Customer): Boolean {
        val terminStart = TerminBerechnungUtils.getStartOfDay(terminDatum)
        for (e in getEffectiveUrlaubEintraege(customer)) {
            val vonStart = TerminBerechnungUtils.getStartOfDay(e.von)
            val bisStart = TerminBerechnungUtils.getStartOfDay(e.bis)
            if (terminStart in vonStart..bisStart) return true
        }
        return false
    }
    
    /**
     * Prüft ob ein Termin überfällig ist (Fälligkeitstag liegt vor dem heutigen Tag).
     */
    fun istUeberfaellig(
        terminDatum: Long,
        aktuellesDatum: Long = System.currentTimeMillis(),
        erledigt: Boolean
    ): Boolean {
        if (erledigt) return false
        val terminStart = TerminBerechnungUtils.getStartOfDay(terminDatum)
        val aktuellesStart = TerminBerechnungUtils.getStartOfDay(aktuellesDatum)
        return terminStart < aktuellesStart
    }

    /**
     * „Heute überfällig“: Termin ist am oder vor dem heutigen Tag fällig und noch nicht erledigt.
     * Überfällige Termine dürfen nur am Tag „Heute“ erledigt werden.
     */
    fun istHeuteUeberfaellig(
        terminDatum: Long,
        heuteStart: Long,
        erledigt: Boolean
    ): Boolean {
        if (erledigt) return false
        val terminStart = TerminBerechnungUtils.getStartOfDay(terminDatum)
        return terminStart <= heuteStart
    }
    
    /**
     * Prüft ob ein überfälliger Termin an einem bestimmten Datum angezeigt werden soll
     * NEUE LOGIK: Überfällige Kunden werden nur am Fälligkeitstag und am heutigen Tag angezeigt
     */
    fun sollUeberfaelligAnzeigen(
        terminDatum: Long,
        anzeigeDatum: Long,
        aktuellesDatum: Long = System.currentTimeMillis()
    ): Boolean {
        val terminStart = TerminBerechnungUtils.getStartOfDay(terminDatum)
        val anzeigeStart = TerminBerechnungUtils.getStartOfDay(anzeigeDatum)
        val aktuellesStart = TerminBerechnungUtils.getStartOfDay(aktuellesDatum)
        
        // Nie als überfällig anzeigen, wenn Anzeige-Datum in der Zukunft liegt (z. B. „morgen“)
        if (anzeigeStart > aktuellesStart) return false
        
        // Überfällig nur anzeigen:
        // 1. Am tatsächlichen Fälligkeitstag (terminStart == anzeigeStart)
        // 2. Am heutigen Tag, wenn noch überfällig (anzeigeStart == aktuellesStart && terminStart < aktuellesStart)
        // NICHT anzeigen: Zwischen Fälligkeitstag und heute
        return (terminStart == anzeigeStart) || (anzeigeStart == aktuellesStart && terminStart < aktuellesStart)
    }
}
