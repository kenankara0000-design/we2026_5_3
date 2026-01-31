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
    /** Kurztext für Status-Badge auf der Karte, z. B. "A+L heute", "Überfällig", "" */
    val statusBadgeText: String,
    /** true = Badge rot (überfällig), false = Badge blau (heute fällig) */
    val isOverdueBadge: Boolean = false
) : Serializable
