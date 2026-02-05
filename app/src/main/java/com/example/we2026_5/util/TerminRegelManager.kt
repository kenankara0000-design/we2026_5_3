package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.TerminSlotVorschlag
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.TourSlot
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Erzeugt Slot-Vorschläge für Termin-Anlegen (Abholung/Auslieferung) basierend auf Kunde und Tour-Slots.
 */
object TerminRegelManager {

    private const val MAX_VORSCHLAEGE = 50

    fun schlageSlotsVor(
        kunde: Customer,
        tourSlots: List<TourSlot>,
        startDatum: Long = System.currentTimeMillis(),
        tageVoraus: Int = 30
    ): List<TerminSlotVorschlag> {
        val startOfDay = TerminBerechnungUtils.getStartOfDay(startDatum)
        val horizon = startOfDay + TimeUnit.DAYS.toMillis(tageVoraus.toLong())

        // Auf Abruf: nächste tageVoraus Tage, nur Mo–Fr (0–4), ein Slot pro Tag (A+L am selben Tag)
        if (kunde.kundenTyp == KundenTyp.AUF_ABRUF) {
            val vorschlaege = mutableListOf<TerminSlotVorschlag>()
            var datum = startOfDay
            while (datum <= horizon && vorschlaege.size < MAX_VORSCHLAEGE) {
                val cal = Calendar.getInstance().apply { timeInMillis = datum }
                val wochentag = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0=Mo .. 6=So
                if (wochentag in 0..4) {
                    vorschlaege += TerminSlotVorschlag(
                        datum = datum,
                        typ = TerminTyp.ABHOLUNG,
                        beschreibung = "${kunde.name} · A+L",
                        customerId = kunde.id,
                        customerName = kunde.name
                    )
                }
                datum += TimeUnit.DAYS.toMillis(1)
            }
            return vorschlaege
        }

        val relevanteSlotsBase = if (kunde.stadt.isNotBlank()) {
            tourSlots.filter { slot -> slot.wochentag in 0..6 && slot.stadt.equals(kunde.stadt, ignoreCase = true) }
        } else {
            tourSlots.filter { slot -> slot.wochentag in 0..6 }
        }
        val relevanteSlots = if (relevanteSlotsBase.isNotEmpty()) relevanteSlotsBase else tourSlots.filter { it.wochentag in 0..6 }
        val defaultAbhol = kunde.effectiveAbholungWochentage.firstOrNull()?.takeIf { WochentagBerechnung.isValidWeekday(it) }

        val vorschlaege = mutableListOf<TerminSlotVorschlag>()
        relevanteSlots.forEach { slot ->
            var datum = WochentagBerechnung.naechsterWochentagAb(startOfDay, slot.wochentag)
            while (datum <= horizon) {
                vorschlaege += TerminSlotVorschlag(
                    datum = datum,
                    typ = TerminTyp.ABHOLUNG,
                    beschreibung = listOfNotNull(
                        kunde.name,
                        slot.stadt.takeIf { it.isNotBlank() },
                        slot.zeitfenster?.let { "${it.start}-${it.ende}" }
                    ).joinToString(" · "),
                    tourSlotId = slot.id,
                    customerId = kunde.id,
                    customerName = kunde.name
                )
                datum += TimeUnit.DAYS.toMillis(7)
            }
        }

        defaultAbhol?.let { weekday ->
            var datum = WochentagBerechnung.naechsterWochentagAb(startOfDay, weekday)
            while (datum <= horizon) {
                vorschlaege += TerminSlotVorschlag(
                    datum = datum,
                    typ = TerminTyp.ABHOLUNG,
                    beschreibung = "${kunde.name} · ${TerminTyp.ABHOLUNG.name}",
                    customerId = kunde.id,
                    customerName = kunde.name
                )
                datum += TimeUnit.DAYS.toMillis(7)
            }
        }

        val auslieferungTag = kunde.effectiveAuslieferungWochentage.firstOrNull()?.takeIf { WochentagBerechnung.isValidWeekday(it) }
        auslieferungTag?.let { weekday ->
            var datum = WochentagBerechnung.naechsterWochentagAb(startOfDay, weekday)
            while (datum <= horizon) {
                vorschlaege += TerminSlotVorschlag(
                    datum = datum,
                    typ = TerminTyp.AUSLIEFERUNG,
                    beschreibung = "${kunde.name} · ${TerminTyp.AUSLIEFERUNG.name}",
                    customerId = kunde.id,
                    customerName = kunde.name
                )
                datum += TimeUnit.DAYS.toMillis(7)
            }
        }

        return vorschlaege.sortedBy { it.datum }.take(MAX_VORSCHLAEGE)
    }
}
