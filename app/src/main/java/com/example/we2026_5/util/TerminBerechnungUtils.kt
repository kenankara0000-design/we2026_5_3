package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.KundenListe
import com.example.we2026_5.TerminRegelTyp
import com.example.we2026_5.util.tageAzuLOrDefault
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.VerschobenerTermin
import com.example.we2026_5.TerminTyp
import java.util.Calendar
import com.example.we2026_5.util.TerminFilterUtils
import com.example.we2026_5.util.AgentDebugLog
import java.util.concurrent.TimeUnit

/**
 * Utility-Klasse für Termin-Berechnungen
 * Unterstützt sowohl alte (einzelne Felder) als auch neue (intervalle-Liste) Struktur
 */
object TerminBerechnungUtils {

    /**
     * Normalisiert ein Datum auf Tagesanfang (00:00:00)
     */
    fun getStartOfDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    
    // Filter-Funktionen entfernt - jetzt in TerminFilterUtils
    
    /**
     * Berechnet alle Termine für einen Zeitraum (365 Tage) für ein CustomerIntervall.
     * Bei MONTHLY_WEEKDAY werden customer (für tageAzuL und erstelltAm) benötigt; sonst L = A, kein Start-Filter.
     */
    fun berechneTermineFuerIntervall(
        intervall: CustomerIntervall,
        startDatum: Long = System.currentTimeMillis(),
        tageVoraus: Int = 365,
        geloeschteTermine: List<Long> = emptyList(),
        verschobeneTermine: List<VerschobenerTermin> = emptyList(),
        customer: Customer? = null
    ): List<TerminInfo> {
        val termine = mutableListOf<TerminInfo>()
        val startDatumStart = getStartOfDay(startDatum)
        val endDatum = startDatumStart + TimeUnit.DAYS.toMillis(tageVoraus.toLong())

        if (intervall.regelTyp == TerminRegelTyp.MONTHLY_WEEKDAY &&
            intervall.monthWeekOfMonth in 1..5 && intervall.monthWeekday in 0..6) {
            val tageAzuL = customer?.tageAzuLOrDefault(7) ?: 0
            val kundeStart = if (customer != null && customer.erstelltAm > 0) getStartOfDay(customer.erstelltAm) else 0L
            val intervallStart = if (intervall.erstelltAm > 0) getStartOfDay(intervall.erstelltAm) else 0L
            val startDatumMin = maxOf(
                startDatumStart,
                kundeStart,
                intervallStart
            )
            val monthly = berechneMonatlicheWochentagTermine(
                intervall = intervall,
                startDatumStart = startDatumStart,
                endDatum = endDatum,
                startDatumMin = startDatumMin,
                tageAzuL = tageAzuL,
                geloeschteTermine = geloeschteTermine,
                verschobeneTermine = verschobeneTermine
            )
            return monthly.sortedBy { it.datum }
        }
        
        // Abholungstermine
        if (intervall.abholungDatum > 0) {
            val abholungTermine = berechneWiederholendeTermine(
                startDatum = intervall.abholungDatum,
                wiederholen = intervall.wiederholen,
                intervallTage = intervall.intervallTage,
                intervallAnzahl = intervall.intervallAnzahl,
                startDatumStart = startDatumStart,
                endDatum = endDatum,
                geloeschteTermine = geloeschteTermine,
                verschobeneTermine = verschobeneTermine.filter { it.typ == TerminTyp.ABHOLUNG },
                intervallId = intervall.id,
                typ = TerminTyp.ABHOLUNG
            )
            termine.addAll(abholungTermine)
        }
        
        // Auslieferungstermine
        if (intervall.auslieferungDatum > 0) {
            val auslieferungTermine = berechneWiederholendeTermine(
                startDatum = intervall.auslieferungDatum,
                wiederholen = intervall.wiederholen,
                intervallTage = intervall.intervallTage,
                intervallAnzahl = intervall.intervallAnzahl,
                startDatumStart = startDatumStart,
                endDatum = endDatum,
                geloeschteTermine = geloeschteTermine,
                verschobeneTermine = verschobeneTermine.filter { it.typ == TerminTyp.AUSLIEFERUNG },
                intervallId = intervall.id,
                typ = TerminTyp.AUSLIEFERUNG
            )
            termine.addAll(auslieferungTermine)
        }
        
        return termine.sortedBy { it.datum }
    }
    
