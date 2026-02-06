package com.example.we2026_5.adapter

import android.content.Context
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.util.TerminInfo
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.tourplanner.ErledigungSheetState
import java.util.concurrent.TimeUnit

/**
 * Helper für Erledigungs-Bottom-Sheet: berechnet den Zustand (getSheetState) für Compose-UI.
 */
class CustomerButtonVisibilityHelper(
    private val context: Context,
    private val displayedDateMillis: Long?,
    private val pressedButtons: Map<String, String>,
    private val getAbholungDatum: ((Customer) -> Long)?,
    private val getAuslieferungDatum: ((Customer) -> Long)?,
    private val getAlleTermine: (Customer, Long, Int) -> List<TerminInfo>
) {

    /**
     * Berechnet den Zustand für das Erledigungs-Bottom-Sheet (für alle Kundenarten inkl. Listen).
     */
    fun getSheetState(customer: Customer): ErledigungSheetState? {
        if (displayedDateMillis == null) return null
        val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
        val viewDateStart = TerminBerechnungUtils.getStartOfDay(displayedDateMillis)
        val pressedButton = pressedButtons[customer.id]

        val abholungDatumHeute = getAbholungDatum?.invoke(customer) ?: 0L
        val hatAbholungHeute = abholungDatumHeute > 0 && TerminBerechnungUtils.getStartOfDay(abholungDatumHeute) == viewDateStart
        val hatUeberfaelligeAbholung = hasUeberfaelligeAbholung(customer, viewDateStart, heuteStart)
        val wurdeHeuteErledigt = customer.abholungErledigtAm > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.abholungErledigtAm) == viewDateStart
        val sollAButtonAnzeigen = hatAbholungHeute || hatUeberfaelligeAbholung || wurdeHeuteErledigt
        val istAmTatsaechlichenAbholungTag = hatAbholungHeute && !hatUeberfaelligeAbholung
        val istHeute = viewDateStart == heuteStart

        val auslieferungDatumHeute = getAuslieferungDatum?.invoke(customer) ?: 0L
        val hatAuslieferungHeute = auslieferungDatumHeute > 0 && TerminBerechnungUtils.getStartOfDay(auslieferungDatumHeute) == viewDateStart
        val hatUeberfaelligeAuslieferung = hasUeberfaelligeAuslieferung(customer, viewDateStart, heuteStart)
        val wurdeAmTagErledigtL = customer.auslieferungErledigtAm > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.auslieferungErledigtAm) == viewDateStart
        val kwErledigtAmTag = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.keinerWäscheErledigtAm) == viewDateStart
        var sollLButtonAnzeigen = hatAuslieferungHeute || hatUeberfaelligeAuslieferung || wurdeAmTagErledigtL
        if (kwErledigtAmTag) sollLButtonAnzeigen = false
        val istAmTatsaechlichenAuslieferungTag = hatAuslieferungHeute && !hatUeberfaelligeAuslieferung

        val sollKwButtonAnzeigen = sollAButtonAnzeigen || (hatAuslieferungHeute || hatUeberfaelligeAuslieferung || wurdeAmTagErledigtL)
        val wurdeKwHeuteErledigt = kwErledigtAmTag || (istHeute && pressedButton == "KW")

        val hatVerschobenenTerminHeute = customer.verschobeneTermine.any { verschoben ->
            val originalStart = TerminBerechnungUtils.getStartOfDay(verschoben.originalDatum)
            val verschobenStart = TerminBerechnungUtils.getStartOfDay(verschoben.verschobenAufDatum)
            originalStart == viewDateStart || verschobenStart == viewDateStart
        }
        // Verschieben: aktiv wenn Kunde an diesem Tag einen Termin (A/L) hat ODER bereits verschobene Termine
        val hatTerminAmTag = sollAButtonAnzeigen || sollLButtonAnzeigen
        val vButtonAktiv = (hatTerminAmTag || hatVerschobenenTerminHeute)
        val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTag

        // Urlaub: immer aktiv (Urlaub jederzeit eintragen möglich)
        val uButtonAktiv = true

        val hatErledigtenATerminAmDatum = if (customer.abholungErfolgt) {
            val abholungErledigtAmStart = if (customer.abholungErledigtAm > 0) TerminBerechnungUtils.getStartOfDay(customer.abholungErledigtAm) else 0L
            if (abholungErledigtAmStart > 0 && viewDateStart == abholungErledigtAmStart) true
            else abholungDatumHeute > 0 && TerminBerechnungUtils.getStartOfDay(abholungDatumHeute) == viewDateStart
        } else false
        val hatErledigtenLTerminAmDatum = if (customer.auslieferungErfolgt) {
            val auslieferungErledigtAmStart = if (customer.auslieferungErledigtAm > 0) TerminBerechnungUtils.getStartOfDay(customer.auslieferungErledigtAm) else 0L
            if (auslieferungErledigtAmStart > 0 && viewDateStart == auslieferungErledigtAmStart) true
            else auslieferungDatumHeute > 0 && TerminBerechnungUtils.getStartOfDay(auslieferungDatumHeute) == viewDateStart
        } else false
        val hatAbholungRelevantAmTag = hatAbholungHeute || hatUeberfaelligeAbholung
        val hatAuslieferungRelevantAmTag = hatAuslieferungHeute || hatUeberfaelligeAuslieferung
        val beideRelevantAmTag = hatAbholungRelevantAmTag && hatAuslieferungRelevantAmTag
        val hatKwErledigtAmDatum = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.keinerWäscheErledigtAm) == viewDateStart
        val sollRueckgaengigAnzeigen = if (istHeute) {
            if (beideRelevantAmTag) hatErledigtenATerminAmDatum && hatErledigtenLTerminAmDatum
            else hatErledigtenATerminAmDatum || hatErledigtenLTerminAmDatum || hatKwErledigtAmDatum
        } else false

        val istAmFaelligkeitstag = viewDateStart < heuteStart
        val nurInfoAmFaelligkeitstag = istAmFaelligkeitstag && (hatUeberfaelligeAbholung || hatUeberfaelligeAuslieferung)
        val istImUrlaubAmTag = TerminFilterUtils.istTerminInUrlaubEintraege(viewDateStart, customer)

        var enableA = sollAButtonAnzeigen && istHeute && !customer.abholungErfolgt && (istAmTatsaechlichenAbholungTag || hatUeberfaelligeAbholung)
        var enableL = sollLButtonAnzeigen && istHeute && customer.abholungErfolgt && !customer.auslieferungErfolgt && (istAmTatsaechlichenAuslieferungTag || hatUeberfaelligeAuslieferung)
        var enableKw = sollKwButtonAnzeigen && istHeute && !wurdeKwHeuteErledigt
        if (istImUrlaubAmTag) {
            enableA = false
            enableL = false
            enableKw = false
        }

        /** Echt überfällig = Fälligkeit vor heute (nicht „heute fällig“). Für Badge „Ü“ nur bei echt überfällig. */
        val hatKlassischUeberfaelligeAbholung = hasKlassischUeberfaelligeAbholung(customer, viewDateStart, heuteStart)
        val hatKlassischUeberfaelligeAuslieferung = hasKlassischUeberfaelligeAuslieferung(customer, viewDateStart, heuteStart)

        val overdueInfoText = when {
            !nurInfoAmFaelligkeitstag -> ""
            hatUeberfaelligeAbholung && hatUeberfaelligeAuslieferung -> context.getString(com.example.we2026_5.R.string.info_overdue_a_l)
            hatUeberfaelligeAbholung -> context.getString(com.example.we2026_5.R.string.info_overdue_a)
            hatUeberfaelligeAuslieferung -> context.getString(com.example.we2026_5.R.string.info_overdue_l)
            else -> ""
        }

        val showAbholungFinal = if (nurInfoAmFaelligkeitstag) false else sollAButtonAnzeigen
        val showAuslieferungFinal = if (nurInfoAmFaelligkeitstag) false else sollLButtonAnzeigen
        val showKwFinal = if (nurInfoAmFaelligkeitstag) false else sollKwButtonAnzeigen

        // Ausnahme-Termine (A-A / A-L) zuerst; haben keinen Einfluss auf reguläre A/L
        val ausnahmeA = customer.ausnahmeTermine.any {
            TerminBerechnungUtils.getStartOfDay(it.datum) == viewDateStart && it.typ == "A"
        }
        val ausnahmeL = customer.ausnahmeTermine.any {
            TerminBerechnungUtils.getStartOfDay(it.datum) == viewDateStart && it.typ == "L"
        }
        // Nur A und L als Badge; Ü (Überfällig), KW, U (Urlaub) nur über Karten-Hintergrund/Status
        val statusBadgeText = when {
            ausnahmeA || ausnahmeL -> when {
                ausnahmeA && ausnahmeL -> context.getString(com.example.we2026_5.R.string.badge_ausnahme_a)
                ausnahmeA -> context.getString(com.example.we2026_5.R.string.badge_ausnahme_a)
                else -> context.getString(com.example.we2026_5.R.string.badge_ausnahme_l)
            }
            istImUrlaubAmTag -> ""
            (hatKlassischUeberfaelligeAbholung || hatKlassischUeberfaelligeAuslieferung) && viewDateStart <= heuteStart -> ""
            nurInfoAmFaelligkeitstag -> when {
                hatUeberfaelligeAbholung && hatUeberfaelligeAuslieferung -> context.getString(com.example.we2026_5.R.string.status_badge_a_plus_l_short)
                hatUeberfaelligeAbholung -> context.getString(com.example.we2026_5.R.string.status_badge_a_short)
                hatUeberfaelligeAuslieferung -> context.getString(com.example.we2026_5.R.string.status_badge_l_short)
                else -> ""
            }
            sollAButtonAnzeigen && sollLButtonAnzeigen -> context.getString(com.example.we2026_5.R.string.status_badge_a_plus_l_short)
            sollAButtonAnzeigen -> context.getString(com.example.we2026_5.R.string.status_badge_a_short)
            sollLButtonAnzeigen -> context.getString(com.example.we2026_5.R.string.status_badge_l_short)
            else -> ""
        }

        val isOverdueBadge = (hatKlassischUeberfaelligeAbholung || hatKlassischUeberfaelligeAuslieferung) && viewDateStart <= heuteStart

        val completedInfoText = when {
            !isDone -> ""
            viewDateStart > heuteStart -> ""
            else -> {
                val parts = mutableListOf<String>()
                if (customer.abholungErfolgt) {
                    val ts = if (customer.abholungZeitstempel > 0) DateFormatter.formatDateTime(customer.abholungZeitstempel)
                        else DateFormatter.formatDate(customer.abholungErledigtAm)
                    parts.add(context.getString(com.example.we2026_5.R.string.info_erledigt_a, ts))
                }
                if (customer.auslieferungErfolgt) {
                    val ts = if (customer.auslieferungZeitstempel > 0) DateFormatter.formatDateTime(customer.auslieferungZeitstempel)
                        else DateFormatter.formatDate(customer.auslieferungErledigtAm)
                    parts.add(context.getString(com.example.we2026_5.R.string.info_erledigt_l, ts))
                }
                parts.joinToString("\n")
            }
        }

        return ErledigungSheetState(
            showAbholung = showAbholungFinal,
            enableAbholung = enableA,
            showAuslieferung = showAuslieferungFinal,
            enableAuslieferung = enableL,
            showKw = showKwFinal,
            enableKw = enableKw,
            showVerschieben = !isDone && vButtonAktiv && !istImUrlaubAmTag && !nurInfoAmFaelligkeitstag,
            showUrlaub = !isDone && uButtonAktiv,
            showRueckgaengig = sollRueckgaengigAnzeigen,
            statusBadgeText = statusBadgeText,
            isOverdueBadge = isOverdueBadge,
            overdueInfoText = overdueInfoText,
            completedInfoText = completedInfoText
        )
    }

    /** Echt überfällig Abholung = Fälligkeit vor heute (terminStart < heuteStart), nicht erledigt. Für Badge „Ü“. */
    private fun hasKlassischUeberfaelligeAbholung(customer: Customer, viewDateStart: Long, heuteStart: Long): Boolean {
        val alleTermine = getAlleTermine(customer, viewDateStart - TimeUnit.DAYS.toMillis(365), 730)
        return alleTermine.any { termin ->
            termin.typ == TerminTyp.ABHOLUNG &&
                TerminFilterUtils.istUeberfaellig(termin.datum, heuteStart, customer.abholungErfolgt) &&
                TerminFilterUtils.sollUeberfaelligAnzeigen(termin.datum, viewDateStart, heuteStart)
        }
    }

    private fun hasKlassischUeberfaelligeAuslieferung(customer: Customer, viewDateStart: Long, heuteStart: Long): Boolean {
        val alleTermine = getAlleTermine(customer, viewDateStart - TimeUnit.DAYS.toMillis(365), 730)
        return alleTermine.any { termin ->
            termin.typ == TerminTyp.AUSLIEFERUNG &&
                TerminFilterUtils.istUeberfaellig(termin.datum, heuteStart, customer.auslieferungErfolgt) &&
                TerminFilterUtils.sollUeberfaelligAnzeigen(termin.datum, viewDateStart, heuteStart)
        }
    }

    /** Überfällig = Fälligkeit vor heute ODER (heute fällig und Anzeige ist Heute) und nicht erledigt. */
    private fun hasUeberfaelligeAbholung(customer: Customer, viewDateStart: Long, heuteStart: Long): Boolean {
        val klassischUeberfaellig = hasKlassischUeberfaelligeAbholung(customer, viewDateStart, heuteStart)
        val alleTermine = getAlleTermine(customer, viewDateStart - TimeUnit.DAYS.toMillis(365), 730)
        val heuteFaelligNochNichtErledigt = viewDateStart == heuteStart && !customer.abholungErfolgt &&
            alleTermine.any { termin ->
                termin.typ == TerminTyp.ABHOLUNG &&
                    TerminBerechnungUtils.getStartOfDay(termin.datum) == heuteStart
            }
        return klassischUeberfaellig || heuteFaelligNochNichtErledigt
    }

    private fun hasUeberfaelligeAuslieferung(customer: Customer, viewDateStart: Long, heuteStart: Long): Boolean {
        val klassischUeberfaellig = hasKlassischUeberfaelligeAuslieferung(customer, viewDateStart, heuteStart)
        val alleTermine = getAlleTermine(customer, viewDateStart - TimeUnit.DAYS.toMillis(365), 730)
        val heuteFaelligNochNichtErledigt = viewDateStart == heuteStart && !customer.auslieferungErfolgt &&
            alleTermine.any { termin ->
                termin.typ == TerminTyp.AUSLIEFERUNG &&
                    TerminBerechnungUtils.getStartOfDay(termin.datum) == heuteStart
            }
        return klassischUeberfaellig || heuteFaelligNochNichtErledigt
    }

}
