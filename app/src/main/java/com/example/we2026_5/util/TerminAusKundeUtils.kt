package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.TerminRegelTyp
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Erstellt ein CustomerIntervall aus Kundendaten (ohne TerminRegel).
 * Nutzt defaultAbholungWochentag, defaultAuslieferungWochentag, intervallTage.
 */
object TerminAusKundeUtils {

    /**
     * Erstellt ein CustomerIntervall aus den Kundendaten.
     * @param customer Kunde mit defaultAbholungWochentag, defaultAuslieferungWochentag, intervallTage
     * @param startDatum Startdatum (z.B. heute)
     * @return CustomerIntervall oder null wenn A/L-Tage fehlen
     */
    fun erstelleIntervallAusKunde(customer: Customer, startDatum: Long = System.currentTimeMillis()): CustomerIntervall? {
        val abholTag = customer.defaultAbholungWochentag.takeIf { WochentagBerechnung.isValidWeekday(it) } ?: return null
        val auslieferTag = customer.defaultAuslieferungWochentag.takeIf { WochentagBerechnung.isValidWeekday(it) } ?: return null
        val zyklus = (customer.intervalle.firstOrNull()?.intervallTage?.takeIf { it in 1..365 }
            ?: @Suppress("DEPRECATION") customer.intervallTage).coerceIn(1, 365)
        val start = TerminBerechnungUtils.getStartOfDay(startDatum)

        val abholungDatum = WochentagBerechnung.naechsterWochentagAb(start, abholTag)
        val auslieferungDatum = if (abholTag == auslieferTag) {
            abholungDatum + TimeUnit.DAYS.toMillis(zyklus.toLong())
        } else {
            WochentagBerechnung.naechsterWochentagAb(start, auslieferTag)
        }

        return CustomerIntervall(
            id = UUID.randomUUID().toString(),
            abholungDatum = abholungDatum,
            auslieferungDatum = auslieferungDatum,
            wiederholen = true,
            intervallTage = zyklus,
            intervallAnzahl = 0,
            erstelltAm = System.currentTimeMillis(),
            terminRegelId = "",
            regelTyp = TerminRegelTyp.WEEKLY,
            tourSlotId = customer.tourSlotId,
            zyklusTage = zyklus
        )
    }
}
