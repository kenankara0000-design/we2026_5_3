package com.example.we2026_5.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Zentrale Spacing-Konstanten für die gesamte App.
 *
 * Nutzung: `AppSpacing.ScreenPadding` statt `16.dp` (oder 20dp, 24dp …).
 */
object AppSpacing {

    // ── Standard-Abstände ──

    /** Screen-Padding (außen, alle Screens) */
    val ScreenPadding = 16.dp

    /** Card-Padding (innen, alle Cards) */
    val CardPadding = 16.dp

    /** Abstand zwischen Feldern in Formularen */
    val FieldSpacing = 12.dp

    /** Abstand zwischen Abschnitten (Sections) */
    val SectionSpacing = 20.dp

    // ── Kleinere Abstände ──

    /** Kompakter Abstand (z.B. innerhalb von Rows) */
    val Compact = 4.dp

    /** Kleiner Abstand */
    val Small = 8.dp

    /** Mittlerer Abstand */
    val Medium = 12.dp

    /** Standard-Abstand */
    val Default = 16.dp

    /** Großer Abstand */
    val Large = 20.dp

    /** Extra-großer Abstand */
    val ExtraLarge = 24.dp

    // ── Spezifische Abstände ──

    /** Card-Elevation (Standard) */
    val CardElevation = 2.dp

    /** Corner-Radius (Standard) */
    val CornerRadius = 12.dp

    /** Corner-Radius klein */
    val CornerRadiusSmall = 8.dp

    /** Corner-Radius groß */
    val CornerRadiusLarge = 16.dp

    /** Minimum Touch-Target (Accessibility) */
    val MinTouchTarget = 48.dp

    /** Standard Button-Höhe (einheitlich 48dp) */
    val ButtonHeight = 48.dp
}