    /**
     * Berechnet wiederholende Termine für ein Intervall
     */
    private fun berechneWiederholendeTermine(
        startDatum: Long,
        wiederholen: Boolean,
        intervallTage: Int,
        intervallAnzahl: Int = 0, // 0 = unbegrenzt
        startDatumStart: Long,
        endDatum: Long,
        geloeschteTermine: List<Long>,
        verschobeneTermine: List<VerschobenerTermin>,
        intervallId: String,
        typ: TerminTyp
    ): List<TerminInfo> {
        val termine = mutableListOf<TerminInfo>()
        val startDatumStartNormalized = getStartOfDay(startDatum)
        
        if (!wiederholen) {
            // Einmaliger Termin
            if (startDatumStartNormalized >= startDatumStart && startDatumStartNormalized <= endDatum) {
                // Prüfe ob verschoben
                val verschoben = TerminFilterUtils.istTerminVerschoben(startDatumStartNormalized, verschobeneTermine, intervallId)
                val finalDatum = verschoben?.verschobenAufDatum ?: startDatumStartNormalized
                
                // Prüfe ob gelöscht
                if (!TerminFilterUtils.istTerminGeloescht(finalDatum, geloeschteTermine)) {
                    termine.add(TerminInfo(
                        datum = finalDatum,
                        typ = typ,
                        intervallId = intervallId,
                        verschoben = verschoben != null,
                        originalDatum = if (verschoben != null) startDatumStartNormalized else null
                    ))
                }
            }
        } else {
            // Wiederholender Termin
            var aktuellesDatum = startDatumStartNormalized
            val intervallTageSafe = intervallTage.coerceIn(1, 365)
            var versuche = 0
            val maxVersuche = 1000 // Sicherheit gegen Endlosschleifen
            var durchgefuehrteAnzahl = 0 // Zähler für durchgeführte Wiederholungen (nur nicht-gelöschte)
            
            // Wenn intervallAnzahl > 0, dann maxVersuche auf intervallAnzahl begrenzen
            // Beachte: intervallAnzahl zählt nur nicht-gelöschte Termine
            val maxWiederholungen = if (intervallAnzahl > 0) intervallAnzahl else Int.MAX_VALUE
            
            while (aktuellesDatum <= endDatum && versuche < maxVersuche && durchgefuehrteAnzahl < maxWiederholungen) {
                val originalAktuellesDatum = aktuellesDatum // Speichern für finalDatum
                // Prüfe ob verschoben
                val verschoben = TerminFilterUtils.istTerminVerschoben(originalAktuellesDatum, verschobeneTermine, intervallId)
                val finalDatum = verschoben?.verschobenAufDatum ?: originalAktuellesDatum
                
                // Prüfe ob gelöscht
                if (!TerminFilterUtils.istTerminGeloescht(finalDatum, geloeschteTermine)) {
                    if (finalDatum >= startDatumStart && finalDatum <= endDatum) {
                        termine.add(TerminInfo(
                            datum = finalDatum,
                            typ = typ,
                            intervallId = intervallId,
                            verschoben = verschoben != null,
                            originalDatum = if (verschoben != null) originalAktuellesDatum else null
                        ))
                        durchgefuehrteAnzahl++ // Zähle nur nicht-gelöschte Termine
                    }
                }
                
                // Nächstes Datum berechnen
                aktuellesDatum += TimeUnit.DAYS.toMillis(intervallTageSafe.toLong())
                versuche++
            }
        }
        
        return termine
    }
    
