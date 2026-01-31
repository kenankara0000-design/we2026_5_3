package com.example.we2026_5.adapter

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.databinding.ItemCustomerBinding
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.util.TerminInfo
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.tourplanner.ErledigungSheetState
import java.util.concurrent.TimeUnit

/**
 * Helper für Button-Sichtbarkeit und -Styling (A/L/KW/V/U/Ü/Rückgängig) auf der Kundenkarte.
 * Extrahiert aus CustomerViewHolderBinder.
 */
class CustomerButtonVisibilityHelper(
    private val context: Context,
    private val displayedDateMillis: Long?,
    private val pressedButtons: Map<String, String>,
    private val getAbholungDatum: ((Customer) -> Long)?,
    private val getAuslieferungDatum: ((Customer) -> Long)?,
    private val getAlleTermine: (Customer, Long, Int) -> List<TerminInfo>
) {

    fun apply(binding: ItemCustomerBinding, customer: Customer) {
        if (displayedDateMillis == null) {
            hideAllButtons(binding)
            return
        }
        val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
        val viewDateStart = TerminBerechnungUtils.getStartOfDay(displayedDateMillis)
        val pressedButton = pressedButtons[customer.id]

        // Überfällig-Bereich (Ü-Button ausgeblendet, aber A/L überfällig anzeigen)
        applyUeberfaelligSection(binding, customer, viewDateStart, heuteStart)

        // A (Abholung)
        val abholungDatumHeute = getAbholungDatum?.invoke(customer) ?: 0L
        val hatAbholungHeute = abholungDatumHeute > 0
        val hatUeberfaelligeAbholung = hasUeberfaelligeAbholung(customer, viewDateStart, heuteStart)
        val wurdeHeuteErledigt = customer.abholungErledigtAm > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.abholungErledigtAm) == viewDateStart
        val sollAButtonAnzeigen = hatAbholungHeute || hatUeberfaelligeAbholung || wurdeHeuteErledigt
        val istAmTatsaechlichenAbholungTag = hatAbholungHeute && !hatUeberfaelligeAbholung
        val istHeute = viewDateStart == heuteStart

        applyAbholungButton(binding, customer, sollAButtonAnzeigen, istHeute, istAmTatsaechlichenAbholungTag, hatUeberfaelligeAbholung, pressedButton)

        // L (Auslieferung)
        val auslieferungDatumHeute = getAuslieferungDatum?.invoke(customer) ?: 0L
        val hatAuslieferungHeute = auslieferungDatumHeute > 0
        val hatUeberfaelligeAuslieferung = hasUeberfaelligeAuslieferung(customer, viewDateStart, heuteStart)
        val wurdeAmTagErledigtL = customer.auslieferungErledigtAm > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.auslieferungErledigtAm) == viewDateStart
        val kwErledigtAmTag = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.keinerWäscheErledigtAm) == viewDateStart
        var sollLButtonAnzeigen = hatAuslieferungHeute || hatUeberfaelligeAuslieferung || wurdeAmTagErledigtL
        if (kwErledigtAmTag) sollLButtonAnzeigen = false
        val istAmTatsaechlichenAuslieferungTag = hatAuslieferungHeute && !hatUeberfaelligeAuslieferung

        applyAuslieferungButton(binding, customer, sollLButtonAnzeigen, istHeute, istAmTatsaechlichenAuslieferungTag, hatUeberfaelligeAuslieferung, pressedButton)

        // V (Verschieben)
        val hatVerschobenenTerminHeute = customer.verschobeneTermine.any { verschoben ->
            val originalStart = TerminBerechnungUtils.getStartOfDay(verschoben.originalDatum)
            val verschobenStart = TerminBerechnungUtils.getStartOfDay(verschoben.verschobenAufDatum)
            originalStart == viewDateStart || verschobenStart == viewDateStart
        }
        val vButtonAktiv = customer.verschobenAufDatum > 0 || hatVerschobenenTerminHeute
        val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTag

        // KW
        val sollKwButtonAnzeigen = sollAButtonAnzeigen || (hatAuslieferungHeute || hatUeberfaelligeAuslieferung || wurdeAmTagErledigtL)
        val wurdeKwHeuteErledigt = kwErledigtAmTag || (istHeute && pressedButton == "KW")
        applyKwButton(binding, sollKwButtonAnzeigen, istHeute, wurdeKwHeuteErledigt)

        binding.btnVerschieben.visibility = if (!isDone && vButtonAktiv) View.VISIBLE else View.GONE
        binding.btnVerschieben.background = ContextCompat.getDrawable(context, R.drawable.button_v_glossy)
        binding.btnVerschieben.setTextColor(
            if (vButtonAktiv) ContextCompat.getColor(context, R.color.white)
            else ContextCompat.getColor(context, R.color.white).let {
                Color.argb(128, Color.red(it), Color.green(it), Color.blue(it))
            }
        )

        // U (Urlaub)
        val hatTerminImUrlaub = if (customer.urlaubVon > 0 && customer.urlaubBis > 0) {
            val termine = getAlleTermine(customer, viewDateStart - TimeUnit.DAYS.toMillis(1), 2)
            termine.any { termin ->
                TerminBerechnungUtils.getStartOfDay(termin.datum) == viewDateStart &&
                    TerminFilterUtils.istTerminImUrlaub(termin.datum, customer.urlaubVon, customer.urlaubBis)
            }
        } else false
        val uButtonAktiv = customer.urlaubVon > 0 && customer.urlaubBis > 0 && hatTerminImUrlaub

        binding.btnUrlaub.visibility = if (!isDone && uButtonAktiv) View.VISIBLE else View.GONE
        binding.btnUrlaub.background = ContextCompat.getDrawable(context, R.drawable.button_u_glossy)
        binding.btnUrlaub.setTextColor(
            if (uButtonAktiv) ContextCompat.getColor(context, R.color.white)
            else ContextCompat.getColor(context, R.color.white).let {
                Color.argb(128, Color.red(it), Color.green(it), Color.blue(it))
            }
        )

        // Rückgängig
        val hatErledigtenATerminAmDatum = if (customer.abholungErfolgt) {
            val abholungErledigtAmStart = if (customer.abholungErledigtAm > 0) TerminBerechnungUtils.getStartOfDay(customer.abholungErledigtAm) else 0L
            if (abholungErledigtAmStart > 0 && viewDateStart == abholungErledigtAmStart) true
            else {
                val abholungDatumHeute = getAbholungDatum?.invoke(customer) ?: 0L
                abholungDatumHeute > 0 && TerminBerechnungUtils.getStartOfDay(abholungDatumHeute) == viewDateStart
            }
        } else false
        val hatErledigtenLTerminAmDatum = if (customer.auslieferungErfolgt) {
            val auslieferungErledigtAmStart = if (customer.auslieferungErledigtAm > 0) TerminBerechnungUtils.getStartOfDay(customer.auslieferungErledigtAm) else 0L
            if (auslieferungErledigtAmStart > 0 && viewDateStart == auslieferungErledigtAmStart) true
            else {
                val auslieferungDatumHeute = getAuslieferungDatum?.invoke(customer) ?: 0L
                auslieferungDatumHeute > 0 && TerminBerechnungUtils.getStartOfDay(auslieferungDatumHeute) == viewDateStart
            }
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

        binding.btnRueckgaengig.visibility = if (sollRueckgaengigAnzeigen) View.VISIBLE else View.GONE
    }

    /**
     * Berechnet den Zustand für das Erledigungs-Bottom-Sheet (für alle Kundenarten inkl. Listen).
     */
    fun getSheetState(customer: Customer): ErledigungSheetState? {
        if (displayedDateMillis == null) return null
        val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
        val viewDateStart = TerminBerechnungUtils.getStartOfDay(displayedDateMillis)
        val pressedButton = pressedButtons[customer.id]

        val abholungDatumHeute = getAbholungDatum?.invoke(customer) ?: 0L
        val hatAbholungHeute = abholungDatumHeute > 0
        val hatUeberfaelligeAbholung = hasUeberfaelligeAbholung(customer, viewDateStart, heuteStart)
        val wurdeHeuteErledigt = customer.abholungErledigtAm > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.abholungErledigtAm) == viewDateStart
        val sollAButtonAnzeigen = hatAbholungHeute || hatUeberfaelligeAbholung || wurdeHeuteErledigt
        val istAmTatsaechlichenAbholungTag = hatAbholungHeute && !hatUeberfaelligeAbholung
        val istHeute = viewDateStart == heuteStart

        val auslieferungDatumHeute = getAuslieferungDatum?.invoke(customer) ?: 0L
        val hatAuslieferungHeute = auslieferungDatumHeute > 0
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
        val vButtonAktiv = customer.verschobenAufDatum > 0 || hatVerschobenenTerminHeute
        val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTag

        val hatTerminImUrlaub = if (customer.urlaubVon > 0 && customer.urlaubBis > 0) {
            val termine = getAlleTermine(customer, viewDateStart - TimeUnit.DAYS.toMillis(1), 2)
            termine.any { t ->
                TerminBerechnungUtils.getStartOfDay(t.datum) == viewDateStart &&
                    TerminFilterUtils.istTerminImUrlaub(t.datum, customer.urlaubVon, customer.urlaubBis)
            }
        } else false
        val uButtonAktiv = customer.urlaubVon > 0 && customer.urlaubBis > 0 && hatTerminImUrlaub

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

        val enableA = sollAButtonAnzeigen && istHeute && !customer.abholungErfolgt && (istAmTatsaechlichenAbholungTag || hatUeberfaelligeAbholung)
        val enableL = sollLButtonAnzeigen && istHeute && customer.abholungErfolgt && !customer.auslieferungErfolgt && (istAmTatsaechlichenAuslieferungTag || hatUeberfaelligeAuslieferung)
        val enableKw = sollKwButtonAnzeigen && istHeute && !wurdeKwHeuteErledigt

        val statusBadgeText = when {
            (hatUeberfaelligeAbholung || hatUeberfaelligeAuslieferung) && viewDateStart <= heuteStart -> context.getString(com.example.we2026_5.R.string.status_badge_overdue)
            sollAButtonAnzeigen && sollLButtonAnzeigen -> context.getString(com.example.we2026_5.R.string.status_badge_a_plus_l)
            sollAButtonAnzeigen -> context.getString(com.example.we2026_5.R.string.status_badge_a)
            sollLButtonAnzeigen -> context.getString(com.example.we2026_5.R.string.status_badge_l)
            else -> ""
        }

        val isOverdueBadge = (hatUeberfaelligeAbholung || hatUeberfaelligeAuslieferung) && viewDateStart <= heuteStart
        return ErledigungSheetState(
            showAbholung = sollAButtonAnzeigen,
            enableAbholung = enableA,
            showAuslieferung = sollLButtonAnzeigen,
            enableAuslieferung = enableL,
            showKw = sollKwButtonAnzeigen,
            enableKw = enableKw,
            showVerschieben = !isDone && vButtonAktiv,
            showUrlaub = !isDone && uButtonAktiv,
            showRueckgaengig = sollRueckgaengigAnzeigen,
            statusBadgeText = statusBadgeText,
            isOverdueBadge = isOverdueBadge
        )
    }

    private fun hideAllButtons(binding: ItemCustomerBinding) {
        binding.btnAbholung.visibility = View.GONE
        binding.btnAbholung.isClickable = false
        binding.btnAuslieferung.visibility = View.GONE
        binding.btnAuslieferung.isClickable = false
        binding.btnKw.visibility = View.GONE
        binding.btnKw.isClickable = false
        binding.btnVerschieben.visibility = View.GONE
        binding.btnVerschieben.isClickable = false
        binding.btnUrlaub.visibility = View.GONE
        binding.btnUrlaub.isClickable = false
        binding.btnRueckgaengig.visibility = View.GONE
        binding.btnRueckgaengig.isClickable = false
    }

    private fun hasUeberfaelligeAbholung(customer: Customer, viewDateStart: Long, heuteStart: Long): Boolean {
        val alleTermine = getAlleTermine(customer, viewDateStart - TimeUnit.DAYS.toMillis(365), 730)
        return alleTermine.any { termin ->
            termin.typ == TerminTyp.ABHOLUNG &&
                TerminFilterUtils.istUeberfaellig(termin.datum, heuteStart, customer.abholungErfolgt) &&
                TerminFilterUtils.sollUeberfaelligAnzeigen(termin.datum, viewDateStart, heuteStart)
        }
    }

    private fun hasUeberfaelligeAuslieferung(customer: Customer, viewDateStart: Long, heuteStart: Long): Boolean {
        val alleTermine = getAlleTermine(customer, viewDateStart - TimeUnit.DAYS.toMillis(365), 730)
        return alleTermine.any { termin ->
            termin.typ == TerminTyp.AUSLIEFERUNG &&
                TerminFilterUtils.istUeberfaellig(termin.datum, heuteStart, customer.auslieferungErfolgt) &&
                TerminFilterUtils.sollUeberfaelligAnzeigen(termin.datum, viewDateStart, heuteStart)
        }
    }

    private fun applyUeberfaelligSection(binding: ItemCustomerBinding, customer: Customer, viewDateStart: Long, heuteStart: Long) {
        binding.btnUeberfaellig.visibility = View.GONE
        binding.arrowUeberfaellig.visibility = View.GONE

        val hatUeberfaelligeAbholungFuerUeButton = hasUeberfaelligeAbholungForUe(customer, viewDateStart, heuteStart)
        val hatUeberfaelligeAuslieferungFuerUeButton = hasUeberfaelligeAuslieferungForUe(customer, viewDateStart, heuteStart)
        val hatUeberfaelligeTermine = hatUeberfaelligeAbholungFuerUeButton || hatUeberfaelligeAuslieferungFuerUeButton
        val istZukunft = viewDateStart > heuteStart

        if (!istZukunft && hatUeberfaelligeTermine) {
            val ueberfaelligeTermineInfo = mutableListOf<String>()
            val ueberfaelligeDaten = mutableSetOf<Long>()
            val alleTermine = getAlleTermine(customer, viewDateStart - TimeUnit.DAYS.toMillis(365), 730)

            alleTermine.forEach { termin ->
                val terminStart = TerminBerechnungUtils.getStartOfDay(termin.datum)
                val istAmTagXFaellig = terminStart == viewDateStart
                val warVorHeuteFaellig = terminStart < heuteStart
                val istHeute = viewDateStart == heuteStart
                val istHeuteFaellig = terminStart == heuteStart
                val istUeberfaellig = (istAmTagXFaellig || (warVorHeuteFaellig && istHeute)) && !istHeuteFaellig

                if (istUeberfaellig) {
                    val terminTyp = when (termin.typ) {
                        TerminTyp.ABHOLUNG -> "A"
                        TerminTyp.AUSLIEFERUNG -> "L"
                        else -> ""
                    }
                    val istNichtErledigt = (termin.typ == TerminTyp.ABHOLUNG && !customer.abholungErfolgt) ||
                        (termin.typ == TerminTyp.AUSLIEFERUNG && !customer.auslieferungErfolgt)
                    if (terminTyp.isNotEmpty() && istNichtErledigt) {
                        ueberfaelligeTermineInfo.add(terminTyp)
                        ueberfaelligeDaten.add(terminStart)
                    }
                }
            }

            if (ueberfaelligeTermineInfo.isNotEmpty() && ueberfaelligeDaten.isNotEmpty()) {
                val terminTypenDistinct = ueberfaelligeTermineInfo.distinct()
                val hatA = terminTypenDistinct.contains("A")
                val hatL = terminTypenDistinct.contains("L")
                val aeltestesDatum = ueberfaelligeDaten.minOrNull() ?: 0L
                val datumStr = if (aeltestesDatum > 0) DateFormatter.formatDate(aeltestesDatum) else ""
                if (hatA) {
                    binding.btnUeberfaelligA.visibility = View.VISIBLE
                    binding.btnUeberfaelligA.background = ContextCompat.getDrawable(context, R.drawable.button_a_overdue)
                } else binding.btnUeberfaelligA.visibility = View.GONE
                if (hatL) {
                    binding.btnUeberfaelligL.visibility = View.VISIBLE
                    binding.btnUeberfaelligL.background = ContextCompat.getDrawable(context, R.drawable.button_l_overdue)
                } else binding.btnUeberfaelligL.visibility = View.GONE
                binding.tvUeberfaelligDatum.text = datumStr
                binding.tvUeberfaelligDatum.visibility = if (datumStr.isNotEmpty()) View.VISIBLE else View.GONE
            } else {
                binding.btnUeberfaelligA.visibility = View.GONE
                binding.btnUeberfaelligL.visibility = View.GONE
                binding.tvUeberfaelligDatum.visibility = View.GONE
            }
        } else {
            binding.btnUeberfaelligA.visibility = View.GONE
            binding.btnUeberfaelligL.visibility = View.GONE
            binding.tvUeberfaelligDatum.visibility = View.GONE
        }
    }

    private fun hasUeberfaelligeAbholungForUe(customer: Customer, viewDateStart: Long, heuteStart: Long): Boolean {
        val alleTermine = getAlleTermine(customer, viewDateStart - TimeUnit.DAYS.toMillis(365), 730)
        return alleTermine.any { termin ->
            val terminStart = TerminBerechnungUtils.getStartOfDay(termin.datum)
            val istAbholung = termin.typ == TerminTyp.ABHOLUNG
            val istNichtErledigt = !customer.abholungErfolgt
            val istAmTagXFaellig = terminStart == viewDateStart
            val warVorHeuteFaellig = terminStart < heuteStart
            val istHeute = viewDateStart == heuteStart
            val istHeuteFaellig = terminStart == heuteStart
            val istUeberfaellig = (istAmTagXFaellig || (warVorHeuteFaellig && istHeute)) && !istHeuteFaellig
            istAbholung && istNichtErledigt && istUeberfaellig
        }
    }

    private fun hasUeberfaelligeAuslieferungForUe(customer: Customer, viewDateStart: Long, heuteStart: Long): Boolean {
        val alleTermine = getAlleTermine(customer, viewDateStart - TimeUnit.DAYS.toMillis(365), 730)
        return alleTermine.any { termin ->
            val terminStart = TerminBerechnungUtils.getStartOfDay(termin.datum)
            val istAuslieferung = termin.typ == TerminTyp.AUSLIEFERUNG
            val istNichtErledigt = !customer.auslieferungErfolgt
            val istAmTagXFaellig = terminStart == viewDateStart
            val warVorHeuteFaellig = terminStart < heuteStart
            val istHeute = viewDateStart == heuteStart
            val istHeuteFaellig = terminStart == heuteStart
            val istUeberfaellig = (istAmTagXFaellig || (warVorHeuteFaellig && istHeute)) && !istHeuteFaellig
            istAuslieferung && istNichtErledigt && istUeberfaellig
        }
    }

    private fun applyAbholungButton(
        binding: ItemCustomerBinding,
        customer: Customer,
        sollAnzeigen: Boolean,
        istHeute: Boolean,
        istAmTatsaechlichenAbholungTag: Boolean,
        hatUeberfaelligeAbholung: Boolean,
        pressedButton: String?
    ) {
        binding.btnAbholung.visibility = if (sollAnzeigen) View.VISIBLE else View.GONE
        binding.btnAbholung.isClickable = istHeute
        val wurdeHeuteErledigtA = istHeute && customer.abholungErledigtAm > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.abholungErledigtAm) == TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
        if (wurdeHeuteErledigtA || pressedButton == "A") {
            binding.btnAbholung.background = ContextCompat.getDrawable(context, R.drawable.button_gray)
            binding.btnAbholung.setTextColor(ContextCompat.getColor(context, R.color.white))
            binding.btnAbholung.alpha = 0.7f
        } else {
            binding.btnAbholung.background = ContextCompat.getDrawable(context, R.drawable.button_a_glossy)
            if (istAmTatsaechlichenAbholungTag) {
                binding.btnAbholung.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.btnAbholung.alpha = 1.0f
            } else if (hatUeberfaelligeAbholung) {
                binding.btnAbholung.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.btnAbholung.alpha = 0.9f
            } else {
                binding.btnAbholung.setTextColor(
                    ContextCompat.getColor(context, R.color.white).let {
                        Color.argb(128, Color.red(it), Color.green(it), Color.blue(it))
                    }
                )
                binding.btnAbholung.alpha = 1.0f
            }
        }
    }

    private fun applyAuslieferungButton(
        binding: ItemCustomerBinding,
        customer: Customer,
        sollAnzeigen: Boolean,
        istHeute: Boolean,
        istAmTatsaechlichenAuslieferungTag: Boolean,
        hatUeberfaelligeAuslieferung: Boolean,
        pressedButton: String?
    ) {
        binding.btnAuslieferung.visibility = if (sollAnzeigen) View.VISIBLE else View.GONE
        binding.btnAuslieferung.isClickable = istHeute
        val wurdeHeuteErledigtL = istHeute && customer.auslieferungErledigtAm > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.auslieferungErledigtAm) == TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
        if (wurdeHeuteErledigtL || pressedButton == "L") {
            binding.btnAuslieferung.background = ContextCompat.getDrawable(context, R.drawable.button_gray)
            binding.btnAuslieferung.setTextColor(ContextCompat.getColor(context, R.color.white))
            binding.btnAuslieferung.alpha = 0.7f
        } else {
            binding.btnAuslieferung.background = ContextCompat.getDrawable(context, R.drawable.button_l_glossy)
            if (istAmTatsaechlichenAuslieferungTag) {
                binding.btnAuslieferung.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.btnAuslieferung.alpha = 1.0f
            } else if (hatUeberfaelligeAuslieferung) {
                binding.btnAuslieferung.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.btnAuslieferung.alpha = 0.9f
            } else {
                binding.btnAuslieferung.setTextColor(
                    ContextCompat.getColor(context, R.color.white).let {
                        Color.argb(128, Color.red(it), Color.green(it), Color.blue(it))
                    }
                )
                binding.btnAuslieferung.alpha = 1.0f
            }
        }
    }

    private fun applyKwButton(binding: ItemCustomerBinding, sollAnzeigen: Boolean, istHeute: Boolean, wurdeKwHeuteErledigt: Boolean) {
        binding.btnKw.visibility = if (sollAnzeigen) View.VISIBLE else View.GONE
        binding.btnKw.isClickable = istHeute
        if (wurdeKwHeuteErledigt) {
            binding.btnKw.background = ContextCompat.getDrawable(context, R.drawable.button_gray)
            binding.btnKw.setTextColor(ContextCompat.getColor(context, R.color.white))
            binding.btnKw.alpha = 0.7f
        } else {
            binding.btnKw.background = ContextCompat.getDrawable(context, R.drawable.button_kw_glossy)
            binding.btnKw.setTextColor(ContextCompat.getColor(context, R.color.white))
            binding.btnKw.alpha = 1.0f
        }
    }
}
