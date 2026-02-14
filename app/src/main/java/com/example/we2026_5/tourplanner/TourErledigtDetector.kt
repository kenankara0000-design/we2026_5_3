package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.util.TerminBerechnungUtils

/**
 * Helper-Klasse für Erledigt-Erkennung in TourDataProcessor.
 * Prüft ob Kunden am angezeigten Tag vollständig erledigt sind.
 */
class TourErledigtDetector(
    private val categorizer: TourDataCategorizer
) {

    /**
     * Prüft ob der Termin am angezeigten Tag vollständig erledigt wurde.
     * Regel: Wenn am Tag sowohl A (Abholung) als auch L (Auslieferung) fällig sind,
     * müssen BEIDE gedrückt sein. Keine Wäsche (KW) am Tag zählt ebenfalls als erledigt.
     */
    fun wurdeAmTagVollstaendigErledigt(
        customer: Customer,
        viewDateStart: Long,
        hatAbholungAmTag: Boolean,
        hatAuslieferungAmTag: Boolean,
        kwErledigtAmTag: Boolean
    ): Boolean {
        val abholungErledigtAmTag = customer.abholungErledigtAm > 0 &&
            TerminBerechnungUtils.isTimestampInBerlinDay(customer.abholungErledigtAm, viewDateStart)
        val auslieferungErledigtAmTag = customer.auslieferungErledigtAm > 0 &&
            TerminBerechnungUtils.isTimestampInBerlinDay(customer.auslieferungErledigtAm, viewDateStart)
        return when {
            hatAbholungAmTag && hatAuslieferungAmTag -> abholungErledigtAmTag && auslieferungErledigtAmTag
            hatAbholungAmTag -> abholungErledigtAmTag
            hatAuslieferungAmTag -> auslieferungErledigtAmTag
            else -> kwErledigtAmTag
        }
    }

    /** Liefert den spätesten Erledigungszeitstempel am viewDateStart (für Sortierung „neueste zuerst“). */
    fun erledigtZeitstempelAmTag(customer: Customer, viewDateStart: Long): Long {
        var maxTs = 0L
        if (customer.abholungErledigtAm > 0 && TerminBerechnungUtils.isTimestampInBerlinDay(customer.abholungErledigtAm, viewDateStart)) {
            maxTs = maxOf(maxTs, if (customer.abholungZeitstempel > 0) customer.abholungZeitstempel else customer.abholungErledigtAm)
        }
        if (customer.auslieferungErledigtAm > 0 && TerminBerechnungUtils.isTimestampInBerlinDay(customer.auslieferungErledigtAm, viewDateStart)) {
            maxTs = maxOf(maxTs, if (customer.auslieferungZeitstempel > 0) customer.auslieferungZeitstempel else customer.auslieferungErledigtAm)
        }
        if (customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 && TerminBerechnungUtils.isTimestampInBerlinDay(customer.keinerWäscheErledigtAm, viewDateStart)) {
            maxTs = maxOf(maxTs, customer.keinerWäscheErledigtAm)
        }
        return maxTs
    }

    fun warUeberfaelligUndErledigtAmDatum(customer: Customer, viewDateStart: Long): Boolean {
        val effectiveFaellig = TerminBerechnungUtils.effectiveFaelligAmDatum(customer)
        if (effectiveFaellig <= 0) return false
        val faelligAmStart = categorizer.getStartOfDay(effectiveFaellig)
        val erledigtAmViewDay = (customer.abholungErledigtAm > 0 && TerminBerechnungUtils.isTimestampInBerlinDay(customer.abholungErledigtAm, viewDateStart)) ||
            (customer.auslieferungErledigtAm > 0 && TerminBerechnungUtils.isTimestampInBerlinDay(customer.auslieferungErledigtAm, viewDateStart))
        return viewDateStart == faelligAmStart || erledigtAmViewDay
    }
}