    /**
     * Liefert Wochentag 0=Mo..6=So für ein Datum (Tagesanfang).
     */
    private fun getWochentagForDay(dayStart: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dayStart
        return (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    }

    /** Calendar.DAY_OF_WEEK: 1=So, 2=Mo, ..., 7=Sa. Unser weekday 0=Mo, 6=So. */
    private fun weekdayToCalendar(weekday: Int): Int =
        if (weekday == 6) Calendar.SUNDAY else (weekday + 2)

    /**
     * Tag im Monat (1..31) für den n-ten Wochentag (1=erste, 2=zweite, ..., 5=letzte).
     * Liefert 0 wenn ungültig.
     */
    private fun getNthWeekdayDayInMonth(cal: Calendar, year: Int, month: Int, weekOfMonth: Int, weekday: Int): Int {
        cal.set(year, month, 1)
        val targetDow = weekdayToCalendar(weekday)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        var count = 0
        var lastMatch = 0
        for (day in 1..maxDay) {
            cal.set(year, month, day)
            if (cal.get(Calendar.DAY_OF_WEEK) == targetDow) {
                count++
                lastMatch = day
                if (weekOfMonth in 1..4 && count == weekOfMonth) return day
            }
        }
        return if (weekOfMonth == 5) lastMatch else 0
    }

    /**
     * Termine für MONTHLY_WEEKDAY: pro Monat ein A-Datum (n-ter Wochentag), L = A + tageAzuL.
     * A und L werden nur erzeugt, wenn sie >= startDatumMin (z. B. erstelltAm des Kunden).
     */
    private fun berechneMonatlicheWochentagTermine(
        intervall: CustomerIntervall,
        startDatumStart: Long,
        endDatum: Long,
        startDatumMin: Long,
        tageAzuL: Int,
        geloeschteTermine: List<Long>,
        verschobeneTermine: List<VerschobenerTermin>
    ): List<TerminInfo> {
        val result = mutableListOf<TerminInfo>()
        val cal = Calendar.getInstance()
        cal.timeInMillis = startDatumStart
        val startYear = cal.get(Calendar.YEAR)
        val startMonth = cal.get(Calendar.MONTH)
        cal.timeInMillis = endDatum
        val endYear = cal.get(Calendar.YEAR)
        val endMonth = cal.get(Calendar.MONTH)
        val tageAzuLMillis = TimeUnit.DAYS.toMillis(tageAzuL.coerceIn(0, 365).toLong())
        var y = startYear
        var m = startMonth
        while (y < endYear || (y == endYear && m <= endMonth)) {
            val day = getNthWeekdayDayInMonth(cal, y, m, intervall.monthWeekOfMonth, intervall.monthWeekday)
            if (day > 0) {
                cal.set(y, m, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val aDatum = cal.timeInMillis
                if (aDatum in startDatumStart..endDatum && aDatum >= startDatumMin) {
                    val verschobenA = TerminFilterUtils.istTerminVerschoben(aDatum, verschobeneTermine.filter { it.typ == TerminTyp.ABHOLUNG }, intervall.id)
                    val finalA = verschobenA?.verschobenAufDatum ?: aDatum
                    if (!TerminFilterUtils.istTerminGeloescht(finalA, geloeschteTermine)) {
                        result.add(TerminInfo(
                            datum = finalA,
                            typ = TerminTyp.ABHOLUNG,
                            intervallId = intervall.id,
                            verschoben = verschobenA != null,
                            originalDatum = verschobenA?.originalDatum
                        ))
                    }
                    val lDatum = aDatum + tageAzuLMillis
                    if (lDatum >= startDatumMin && lDatum <= endDatum) {
                        val verschobenL = TerminFilterUtils.istTerminVerschoben(lDatum, verschobeneTermine.filter { it.typ == TerminTyp.AUSLIEFERUNG }, intervall.id)
                        val finalL = verschobenL?.verschobenAufDatum ?: lDatum
                        if (!TerminFilterUtils.istTerminGeloescht(finalL, geloeschteTermine)) {
                            result.add(TerminInfo(
                                datum = finalL,
                                typ = TerminTyp.AUSLIEFERUNG,
                                intervallId = intervall.id,
                                verschoben = verschobenL != null,
                                originalDatum = verschobenL?.originalDatum
                            ))
                        }
                    }
                }
            }
            m++
            if (m > Calendar.DECEMBER) {
                m = Calendar.JANUARY
                y++
            }
        }
        return result
    }

    /**
     * Termine aus A-Wochentagen + Intervall: Nur A wird an A-Tagen erzeugt, L nur als A + tageAzuL (keine eigenen L-Tage).
     * Intervall betrifft nur A (A alle 7 Tage an konfigurierten Wochentagen); L = A + tageAzuL.
     * Start = startDatum; wenn customer.erstelltAm > 0, mindestens ab erstelltAm (alte Logik: neue Kunden keine Termine vor Anlage, Überfällig für bestehende bleibt).
     */
    private fun berechneTermineAusWochentagen(
        customer: Customer,
        startDatum: Long,
        tageVoraus: Int
    ): List<TerminInfo> {
        val aTage = customer.effectiveAbholungWochentage
        if (aTage.isEmpty()) return emptyList()
        val tageAzuL = customer.tageAzuLOrDefault(7)
        val startRequested = getStartOfDay(startDatum)
        val start = if (customer.erstelltAm > 0) {
            val erstelltStart = getStartOfDay(customer.erstelltAm)
            maxOf(startRequested, erstelltStart)
        } else {
            startRequested
        }
        val end = start + TimeUnit.DAYS.toMillis(tageVoraus.toLong())
        val result = mutableListOf<TerminInfo>()
        var current = start
        while (current <= end) {
            val w = getWochentagForDay(current)
            if (w in aTage) {
                val verschoben = TerminFilterUtils.istTerminVerschoben(current, customer.verschobeneTermine.filter { it.typ == TerminTyp.ABHOLUNG }, "wochentag")
                val finalDatum = verschoben?.verschobenAufDatum ?: current
                if (!TerminFilterUtils.istTerminGeloescht(finalDatum, customer.geloeschteTermine)) {
                    result.add(TerminInfo(
                        datum = finalDatum,
                        typ = TerminTyp.ABHOLUNG,
                        intervallId = "wochentag",
                        verschoben = verschoben != null,
                        originalDatum = verschoben?.originalDatum
                    ))
                }
            }
            current += TimeUnit.DAYS.toMillis(1)
        }
        // L nur aus A + tageAzuL (keine eigenen L-Wochentage)
        val aTermine = result.toList()
        for (aTermin in aTermine) {
            val lDatum = aTermin.datum + TimeUnit.DAYS.toMillis(tageAzuL.toLong())
            val verschobenL = TerminFilterUtils.istTerminVerschoben(lDatum, customer.verschobeneTermine.filter { it.typ == TerminTyp.AUSLIEFERUNG }, "wochentag")
            val finalL = verschobenL?.verschobenAufDatum ?: lDatum
            if (!TerminFilterUtils.istTerminGeloescht(finalL, customer.geloeschteTermine)) {
                result.add(TerminInfo(
                    datum = finalL,
                    typ = TerminTyp.AUSLIEFERUNG,
                    intervallId = "wochentag",
                    verschoben = verschobenL != null,
                    originalDatum = verschobenL?.originalDatum
                ))
            }
        }
        return result
    }

    /**
     * Berechnet alle Termine für einen Kunden (365 Tage).
     * Hat der Kunde Intervalle, kommen Termine nur aus den Intervallen (Wochentag nur Anzeige).
     * Ohne Intervalle werden bei gesetzten A-Wochentagen Termine an jedem dieser Wochentage erzeugt (L = A + tageAzuL).
     */
    fun berechneAlleTermineFuerKunde(
        customer: Customer,
        liste: KundenListe? = null,
        startDatum: Long = System.currentTimeMillis(),
        tageVoraus: Int = 365
    ): List<TerminInfo> {
        val alleTermine = mutableListOf<TerminInfo>()
        
        // NEUE STRUKTUR: Intervalle-Liste
        if (customer.intervalle.isNotEmpty()) {
            customer.intervalle.forEach { intervall ->
                val termine = berechneTermineFuerIntervall(
                    intervall = intervall,
                    startDatum = startDatum,
                    tageVoraus = tageVoraus,
                    geloeschteTermine = customer.geloeschteTermine,
                    verschobeneTermine = customer.verschobeneTermine,
                    customer = customer
                )
                alleTermine.addAll(termine)
            }
        }
        // Kunden ohne intervalle: keine Intervall-Termine (Migration 2.1 füllt intervalle aus Liste/Legacy)

        // Option A: Wochentags-Termine nur, wenn der Kunde KEINE Intervalle hat.
        if (customer.intervalle.isEmpty() && customer.effectiveAbholungWochentage.isNotEmpty()) {
            val fromWeekdays = berechneTermineAusWochentagen(customer, startDatum, tageVoraus)
            alleTermine.addAll(fromWeekdays)
        }

        // Termine von der Tour-Liste: auf Kunden übertragen (termineVonListe); Erledigen etc. funktioniert ohne Liste
        if (customer.termineVonListe.isNotEmpty()) {
            val start = getStartOfDay(startDatum)
            val end = start + TimeUnit.DAYS.toMillis(tageVoraus.toLong())
            customer.termineVonListe.forEach { kt ->
                val datumStart = getStartOfDay(kt.datum)
                if (datumStart in start..end) {
                    val typ = if (kt.typ == "L") TerminTyp.AUSLIEFERUNG else TerminTyp.ABHOLUNG
                    alleTermine.add(TerminInfo(datum = kt.datum, typ = typ, intervallId = "liste", verschoben = false, originalDatum = null))
                }
            }
        }
        // Fallback: Listen-Termine direkt von der Liste (wenn keine termineVonListe beim Kunden)
        else if (liste != null && liste.listenTermine.isNotEmpty()) {
            val tageAzuL = liste.tageAzuL.coerceIn(1, 365)
            val start = getStartOfDay(startDatum)
            val end = start + TimeUnit.DAYS.toMillis(tageVoraus.toLong())
            liste.listenTermine.filter { it.typ == "A" }.forEach { a ->
                val aStart = getStartOfDay(a.datum)
                if (aStart in start..end) {
                    val lDatum = getStartOfDay(aStart + TimeUnit.DAYS.toMillis(tageAzuL.toLong()))
                    alleTermine.add(TerminInfo(datum = aStart, typ = TerminTyp.ABHOLUNG, intervallId = "liste", verschoben = false, originalDatum = null))
                    alleTermine.add(TerminInfo(datum = lDatum, typ = TerminTyp.AUSLIEFERUNG, intervallId = "liste", verschoben = false, originalDatum = null))
                }
            }
        }

        return alleTermine.sortedBy { it.datum }
    }
    
    /**
     * Direkte Prüfung: Hat der Kunde am angegebenen Datum einen Termin des Typs (A oder L)?
     * Kein "Suchen" im Aufrufer – eine klare Ja/Nein-Antwort für genau dieses Datum.
     * Intern wird ein sicherer Bereich um das Datum herum berechnet (Implementierungsdetail).
     */
    fun hatTerminAmDatum(
        customer: Customer,
        liste: KundenListe?,
        datum: Long,
        typ: TerminTyp
    ): Boolean {
        val datumStart = getStartOfDay(datum)
        // Sicherer Bereich um das eine Datum (7 Tage zurück, 14 Tage Länge), damit
        // wochentags- und intervallbasierte Regeln den Tag treffen
        val termine = berechneAlleTermineFuerKunde(
            customer = customer,
            liste = liste,
            startDatum = datumStart - TimeUnit.DAYS.toMillis(7),
            tageVoraus = 14
        )
        return termine.any {
            getStartOfDay(it.datum) == datumStart && it.typ == typ
        }
    }
    
    /**
     * Nächstes fälliges Termin-Datum (A oder L) ab heute.
     * Berücksichtigt gelöschte Termine. Ersetzt Customer.getFaelligAm().
     */
    fun naechstesFaelligAmDatum(customer: Customer): Long {
        // #region agent log
        val hasMonthly = customer.intervalle.any { it.regelTyp == TerminRegelTyp.MONTHLY_WEEKDAY }
        AgentDebugLog.log("TerminBerechnungUtils.kt", "naechstesFaelligAmDatum_entry", mapOf("customerId" to customer.id, "intervalleSize" to customer.intervalle.size, "hasMonthlyWeekday" to hasMonthly), "H7")
        // #endregion
        if (customer.intervalle.isEmpty() && customer.effectiveAbholungWochentage.isEmpty()) return 0L
        val heute = System.currentTimeMillis()
        val heuteStart = getStartOfDay(heute)
        val termine = berechneAlleTermineFuerKunde(
            customer = customer,
            liste = null,
            startDatum = heute,
            tageVoraus = 365
        )
        val naechstes = termine.firstOrNull {
            it.datum >= heuteStart && !TerminFilterUtils.istTerminGeloescht(it.datum, customer.geloeschteTermine)
        }
        val result = naechstes?.datum ?: 0L
        // #region agent log
        AgentDebugLog.log("TerminBerechnungUtils.kt", "naechstesFaelligAmDatum_result", mapOf("customerId" to customer.id, "termineCount" to termine.size, "result" to result), "H7")
        // #endregion
        return result
    }

    /**
     * Für Anzeige/Logik: gespeichertes faelligAmDatum, sofern nicht vor Startdatum (erstelltAm).
     * Wenn faelligAmDatum vor erstelltAm liegt (z. B. alter Eintrag vor Startdatum-Fix), wird das
     * berechnete nächste Fälligkeitsdatum verwendet, damit keine „Geister-Termine“ vor Start angezeigt werden.
     */
    fun effectiveFaelligAmDatum(customer: Customer): Long {
        val stored = customer.faelligAmDatum
        if (stored <= 0) return stored
        if (customer.erstelltAm > 0 && getStartOfDay(stored) < getStartOfDay(customer.erstelltAm))
            return naechstesFaelligAmDatum(customer)
        return stored
    }

    // Filter-Funktionen entfernt - jetzt in TerminFilterUtils
}

/**
 * Repräsentiert einen Termin mit allen relevanten Informationen
 */
data class TerminInfo(
    val datum: Long,
    val typ: TerminTyp,
    val intervallId: String,
    val verschoben: Boolean = false,
    val originalDatum: Long? = null // Nur gesetzt wenn verschoben
)
