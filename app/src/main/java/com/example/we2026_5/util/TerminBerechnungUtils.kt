package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.util.AgentDebugLog
import java.util.concurrent.TimeUnit

/**
 * Utility-Klasse für Termin-Berechnungen und Überfällig-Prüfungen.
 * Berechnungs-Logik wurde nach TerminCalculator extrahiert.
 */
object TerminBerechnungUtils {

    /**
     * Normalisiert ein Datum auf Tagesanfang (00:00:00) in Europe/Berlin.
     */
    fun getStartOfDay(timestamp: Long): Long {
        val cal = AppTimeZone.newCalendar()
        cal.timeInMillis = timestamp
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Prüft, ob ein Zeitstempel (z. B. abholungErledigtAm) in den angezeigten Berlin-Tag fällt.
     * Verwendet Tagesbereich [berlinDayStart, berlinDayStart+24h) statt exakter Gleichheit,
     * damit alte Erledigt-Werte (gespeichert als Mitternacht in Geräte-/UTC-Zeitzone vor dem
     * Timezone-Fix) weiterhin als „erledigt am Tag" erkannt werden.
     */
    fun isTimestampInBerlinDay(ts: Long, berlinDayStart: Long): Boolean =
        ts >= berlinDayStart && ts < berlinDayStart + TimeUnit.DAYS.toMillis(1)
    
    /**
     * Eine Source of Truth: Kunde hat überfälligen Termin (nächstes Fälligkeitsdatum vor heute).
     * Für Anzeige/Badge (Kundendetail, Statistiken). Bei Tour/Listen-Kontext: TourDataFilter.istKundeUeberfaellig nutzen.
     */
    fun istKundeUeberfaelligHeute(customer: Customer): Boolean {
        val faellig = naechstesFaelligAmDatum(customer)
        return faellig > 0 && faellig < getStartOfDay(System.currentTimeMillis())
    }

    /**
     * Nächstes fälliges Termin-Datum (A oder L) ab heute.
     * Berücksichtigt gelöschte Termine. Ersetzt Customer.getFaelligAm().
     */
    fun naechstesFaelligAmDatum(customer: Customer): Long {
        // #region agent log
        val hasMonthly = customer.intervalle.any { it.regelTyp == com.example.we2026_5.TerminRegelTyp.MONTHLY_WEEKDAY }
        AgentDebugLog.log("TerminBerechnungUtils.kt", "naechstesFaelligAmDatum_entry", mapOf("customerId" to customer.id, "intervalleSize" to customer.intervalle.size, "hasMonthlyWeekday" to hasMonthly), "H7")
        // #endregion
        if (customer.intervalle.isEmpty() && customer.effectiveAbholungWochentage.isEmpty()) return 0L
        val heute = System.currentTimeMillis()
        val heuteStart = getStartOfDay(heute)
        val termine = TerminCalculator.berechneAlleTermineFuerKunde(
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
     * berechnete nächste Fälligkeitsdatum verwendet, damit keine „Geister-Termine" vor Start angezeigt werden.
     */
    fun effectiveFaelligAmDatum(customer: Customer): Long {
        val stored = customer.faelligAmDatum
        if (stored <= 0) return stored
        if (customer.erstelltAm > 0 && getStartOfDay(stored) < getStartOfDay(customer.erstelltAm))
            return naechstesFaelligAmDatum(customer)
        return stored
    }
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
