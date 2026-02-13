package com.example.we2026_5.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Zentrale Farb-Konstanten für die gesamte App.
 * Entspricht den Werten in colors.xml – hier als Compose-Color für direkten Zugriff.
 *
 * Nutzung: `AppColors.PrimaryBlue` statt `Color(0xFF1976D2)` oder `colorResource(R.color.primary_blue)`.
 */
object AppColors {

    // ── Primärfarben ──
    val PrimaryBlue = Color(0xFF1976D2)
    val PrimaryBlueDark = Color(0xFF1565C0)
    val PrimaryBlueLight = Color(0xFF42A5F5)
    val ButtonBlue = Color(0xFF2196F3)

    // ── Status-Farben ──
    val StatusOverdue = Color(0xFFD32F2F)
    val StatusDone = Color(0xFF388E3C)
    val StatusWarning = Color(0xFFF57C00)
    val StatusInfo = Color(0xFF1976D2)
    val StatusAusnahme = Color(0xFFF9A825)
    val OfflineYellow = Color(0xFFFFEB3B)
    val ErrorRed = Color(0xFFD32F2F) // = StatusOverdue

    // ── Button-Farben ──
    val ButtonAbholung = Color(0xFF1976D2)
    val ButtonAuslieferung = Color(0xFF388E3C)
    val ButtonVerschieben = Color(0xFFD32F2F)
    val ButtonUrlaub = Color(0xFFE65100)
    val ButtonRueckgaengig = Color(0xFFD32F2F)

    // ── Akzent-Farben ──
    val AccentOrange = Color(0xFFF57C00)
    val AccentGreen = Color(0xFF388E3C)
    val AccentBlue = Color(0xFF1976D2)
    val AccentBrown = Color(0xFF5D4037)

    // ── G/P/L Buttons ──
    val ButtonGewerblichGlossy = Color(0xFF1565C0)
    val ButtonPrivatGlossy = Color(0xFFE65100)
    val ButtonListeGlossy = Color(0xFF5D4037)

    // ── Neutrale Farben ──
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val LightGray = Color(0xFFE0E0E0)
    val BackgroundLight = Color(0xFFFAFAFA)
    val SurfaceWhite = Color(0xFFFFFFFF)
    val SurfaceLight = Color(0xFFF5F5F5)
    val TextPrimary = Color(0xFF212121)
    val TextSecondary = Color(0xFF757575)
    val Divider = Color(0xFFE0E0E0)
    val ButtonInactive = Color(0xFFBDBDBD)
    val ButtonActive = Color(0xFFF57C00)

    // ── Section-Header ──
    val SectionOverdueBg = Color(0xFFFFEBEE)
    val SectionOverdueText = Color(0xFFC62828)
    val SectionDoneBg = Color(0xFF43A047)
    val SectionDoneText = Color(0xFF2E7D32)

    // ── Wochentag ──
    val WeekdayBlue = Color(0xFF1976D2)
    val WeekdayBlueDark = Color(0xFF1565C0)
    val WeekendTurquoise = Color(0xFF00897B)
    val WeekendTurquoiseDark = Color(0xFF00695C)
    val WeekendOrange = Color(0xFFF57C00)
    val WeekendOrangeDark = Color(0xFFE65100)

    // ── Kunden-Status Hintergrund ──
    val CustomerOverdueBg = Color(0xFFFFEBEE)
    val CustomerDoneBg = Color(0xFFF5F5F5)
    val CustomerUrlaubBg = Color(0xFFFFF3E0)
    val CustomerGewerblichBg = Color(0xFFE3F2FD)

    // ── Termin ──
    val TerminToday = Color(0xFF1976D2)
    val TerminTomorrow = Color(0xFF42A5F5)
    val TerminUpcoming = Color(0xFF757575)
    val TerminPast = Color(0xFFBDBDBD)

    // ── Termin-Regel ──
    val TerminRegelCardBg = Color(0xFFFFFFFF)
    val TerminRegelHeaderBg = Color(0xFF1976D2)
    val TerminRegelHeaderText = Color(0xFFFFFFFF)
    val TerminRegelButtonSave = Color(0xFF388E3C)
    val TerminRegelButtonSaveDark = Color(0xFF2E7D32)
    val TerminRegelFab = Color(0xFF2196F3)
    val TerminRegelAbholung = Color(0xFF1976D2)
    val TerminRegelAuslieferung = Color(0xFF388E3C)

    // ── Tour-Planner ──
    val TourPlannerDraggingBg = Color(0xFFE3F2FD)

    // ── Sonstige (häufig hardcodiert gefunden) ──
    val InfoBlueBg = Color(0xFFE8F0FE) // Häufig für Info-Backgrounds
}
