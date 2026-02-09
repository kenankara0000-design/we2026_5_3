package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminInfo
import com.example.we2026_5.util.tageAzuLOrDefault
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Cache für berechnete Termine pro Kunde (nächste 365 Tage).
 * Wird bei Kunden-Änderung invalidiert, um doppelte Berechnung zu vermeiden.
 */
class TerminCache {

    private data class CachedEntry(
        val windowStartDay: Long,
        val termine: List<TerminInfo>
    )

    private val cache = ConcurrentHashMap<String, CachedEntry>()

    /**
     * Liefert die Termine für die nächsten 365 Tage ab heute.
     * Wenn liste angegeben und customer in der Liste: listenTermine werden einbezogen.
     * Cache nur wenn liste == null (Listen-Änderungen würden sonst veraltete Daten liefern).
     */
    fun getTermine365(customer: Customer, liste: KundenListe? = null): List<TerminInfo> {
        val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
        val effectiveListe = if (liste != null && customer.listeId == liste.id) liste else null
        if (effectiveListe == null) {
            val cached = cache[customer.id]
            if (cached != null && cached.windowStartDay == heuteStart) return cached.termine
        }
        val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
            customer = customer,
            liste = effectiveListe,
            startDatum = heuteStart,
            tageVoraus = 365
        )
        if (effectiveListe == null) cache[customer.id] = CachedEntry(heuteStart, termine)
        return termine
    }

    /**
     * Liefert Termine gefiltert auf den angegebenen Zeitraum.
     * liste: optional, für Listen-Kunden (listenTermine werden einbezogen).
     */
    fun getTermineInRange(
        customer: Customer,
        startDatum: Long,
        tageVoraus: Int,
        liste: KundenListe? = null
    ): List<TerminInfo> {
        val termine = getTermine365(customer, liste)
        val start = TerminBerechnungUtils.getStartOfDay(startDatum)
        val end = start + TimeUnit.DAYS.toMillis(tageVoraus.toLong())
        return termine.filter { it.datum in start..end }
    }

    /** Invalidiert den Cache für einen Kunden (nach Änderung). */
    fun invalidate(customerId: String) {
        cache.remove(customerId)
    }

    /** Invalidiert den gesamten Cache (z. B. bei Listenwechsel). */
    fun invalidateAll() {
        cache.clear()
    }

    /**
     * Liefert A/L-Paare für die nächsten 365 Tage (für Termine-Tab).
     * Merge: berechnete Termine (Intervall/Wochentag) + kundenTermine + ausnahmeTermine.
     */
    fun getTerminePairs365(customer: Customer, liste: KundenListe? = null): List<Pair<Long, Long>> {
        val tageAzuL = customer.tageAzuLOrDefault(7)
        val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
        val endMillis = heuteStart + TimeUnit.DAYS.toMillis(365)

        val pairs = mutableSetOf<Pair<Long, Long>>()

        // Berechnete Termine (Intervall, Wochentag, listenTermine)
        val computed = getTermine365(customer, liste)
        computed
            .filter { it.typ == TerminTyp.ABHOLUNG && it.datum >= heuteStart && it.datum <= endMillis }
            .forEach { a ->
                val lDatum = TerminBerechnungUtils.getStartOfDay(a.datum + TimeUnit.DAYS.toMillis(tageAzuL.toLong()))
                pairs.add(Pair(a.datum, lDatum))
            }

        // Kunden-Termine
        customer.kundenTermine
            .filter { it.typ == "A" && it.datum >= heuteStart && it.datum <= endMillis }
            .forEach { a ->
                val lDatum = TerminBerechnungUtils.getStartOfDay(a.datum + TimeUnit.DAYS.toMillis(tageAzuL.toLong()))
                pairs.add(Pair(a.datum, lDatum))
            }
        // Ausnahme-Termine
        customer.ausnahmeTermine
            .filter { it.typ == "A" && it.datum >= heuteStart && it.datum <= endMillis }
            .forEach { a ->
                val lDatum = TerminBerechnungUtils.getStartOfDay(a.datum + TimeUnit.DAYS.toMillis(tageAzuL.toLong()))
                pairs.add(Pair(a.datum, lDatum))
            }

        return pairs.sortedBy { it.first }
    }
}
