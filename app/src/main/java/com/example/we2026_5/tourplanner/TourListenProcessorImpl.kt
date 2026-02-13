package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.util.TerminBerechnungUtils
import java.util.concurrent.TimeUnit

/**
 * Implementierung: Listen ohne Wochentag (listeId), Kunden mit zugeordneter Liste, fällig oder erledigt am Tag.
 */
class TourListenProcessorImpl(
    private val categorizer: TourDataCategorizer,
    private val filter: TourDataFilter,
    private val termincache: TerminCache,
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
                filter.hatKundeTerminAmDatum(customer, liste, viewDateStart) ||
                    filter.istKundeUeberfaellig(customer, liste, viewDateStart, heuteStart)
            }
            if (istFaellig) {
                val fälligeKunden = kunden.filter { customer ->
                    val kwErledigtAmTag = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
                        TerminBerechnungUtils.isTimestampInBerlinDay(customer.keinerWäscheErledigtAm, viewDateStart)
                    val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTag
                    if (isDone) {
                        // 3-Tage-Fenster (PLAN_TOURPLANNER_PERFORMANCE_3TAGE)
                        val termine = termincache.getTermineInRange(
                            customer = customer,
                            startDatum = viewDateStart - TimeUnit.DAYS.toMillis(1),
                            tageVoraus = 3,
                            liste = liste
                        )
                        val termineAmTag = termine.filter { categorizer.getStartOfDay(it.datum) == viewDateStart }
                        val effectiveFaellig = TerminBerechnungUtils.effectiveFaelligAmDatum(customer)
                        val warUeberfaelligUndErledigtAmDatum = if (effectiveFaellig > 0) {
                            val faelligAmStart = categorizer.getStartOfDay(effectiveFaellig)
                            val erledigtAmViewDay = (customer.abholungErledigtAm > 0 && TerminBerechnungUtils.isTimestampInBerlinDay(customer.abholungErledigtAm, viewDateStart)) ||
                                (customer.auslieferungErledigtAm > 0 && TerminBerechnungUtils.isTimestampInBerlinDay(customer.auslieferungErledigtAm, viewDateStart))
                            viewDateStart == faelligAmStart || erledigtAmViewDay
                        } else false
                        val hatAbholungAmTag = termineAmTag.any { it.typ == TerminTyp.ABHOLUNG }
                        val hatAuslieferungAmTag = termineAmTag.any { it.typ == TerminTyp.AUSLIEFERUNG }
                        val wurdeAmTagErledigt = wasCompletedOnDay(customer, viewDateStart, hatAbholungAmTag, hatAuslieferungAmTag, kwErledigtAmTag)
                        if (warUeberfaelligUndErledigtAmDatum || wurdeAmTagErledigt) return@filter true
                    }
                    val isOverdue = filter.istKundeUeberfaellig(customer, liste, viewDateStart, heuteStart)
                    if (isOverdue) return@filter true
                    filter.hatKundeTerminAmDatum(customer, liste, viewDateStart)
                }
                if (fälligeKunden.isNotEmpty()) {
                    listenMitKunden[listeId] = fälligeKunden.sortedBy { it.name }
                }
            }
        }
    }
}
