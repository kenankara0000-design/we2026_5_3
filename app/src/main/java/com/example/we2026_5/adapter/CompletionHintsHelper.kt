package com.example.we2026_5.adapter

import android.view.View
import com.example.we2026_5.Customer
import com.example.we2026_5.databinding.ItemCustomerBinding
import com.example.we2026_5.R
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.DateFormatter

/**
 * Helper für Erledigungs- und Überfällig-Hinweise auf der Kundenkarte.
 * Extrahiert aus CustomerViewHolderBinder.
 */
object CompletionHintsHelper {

    /**
     * Zeigt Erledigungs-Hinweise (Abholung/Auslieferung/KW) und Überfällig-Indikator an,
     * wenn am angezeigten Tag erledigt wurde oder der Tag "Heute" ist.
     */
    fun apply(
        binding: ItemCustomerBinding,
        customer: Customer,
        viewDateStart: Long,
        heuteStart: Long
    ) {
        val istHeute = viewDateStart == heuteStart
        val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt

        val abholungErledigtAmTag = customer.abholungErfolgt && customer.abholungErledigtAm > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.abholungErledigtAm) == viewDateStart
        val auslieferungErledigtAmTag = customer.auslieferungErfolgt && customer.auslieferungErledigtAm > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.auslieferungErledigtAm) == viewDateStart
        val warUeberfaelligAmTag = customer.faelligAmDatum > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.faelligAmDatum) == viewDateStart

        val sollHinweiseAnzeigen = abholungErledigtAmTag || auslieferungErledigtAmTag || warUeberfaelligAmTag || istHeute

        if (!sollHinweiseAnzeigen || !isDone) {
            binding.tvErledigungsHinweise.visibility = View.GONE
            binding.tvUeberfaelligIndikator.visibility = View.GONE
            return
        }

        if (isDone) {
            val hinweise = mutableListOf<String>()
            val warUeberfaellig = customer.faelligAmDatum > 0

            if (warUeberfaelligAmTag || (warUeberfaellig && istHeute)) {
                binding.tvUeberfaelligIndikator.visibility = View.VISIBLE
                binding.tvUeberfaelligIndikator.text = binding.root.context.getString(R.string.overdue_date_format, DateFormatter.formatDate(customer.faelligAmDatum))
            } else {
                binding.tvUeberfaelligIndikator.visibility = View.GONE
            }

            val sollAbholungHinweisAnzeigen = abholungErledigtAmTag ||
                (customer.abholungErfolgt && istHeute) ||
                (customer.abholungErfolgt && warUeberfaelligAmTag)
            val kwErledigtAmTagHinweis = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
                TerminBerechnungUtils.getStartOfDay(customer.keinerWäscheErledigtAm) == viewDateStart
            val ctx = binding.root.context
            if (kwErledigtAmTagHinweis) {
                hinweise.add(ctx.getString(R.string.hint_kw_done))
            }
            if (sollAbholungHinweisAnzeigen) {
                if (customer.abholungErfolgt && customer.abholungZeitstempel > 0) {
                    hinweise.add(ctx.getString(R.string.label_abholung_with_date, DateFormatter.formatDateTime(customer.abholungZeitstempel)))
                } else if (customer.abholungErfolgt && customer.abholungErledigtAm > 0) {
                    hinweise.add(ctx.getString(R.string.label_abholung_with_date, DateFormatter.formatDate(customer.abholungErledigtAm)))
                }
            }

            val sollAuslieferungHinweisAnzeigen = auslieferungErledigtAmTag ||
                (customer.auslieferungErfolgt && istHeute) ||
                (customer.auslieferungErfolgt && warUeberfaelligAmTag)
            if (sollAuslieferungHinweisAnzeigen) {
                if (customer.auslieferungErfolgt && customer.auslieferungZeitstempel > 0) {
                    hinweise.add(ctx.getString(R.string.label_auslieferung_with_date, DateFormatter.formatDateTime(customer.auslieferungZeitstempel)))
                } else if (customer.auslieferungErfolgt && customer.auslieferungErledigtAm > 0) {
                    hinweise.add(ctx.getString(R.string.label_auslieferung_with_date, DateFormatter.formatDate(customer.auslieferungErledigtAm)))
                }
            }

            if (hinweise.isNotEmpty()) {
                binding.tvErledigungsHinweise.text = hinweise.joinToString("\n")
                binding.tvErledigungsHinweise.visibility = View.VISIBLE
            } else {
                binding.tvErledigungsHinweise.visibility = View.GONE
            }
        } else {
            binding.tvErledigungsHinweise.visibility = View.GONE
            binding.tvUeberfaelligIndikator.visibility = View.GONE
        }
    }
}
