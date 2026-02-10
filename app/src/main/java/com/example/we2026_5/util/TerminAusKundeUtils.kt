package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.TerminRegelTyp
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Zentrale Abstraktion: erstes Intervall eines Kunden (Phase 3.1).
 * Alle Stellen, die „erstes Intervall“ oder abgeleitete Werte brauchen, nutzen diese Erweiterungen.
 */

/** Erstes Intervall des Kunden; null wenn keine Intervalle. */
fun Customer.firstIntervallOrNull(): CustomerIntervall? = intervalle.firstOrNull()

/** Tage A→L: zuerst gespeicherter Wert [tageAzuL], sonst aus erstem Intervall, sonst [default]. */
fun Customer.tageAzuLOrDefault(default: Int = 7): Int =
    (tageAzuL?.takeIf { it in 0..365 } ?: firstIntervallOrNull()?.let {
        if (it.abholungDatum > 0 && it.auslieferungDatum > 0)
            TimeUnit.MILLISECONDS.toDays(it.auslieferungDatum - it.abholungDatum).toInt().coerceIn(0, 365)
        else null
    }) ?: default

/** Intervall-Tage (Zyklus) aus erstem Intervall; sonst [default]. */
fun Customer.intervallTageOrDefault(default: Int = 7): Int =
    firstIntervallOrNull()?.intervallTage?.takeIf { it in 1..365 }?.coerceIn(1, 365) ?: default

/**
 * Erstellt ein CustomerIntervall aus Kundendaten (ohne TerminRegel).
 * Nutzt defaultAbholungWochentag, defaultAuslieferungWochentag, intervallTage.
 */
object TerminAusKundeUtils {

    /**
     * Erstellt CustomerIntervalle aus den Kundendaten – ein Intervall pro A-Wochentag.
     * Regelmäßig: L = A + tageAzuL, Zyklus = intervallTage (Tage bis zum nächsten A).
     * Mehrere A-Tage (z. B. Mo + Mi): je ein Intervall, damit alle Termine in der 365-Tage-Berechnung erscheinen.
     * @param customer Kunde mit defaultAbholungWochentag, intervallTage (Zyklus)
     * @param startDatum Startdatum (z.B. heute)
     * @param tageAzuL Tage zwischen Abholung und Auslieferung (nur bei REGELMAESSIG)
     * @param intervallTageOverride Wenn gesetzt, wird dieser Zyklus verwendet (z. B. beim Bearbeiten ohne bestehende Intervalle).
     * @return Liste von CustomerIntervall (ein Eintrag pro A-Tag), leer wenn keine gültigen A-Tage
     */
    fun erstelleIntervalleAusKunde(
        customer: Customer,
        startDatum: Long = System.currentTimeMillis(),
        tageAzuL: Int = 7,
        intervallTageOverride: Int? = null
    ): List<CustomerIntervall> {
        val aTage = customer.effectiveAbholungWochentage.filter { WochentagBerechnung.isValidWeekday(it) }
        if (aTage.isEmpty()) return emptyList()
        val zyklus = intervallTageOverride?.coerceIn(1, 365) ?: customer.intervallTageOrDefault(7)
        val tageAL = tageAzuL.coerceIn(0, 365)
        val start = TerminBerechnungUtils.getStartOfDay(startDatum)

        return aTage.map { abholTag ->
            val abholungDatum = WochentagBerechnung.naechsterWochentagAb(start, abholTag)
            val auslieferungDatum = abholungDatum + TimeUnit.DAYS.toMillis(tageAL.toLong())
            CustomerIntervall(
                id = UUID.randomUUID().toString(),
                abholungDatum = abholungDatum,
                auslieferungDatum = auslieferungDatum,
                wiederholen = true,
                intervallTage = zyklus,
                intervallAnzahl = 0,
                erstelltAm = start,
                terminRegelId = "",
                regelTyp = TerminRegelTyp.WEEKLY,
                tourSlotId = customer.tourSlotId,
                zyklusTage = zyklus
            )
        }
    }

    /**
     * @deprecated Verwende erstelleIntervalleAusKunde – Rückgabe für Abwärtskompatibilität (erstes Intervall).
     */
    fun erstelleIntervallAusKunde(
        customer: Customer,
        startDatum: Long = System.currentTimeMillis(),
        tageAzuL: Int = 7,
        intervallTageOverride: Int? = null
    ): CustomerIntervall? = erstelleIntervalleAusKunde(customer, startDatum, tageAzuL, intervallTageOverride).firstOrNull()
}
