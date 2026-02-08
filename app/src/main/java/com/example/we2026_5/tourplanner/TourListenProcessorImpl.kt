package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.util.TerminBerechnungUtils
import java.util.concurrent.TimeUnit

/**
 * Implementierung: Tour-Listen (listeId), Kunden mit zugeordneter Liste, fällig oder erledigt am Tag.
 */
class TourListenProcessorImpl(
    private val categorizer: TourDataCategorizer,
    private val filter: TourDataFilter,
    private val wasCompletedOnDay: (Customer, Long, Boolean, Boolean, Boolean) -> Boolean
) : TourListenProcessor {

    override fun fill(
        kundenNachListen: Map<String, List<Customer>>,
        allListen: List<KundenListe>,
        listenMitKunden: MutableMap<String, List<Customer>>,
        viewDateStart: Long,
        heuteStart: Long
    ) {
        kundenNachListen.forEach { (listeId, kunden) ->
            if (listeId.isEmpty()) return@forEach
            val liste = allListen.find { it.id == listeId } ?: return@forEach
            // Liste fällig am Tag = mindestens ein Kunde der Liste hat Termin oder ist überfällig (nur Kundendaten).
            val istFaellig = kunden.any { customer ->
                filter.hatKundeTerminAmDatum(customer, null, viewDateStart) ||
                    filter.istKundeUeberfaellig(customer, null, viewDateStart, heuteStart)
            }
            if (istFaellig) {
                val fälligeKunden = kunden.filter { customer ->
                    val kwErledigtAmTag = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
                        categorizer.getStartOfDay(customer.keinerWäscheErledigtAm) == viewDateStart
                    val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTag
                    if (isDone) {
                        // 3-Tage-Fenster (PLAN_TOURPLANNER_PERFORMANCE_3TAGE)
                        val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                            customer = customer,
                            liste = null,
                            startDatum = viewDateStart - TimeUnit.DAYS.toMillis(1),
                            tageVoraus = 3
                        )
                        val termineAmTag = termine.filter { categorizer.getStartOfDay(it.datum) == viewDateStart }
                        val warUeberfaelligUndErledigtAmDatum = if (customer.faelligAmDatum > 0) {
                            val faelligAmStart = categorizer.getStartOfDay(customer.faelligAmDatum)
                            val erledigtAmStart = if (customer.abholungErledigtAm > 0) {
                                categorizer.getStartOfDay(customer.abholungErledigtAm)
                            } else if (customer.auslieferungErledigtAm > 0) {
                                categorizer.getStartOfDay(customer.auslieferungErledigtAm)
                            } else 0L
                            viewDateStart == faelligAmStart || (erledigtAmStart > 0 && viewDateStart == erledigtAmStart)
                        } else false
                        val hatAbholungAmTag = termineAmTag.any { it.typ == TerminTyp.ABHOLUNG }
                        val hatAuslieferungAmTag = termineAmTag.any { it.typ == TerminTyp.AUSLIEFERUNG }
                        val wurdeAmTagErledigt = wasCompletedOnDay(customer, viewDateStart, hatAbholungAmTag, hatAuslieferungAmTag, kwErledigtAmTag)
                        if (warUeberfaelligUndErledigtAmDatum || wurdeAmTagErledigt) return@filter true
                    }
                    val isOverdue = filter.istKundeUeberfaellig(customer, null, viewDateStart, heuteStart)
                    if (isOverdue) return@filter true
                    filter.hatKundeTerminAmDatum(customer, null, viewDateStart)
                }
                if (fälligeKunden.isNotEmpty()) {
                    listenMitKunden[listeId] = fälligeKunden.sortedBy { it.name }
                }
            }
        }
    }
}
