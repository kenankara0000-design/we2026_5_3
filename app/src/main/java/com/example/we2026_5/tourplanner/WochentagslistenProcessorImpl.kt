package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe

/**
 * Implementierung: Wochentagslisten (wochentag 0..6), nur G/P ohne listeId, nur nicht-erledigte.
 */
class WochentagslistenProcessorImpl(
    private val categorizer: TourDataCategorizer,
    private val filter: TourDataFilter
) : WochentagslistenProcessor {

    override fun fill(
        allCustomers: List<Customer>,
        allListen: List<KundenListe>,
        listenMitKunden: MutableMap<String, List<Customer>>,
        viewDateStart: Long,
        heuteStart: Long
    ) {
        allListen.filter { it.wochentag in 0..6 }.forEach { liste ->
            val kunden = allCustomers.filter { k ->
                (k.kundenArt == "Gewerblich" || k.kundenArt == "Privat") &&
                    k.listeId.isEmpty() &&
                    (k.defaultAbholungWochentag == liste.wochentag || k.defaultAuslieferungWochentag == liste.wochentag)
            }
            val fälligeKunden = kunden.filter { customer ->
                val kwErledigtAmTag = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
                    categorizer.getStartOfDay(customer.keinerWäscheErledigtAm) == viewDateStart
                val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTag
                if (isDone) return@filter false
                val isOverdue = filter.istKundeUeberfaellig(customer, null, viewDateStart, heuteStart)
                if (isOverdue) return@filter true
                filter.hatKundeTerminAmDatum(customer, null, viewDateStart)
            }
            if (fälligeKunden.isNotEmpty()) {
                listenMitKunden[liste.id] = fälligeKunden.sortedBy { it.name }
            }
        }
    }
}
