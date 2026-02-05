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
     * Regelmäßig: L = A + tageAzuL, Zyklus = intervallTage (Tage bis zum nächsten A).
     * @param customer Kunde mit defaultAbholungWochentag, intervallTage (Zyklus)
     * @param startDatum Startdatum (z.B. heute)
     * @param tageAzuL Tage zwischen Abholung und Auslieferung (nur bei REGELMAESSIG)
     * @return CustomerIntervall oder null wenn A-Tag fehlt
     */
    fun erstelleIntervallAusKunde(
        customer: Customer,
        startDatum: Long = System.currentTimeMillis(),
        tageAzuL: Int = 7
    ): CustomerIntervall? {
        val abholTag = customer.effectiveAbholungWochentage.firstOrNull()?.takeIf { WochentagBerechnung.isValidWeekday(it) } ?: return null
        val zyklus = (customer.intervalle.firstOrNull()?.intervallTage?.takeIf { it in 1..365 }
            ?: @Suppress("DEPRECATION") customer.intervallTage).coerceIn(1, 365)
        val tageAL = tageAzuL.coerceIn(0, 365)
        val start = TerminBerechnungUtils.getStartOfDay(startDatum)

        val abholungDatum = WochentagBerechnung.naechsterWochentagAb(start, abholTag)
        val auslieferungDatum = abholungDatum + TimeUnit.DAYS.toMillis(tageAL.toLong())

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
