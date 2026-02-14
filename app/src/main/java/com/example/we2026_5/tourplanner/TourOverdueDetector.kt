package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.util.TerminInfo

/**
 * Helper-Klasse für Überfällig-Erkennung in TourDataProcessor.
 * Prüft ob Termine überfällig sind und liefert Badge-Suffix (A/L/AL).
 */
class TourOverdueDetector(
    private val categorizer: TourDataCategorizer
) {

    fun istTerminUeberfaellig(terminStart: Long, viewDateStart: Long, heuteStart: Long): Boolean {
        val warVorHeuteFaellig = terminStart < heuteStart
        val istAmFaelligkeitstag = terminStart == viewDateStart
        val istHeute = viewDateStart == heuteStart
        val istHeuteFaellig = terminStart == heuteStart
        return (istAmFaelligkeitstag || (warVorHeuteFaellig && istHeute)) && !istHeuteFaellig
    }

    fun hatUeberfaelligeAbholung(
        customer: Customer,
        alleTermine: List<TerminInfo>,
        viewDateStart: Long,
        heuteStart: Long
    ): Boolean {
        val nurHeuteOderVergangenheit = viewDateStart <= heuteStart
        return nurHeuteOderVergangenheit && alleTermine.any { termin ->
            val terminStart = categorizer.getStartOfDay(termin.datum)
            val istAbholung = termin.typ == TerminTyp.ABHOLUNG
            val istNichtErledigt = !customer.abholungErfolgt
            istAbholung && istNichtErledigt && istTerminUeberfaellig(terminStart, viewDateStart, heuteStart)
        }
    }

    fun hatUeberfaelligeAuslieferung(
        customer: Customer,
        alleTermine: List<TerminInfo>,
        viewDateStart: Long,
        heuteStart: Long
    ): Boolean {
        val nurHeuteOderVergangenheit = viewDateStart <= heuteStart
        return nurHeuteOderVergangenheit && alleTermine.any { termin ->
            val terminStart = categorizer.getStartOfDay(termin.datum)
            val istAuslieferung = termin.typ == TerminTyp.AUSLIEFERUNG
            val istNichtErledigt = !customer.auslieferungErfolgt
            istAuslieferung && istNichtErledigt && istTerminUeberfaellig(terminStart, viewDateStart, heuteStart)
        }
    }

    /** Liefert für überfällige Kunden den Badge-Suffix „A“, „L“ oder „AL“; sonst null. */
    fun getOverdueAlSuffix(
        customer: Customer,
        alleTermine: List<TerminInfo>,
        viewDateStart: Long,
        heuteStart: Long
    ): String? {
        val hatA = hatUeberfaelligeAbholung(customer, alleTermine, viewDateStart, heuteStart)
        val hatL = hatUeberfaelligeAuslieferung(customer, alleTermine, viewDateStart, heuteStart)
        return when {
            hatA && hatL -> "AL"
            hatA -> "A"
            hatL -> "L"
            else -> null
        }
    }
}
