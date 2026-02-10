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
     * Bei gleichen A/L-Tagen: sameDayLStrategy (0 = L am selben Tag, 7 = L eine Woche später).
     * Bei unterschiedlichen A/L-Tagen: Zuordnung 1:1 (1. A → 1. L usw.).
     * @param customer Kunde mit defaultAbholungWochentage, defaultAuslieferungWochentage, sameDayLStrategy, intervallTage
     * @param startDatum Startdatum (z.B. heute)
     * @param tageAzuLOverride Tage A→L; nur verwendet wenn A- und L-Tage unterschiedlich und single. Sonst aus sameDayLStrategy oder 1:1 berechnet.
     * @param intervallTageOverride Wenn gesetzt, wird dieser Zyklus verwendet (z. B. beim Bearbeiten ohne bestehende Intervalle).
     * @return Liste von CustomerIntervall (ein Eintrag pro A-Tag), leer wenn keine gültigen A-Tage
     */
    fun erstelleIntervalleAusKunde(
        customer: Customer,
        startDatum: Long = System.currentTimeMillis(),
        tageAzuLOverride: Int = 7,
        intervallTageOverride: Int? = null
    ): List<CustomerIntervall> {
        val aTage = customer.effectiveAbholungWochentage.filter { WochentagBerechnung.isValidWeekday(it) }
        if (aTage.isEmpty()) return emptyList()
        val lTage = customer.effectiveAuslieferungWochentage.filter { WochentagBerechnung.isValidWeekday(it) }
        val useFallbackTageAzuL = lTage.isEmpty()
        if (useFallbackTageAzuL && aTage.isNotEmpty()) {
            val tageAL = tageAzuLOverride.coerceIn(0, 365)
            val zyklus = intervallTageOverride?.coerceIn(1, 365) ?: customer.intervallTageOrDefault(7)
            val start = TerminBerechnungUtils.getStartOfDay(startDatum)
            return aTage.map { abholTag ->
                val abholungDatum = WochentagBerechnung.naechsterWochentagAb(start, abholTag)
                CustomerIntervall(
                    id = UUID.randomUUID().toString(),
                    abholungDatum = abholungDatum,
                    auslieferungDatum = abholungDatum + TimeUnit.DAYS.toMillis(tageAL.toLong()),
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
        val zyklus = intervallTageOverride?.coerceIn(1, 365) ?: customer.intervallTageOrDefault(7)
        val start = TerminBerechnungUtils.getStartOfDay(startDatum)
        val sameSet = aTage == lTage
        val sameDayStrategy = (customer.sameDayLStrategy ?: 0).let { if (it == 7) 7 else 0 }

        return aTage.mapIndexed { index, abholTag ->
            val tageAL = when {
                sameSet -> sameDayStrategy
                else -> {
                    val lTag = lTage.getOrElse(index) { lTage.lastOrNull() ?: abholTag }
                    tageAzuLBetween(abholTag, lTag)
                }
            }
            val abholungDatum = WochentagBerechnung.naechsterWochentagAb(start, abholTag)
            val auslieferungDatum = abholungDatum + TimeUnit.DAYS.toMillis(tageAL.coerceIn(0, 365).toLong())
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

    /** Tage von A-Wochentag bis L-Wochentag (0–7). Bei gleichem Tag: 0. */
    private fun tageAzuLBetween(aTag: Int, lTag: Int): Int {
        if (aTag == lTag) return 0
        val d = (lTag - aTag + 7) % 7
        return if (d == 0) 7 else d
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
