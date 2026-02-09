package com.example.we2026_5.tourplanner

import java.io.Serializable

/**
 * Zustand für das Erledigungs-Bottom-Sheet: welche Aktionen anzeigen/aktivieren.
 * Gilt für alle Kundenarten (Gewerblich, Privat, Liste).
 */
data class ErledigungSheetState(
    val showAbholung: Boolean,
    val enableAbholung: Boolean,
    val showAuslieferung: Boolean,
    val enableAuslieferung: Boolean,
    val showKw: Boolean,
    val enableKw: Boolean,
    val showVerschieben: Boolean,
    val showUrlaub: Boolean,
    val showRueckgaengig: Boolean,
    /** Ausnahme-Termin A an diesem Tag: Button „A-Abholung Erledigen“ anzeigen */
    val showAusnahmeAbholung: Boolean = false,
    val enableAusnahmeAbholung: Boolean = false,
    /** Ausnahme-Termin L an diesem Tag: Button „L-Auslieferung Erledigen“ anzeigen */
    val showAusnahmeAuslieferung: Boolean = false,
    val enableAusnahmeAuslieferung: Boolean = false,
    /** Kunden-Termin A an diesem Tag (für Info im Tab Termin) */
    val showKundenAbholung: Boolean = false,
    /** Kunden-Termin L an diesem Tag (für Info im Tab Termin) */
    val showKundenLieferung: Boolean = false,
    /** Kurztext für Status-Badge auf der Karte, z. B. "A+L heute", "Überfällig", "A", "L", "" */
    val statusBadgeText: String,
    /** true = Badge rot (überfällig), false = Badge blau (heute fällig) */
    val isOverdueBadge: Boolean = false,
    /** Am Fälligkeitstag nur Info: z. B. "A überfällig" / "L überfällig" (keine Aktionen) */
    val overdueInfoText: String = "",
    /** Wenn überfälliger Termin erledigt: A/L mit Datum und Zeitstempel */
    val completedInfoText: String = ""
) : Serializable
