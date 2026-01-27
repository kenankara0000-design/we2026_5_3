package com.example.we2026_5.util

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
     * Prüft ob ein Termin im Urlaub liegt
     */
    fun istTerminImUrlaub(terminDatum: Long, urlaubVon: Long, urlaubBis: Long): Boolean {
        if (urlaubVon == 0L || urlaubBis == 0L) return false
        return terminDatum in urlaubVon..urlaubBis
    }
    
    /**
     * Prüft ob ein Termin überfällig ist
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
        
        // Überfällig nur anzeigen:
        // 1. Am tatsächlichen Fälligkeitstag (terminStart == anzeigeStart)
        // 2. Am heutigen Tag, wenn noch überfällig (anzeigeStart == aktuellesStart && terminStart < aktuellesStart)
        // NICHT anzeigen: Zwischen Fälligkeitstag und heute
        return (terminStart == anzeigeStart) || (anzeigeStart == aktuellesStart && terminStart < aktuellesStart)
    }
}
