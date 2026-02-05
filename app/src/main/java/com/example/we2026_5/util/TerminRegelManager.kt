package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.CustomerStatus
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.TerminRegel
import com.example.we2026_5.TerminRegelTyp
import com.example.we2026_5.TerminSlotVorschlag
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.TourSlot
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Manager für die Anwendung von Termin-Regeln auf Kunden und Listen.
 * Unterstützt mehrere Abhol- und Auslieferungswochentage (paarweise) sowie flexible Zyklen.
 */
object TerminRegelManager {

    private const val TAEGLICH_ANZAHL_TAGE = 365
    private const val MAX_VORSCHLAEGE = 50

    fun wendeRegelAufKundeAn(regel: TerminRegel, customer: Customer): List<CustomerIntervall> {
        if (!regel.aktiv && regel.regelTyp != TerminRegelTyp.ADHOC) return emptyList()
        if (customer.status == CustomerStatus.PAUSIERT && customer.pauseEnde == 0L) return emptyList()
        if (regel.regelTyp == TerminRegelTyp.ADHOC) return emptyList()
        if (regel.taeglich) return wendeTaeglichAufKundeAn(regel, customer)

        val abholTage = resolveAbholtage(regel, customer)
        val auslieferTage = resolveAuslieferungstage(regel, customer)
        val startDatum = resolveStartDatum(regel, customer)
        val zyklusTage = resolveZyklus(regel)
        val wiederholen = regel.wiederholen || regel.regelTyp == TerminRegelTyp.WEEKLY || regel.regelTyp == TerminRegelTyp.FLEXIBLE_CYCLE

        if (abholTage.isEmpty() || auslieferTage.isEmpty()) {
            val fallbackAbholung = if (regel.abholungDatum > 0) regel.abholungDatum else startDatum
            val fallbackAuslieferung = if (regel.auslieferungDatum > 0) regel.auslieferungDatum else startDatum
            return listOf(
                baseCustomerIntervall(
                    abholungDatum = fallbackAbholung,
                    auslieferungDatum = fallbackAuslieferung,
                    wiederholen = wiederholen,
                    intervallTage = zyklusTage,
                    regel = regel,
                    customer = customer
                )
            )
        }

        val count = minOf(abholTage.size, auslieferTage.size)
        return (0 until count).map { index ->
            val abholungDatum = WochentagBerechnung.naechsterWochentagAb(startDatum, abholTage[index])
            val auslieferungDatum = if (abholTage[index] == auslieferTage[index]) {
                abholungDatum + TimeUnit.DAYS.toMillis(zyklusTage.toLong())
            } else {
                WochentagBerechnung.naechsterWochentagAb(startDatum, auslieferTage[index])
            }
            baseCustomerIntervall(
                abholungDatum = abholungDatum,
                auslieferungDatum = auslieferungDatum,
                wiederholen = wiederholen,
                intervallTage = zyklusTage,
                regel = regel,
                customer = customer
            )
        }
    }

    fun generiereSerienVorschau(
        regel: TerminRegel,
        customer: Customer,
        tageVoraus: Int = 60
    ): List<TerminSlotVorschlag> {
        val intervalle = wendeRegelAufKundeAn(regel, customer)
        val termine = intervalle.flatMap {
            TerminBerechnungUtils.berechneTermineFuerIntervall(
                intervall = it,
                startDatum = System.currentTimeMillis(),
                tageVoraus = tageVoraus,
                geloeschteTermine = customer.geloeschteTermine,
                verschobeneTermine = customer.verschobeneTermine
            )
        }.sortedBy { it.datum }
        return termine.take(MAX_VORSCHLAEGE).map {
            TerminSlotVorschlag(
                datum = it.datum,
                typ = it.typ,
                beschreibung = "${customer.name} (${regel.name})",
                tourSlotId = intervalle.firstOrNull { intervall -> intervall.id == it.intervallId }?.tourSlotId,
                customerId = customer.id,
                customerName = customer.name
            )
        }
    }

