package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils
import com.example.we2026_5.util.TerminInfo
import com.example.we2026_5.util.tageAzuLOrDefault
import java.util.concurrent.TimeUnit

/**
 * Berechnet den Status-Badge-Text für Tourenplaner-Kunden.
 * Läuft ohne Context – gibt die gleichen Strings zurück wie R.string (A, L, AL, A-A, A-L).
 * Eine Berechnung pro Kunde (getAlleTermine einmal aufgerufen).
 */
object TourPlannerStatusBadge {

    fun compute(customer: Customer, viewDateStart: Long, heuteStart: Long, termincache: TerminCache? = null, liste: KundenListe? = null): String {
        val effectiveListe = if (liste != null && customer.listeId == liste.id) liste else null
        val alleTermine = if (termincache != null) {
            val cached = termincache.getTermine365(customer, effectiveListe)
            val (startDatum, tageVoraus) = if (viewDateStart <= heuteStart) {
                Pair(heuteStart - TimeUnit.DAYS.toMillis(60), 63)
            } else {
                val tageAzuL = customer.tageAzuLOrDefault(7).coerceIn(0, 365)
                val start = viewDateStart - TimeUnit.DAYS.toMillis(tageAzuL.toLong())
                val tage = tageAzuL + 3
                Pair(start, tage)
            }
            val start = startDatum
            val end = start + TimeUnit.DAYS.toMillis(tageVoraus.toLong())
            cached.filter { it.datum in start..end }
        } else {
            val (startDatum, tageVoraus) = if (viewDateStart <= heuteStart) {
                Pair(heuteStart - TimeUnit.DAYS.toMillis(60), 63)
            } else {
                val tageAzuL = customer.tageAzuLOrDefault(7).coerceIn(0, 365)
                val start = viewDateStart - TimeUnit.DAYS.toMillis(tageAzuL.toLong())
                val tage = tageAzuL + 3
                Pair(start, tage)
            }
            TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                liste = effectiveListe,
                startDatum = startDatum,
                tageVoraus = tageVoraus
            )
        }
        return computeWithTermine(customer, viewDateStart, heuteStart, alleTermine)
    }

    fun computeWithTermine(
        customer: Customer,
        viewDateStart: Long,
        heuteStart: Long,
        alleTermine: List<TerminInfo>
    ): String {
        val ausnahmeA = customer.ausnahmeTermine.any {
            TerminBerechnungUtils.getStartOfDay(it.datum) == viewDateStart && it.typ == "A"
        }
        val ausnahmeL = customer.ausnahmeTermine.any {
            TerminBerechnungUtils.getStartOfDay(it.datum) == viewDateStart && it.typ == "L"
        }
        if (ausnahmeA || ausnahmeL) return if (ausnahmeA && ausnahmeL) "A-A" else if (ausnahmeA) "A-A" else "A-L"

        val kundenTerminA = customer.kundenTermine.any {
            TerminBerechnungUtils.getStartOfDay(it.datum) == viewDateStart && it.typ == "A"
        }
        val kundenTerminL = customer.kundenTermine.any {
            TerminBerechnungUtils.getStartOfDay(it.datum) == viewDateStart && it.typ == "L"
        }
        if (kundenTerminA || kundenTerminL) return when {
            kundenTerminA && kundenTerminL -> "AL"
            kundenTerminA -> "A"
            else -> "L"
        }

        val istImUrlaubAmTag = TerminFilterUtils.istTerminInUrlaubEintraege(viewDateStart, customer)
        if (istImUrlaubAmTag) return ""

        val hatKlassischUeberfaelligeAbholung = hasKlassischUeberfaelligeAbholung(customer, viewDateStart, heuteStart, alleTermine)
        val hatKlassischUeberfaelligeAuslieferung = hasKlassischUeberfaelligeAuslieferung(customer, viewDateStart, heuteStart, alleTermine)
        if ((hatKlassischUeberfaelligeAbholung || hatKlassischUeberfaelligeAuslieferung) && viewDateStart <= heuteStart) return ""

        val hatUeberfaelligeAbholung = hatKlassischUeberfaelligeAbholung || (viewDateStart == heuteStart && !customer.abholungErfolgt &&
            alleTermine.any { it.typ == TerminTyp.ABHOLUNG && TerminBerechnungUtils.getStartOfDay(it.datum) == heuteStart })
        val hatUeberfaelligeAuslieferung = hatKlassischUeberfaelligeAuslieferung || (viewDateStart == heuteStart && !customer.auslieferungErfolgt &&
            alleTermine.any { it.typ == TerminTyp.AUSLIEFERUNG && TerminBerechnungUtils.getStartOfDay(it.datum) == heuteStart })

        val istAmFaelligkeitstag = viewDateStart < heuteStart
        val nurInfoAmFaelligkeitstag = istAmFaelligkeitstag && (hatUeberfaelligeAbholung || hatUeberfaelligeAuslieferung)

        val abholungDatumHeute = alleTermine.filter { it.typ == TerminTyp.ABHOLUNG }
            .firstOrNull { TerminBerechnungUtils.getStartOfDay(it.datum) == viewDateStart }?.datum ?: 0L
        val hatAbholungHeute = abholungDatumHeute > 0
        val wurdeHeuteErledigt = customer.abholungErledigtAm > 0 &&
            TerminBerechnungUtils.isTimestampInBerlinDay(customer.abholungErledigtAm, viewDateStart)
        val sollAButtonAnzeigen = hatAbholungHeute || hatUeberfaelligeAbholung || wurdeHeuteErledigt

        val auslieferungDatumHeute = alleTermine.filter { it.typ == TerminTyp.AUSLIEFERUNG }
            .firstOrNull { TerminBerechnungUtils.getStartOfDay(it.datum) == viewDateStart }?.datum ?: 0L
        val hatAuslieferungHeute = auslieferungDatumHeute > 0
        val wurdeAmTagErledigtL = customer.auslieferungErledigtAm > 0 &&
            TerminBerechnungUtils.isTimestampInBerlinDay(customer.auslieferungErledigtAm, viewDateStart)
        val sollLButtonAnzeigen = hatAuslieferungHeute || hatUeberfaelligeAuslieferung || wurdeAmTagErledigtL

        return when {
            nurInfoAmFaelligkeitstag -> when {
                hatUeberfaelligeAbholung && hatUeberfaelligeAuslieferung -> "AL"
                hatUeberfaelligeAbholung -> "A"
                hatUeberfaelligeAuslieferung -> "L"
                else -> ""
            }
            sollAButtonAnzeigen && sollLButtonAnzeigen -> "AL"
            sollAButtonAnzeigen -> "A"
            sollLButtonAnzeigen -> "L"
            else -> ""
        }
    }

    private fun hasKlassischUeberfaelligeAbholung(
        customer: Customer,
        viewDateStart: Long,
        heuteStart: Long,
        alleTermine: List<TerminInfo>
    ): Boolean = alleTermine.any { termin ->
        termin.typ == TerminTyp.ABHOLUNG &&
            TerminFilterUtils.istUeberfaellig(termin.datum, heuteStart, customer.abholungErfolgt) &&
            TerminFilterUtils.sollUeberfaelligAnzeigen(termin.datum, viewDateStart, heuteStart)
    }

    private fun hasKlassischUeberfaelligeAuslieferung(
        customer: Customer,
        viewDateStart: Long,
        heuteStart: Long,
        alleTermine: List<TerminInfo>
    ): Boolean = alleTermine.any { termin ->
        termin.typ == TerminTyp.AUSLIEFERUNG &&
            TerminFilterUtils.istUeberfaellig(termin.datum, heuteStart, customer.auslieferungErfolgt) &&
            TerminFilterUtils.sollUeberfaelligAnzeigen(termin.datum, viewDateStart, heuteStart)
    }
}