    fun schlageSlotsVor(
        kunde: Customer,
        regel: TerminRegel?,
        tourSlots: List<TourSlot>,
        startDatum: Long = System.currentTimeMillis(),
        tageVoraus: Int = 30
    ): List<TerminSlotVorschlag> {
        val startOfDay = TerminBerechnungUtils.getStartOfDay(startDatum)
        val horizon = startOfDay + TimeUnit.DAYS.toMillis(tageVoraus.toLong())

        val relevanteSlotsBase = if (kunde.stadt.isNotBlank()) {
            tourSlots.filter { slot -> slot.wochentag in 0..6 && slot.stadt.equals(kunde.stadt, ignoreCase = true) }
        } else {
            tourSlots.filter { slot -> slot.wochentag in 0..6 }
        }
        val relevanteSlots = if (relevanteSlotsBase.isNotEmpty()) relevanteSlotsBase else tourSlots.filter { it.wochentag in 0..6 }
        val defaultAbhol = kunde.defaultAbholungWochentag.takeIf { WochentagBerechnung.isValidWeekday(it) }

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

        val auslieferungTag = kunde.defaultAuslieferungWochentag.takeIf { WochentagBerechnung.isValidWeekday(it) }
            ?: regel?.auslieferungWochentage?.firstOrNull()
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

    fun wendeRegelAufListeAn(regel: TerminRegel, liste: KundenListe): List<ListeIntervall> {
        if (regel.taeglich) return wendeTaeglichAufListeAn(regel)
        val abholTage = resolveAbholtage(regel, null)
        val auslieferTage = resolveAuslieferungstage(regel, null)
        val startDatum = resolveStartDatum(regel, null)
        val zyklusTage = resolveZyklus(regel)

        if (abholTage.isEmpty() || auslieferTage.isEmpty()) {
            return listOf(
                ListeIntervall(
                    abholungDatum = regel.abholungDatum.takeIf { it > 0 } ?: startDatum,
                    auslieferungDatum = regel.auslieferungDatum.takeIf { it > 0 } ?: startDatum,
                    wiederholen = regel.wiederholen,
                    intervallTage = zyklusTage,
                    intervallAnzahl = regel.intervallAnzahl
                )
            )
        }

        val count = minOf(abholTage.size, auslieferTage.size)
        return (0 until count).map { index ->
            val abholungDatum = WochentagBerechnung.naechsterWochentagAb(startDatum, abholTage[index])
            val auslieferungDatum = if (abholTage[index] == auslieferTage[index]) {
                abholungDatum + TimeUnit.DAYS.toMillis(zyklusTage.toLong())
            } else {
                WochentagBerechnung.naechsterWochentagAb(startDatum, auslieferTage[index])
            }
            ListeIntervall(
                abholungDatum = abholungDatum,
                auslieferungDatum = auslieferungDatum,
                wiederholen = true,
                intervallTage = zyklusTage,
                intervallAnzahl = regel.intervallAnzahl
            )
        }
    }

    private fun wendeTaeglichAufKundeAn(regel: TerminRegel, customer: Customer): List<CustomerIntervall> {
        val startDatum = resolveStartDatum(regel, customer)
        val cal = Calendar.getInstance().apply {
            timeInMillis = startDatum
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (0 until TAEGLICH_ANZAHL_TAGE).map {
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            baseCustomerIntervall(
                abholungDatum = dayStart,
                auslieferungDatum = dayStart,
                wiederholen = false,
                intervallTage = 0,
                regel = regel,
                customer = customer
            )
        }
    }

    private fun wendeTaeglichAufListeAn(regel: TerminRegel): List<ListeIntervall> {
        val startDatum = resolveStartDatum(regel, null)
        val cal = Calendar.getInstance().apply {
            timeInMillis = startDatum
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (0 until TAEGLICH_ANZAHL_TAGE).map {
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            ListeIntervall(
                abholungDatum = dayStart,
                auslieferungDatum = dayStart,
                wiederholen = false,
                intervallTage = 0,
                intervallAnzahl = 0
            )
        }
    }

    private fun baseCustomerIntervall(
        abholungDatum: Long,
        auslieferungDatum: Long,
        wiederholen: Boolean,
        intervallTage: Int,
        regel: TerminRegel,
        customer: Customer
    ) = CustomerIntervall(
        id = java.util.UUID.randomUUID().toString(),
        abholungDatum = abholungDatum,
        auslieferungDatum = auslieferungDatum,
        wiederholen = wiederholen,
        intervallTage = intervallTage,
        intervallAnzahl = regel.intervallAnzahl,
        erstelltAm = System.currentTimeMillis(),
        terminRegelId = regel.id,
        regelTyp = regel.regelTyp,
        tourSlotId = regel.tourSlotId.ifEmpty { customer.tourSlotId },
        zyklusTage = intervallTage
    )

    private fun resolveAbholtage(regel: TerminRegel, customer: Customer?): List<Int> {
        val regelTage = regel.abholungWochentage?.filter { WochentagBerechnung.isValidWeekday(it) }?.distinct()?.sorted().orEmpty()
        if (regelTage.isNotEmpty()) return regelTage
        if (WochentagBerechnung.isValidWeekday(regel.abholungWochentag)) return listOf(regel.abholungWochentag)
        val customerTag = customer?.defaultAbholungWochentag?.takeIf { WochentagBerechnung.isValidWeekday(it) }
        return customerTag?.let { listOf(it) } ?: emptyList()
    }

    private fun resolveAuslieferungstage(regel: TerminRegel, customer: Customer?): List<Int> {
        val regelTage = regel.auslieferungWochentage?.filter { WochentagBerechnung.isValidWeekday(it) }?.distinct()?.sorted().orEmpty()
        if (regelTage.isNotEmpty()) return regelTage
        if (WochentagBerechnung.isValidWeekday(regel.auslieferungWochentag)) return listOf(regel.auslieferungWochentag)
        val customerTag = customer?.defaultAuslieferungWochentag?.takeIf { WochentagBerechnung.isValidWeekday(it) }
        return customerTag?.let { listOf(it) } ?: emptyList()
    }

    private fun resolveStartDatum(regel: TerminRegel, customer: Customer?): Long {
        val basis = if (regel.startDatum > 0) regel.startDatum else TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
        val pauseCandidate = sequenceOf(
            regel.pauseEnde,
            customer?.pauseEnde,
            customer?.reaktivierungsDatum
        ).filterNotNull().filter { it > 0 }.maxOrNull()
        return maxOf(basis, pauseCandidate ?: basis)
    }

    private fun resolveZyklus(regel: TerminRegel): Int {
        val candidate = when (regel.regelTyp) {
            TerminRegelTyp.FLEXIBLE_CYCLE -> regel.zyklusTage
            else -> regel.intervallTage
        }
        return candidate.coerceIn(1, 365)
    }

}
