package com.example.we2026_5.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Zentrales Typografie-System für die gesamte App.
 *
 * Nutzung: `AppTypography.TitleLarge` statt `fontSize = 28.sp, fontWeight = FontWeight.Bold`.
 */
object AppTypography {

    /** Großer Titel – z.B. Hauptbildschirm-Überschrift (28sp, Bold) */
    val TitleLarge = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    )

    /** Mittlerer Titel – z.B. Screen-Titel in TopBar (20sp, Bold) */
    val TitleMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    )

    /** Kleiner Titel – z.B. Card-Überschriften (18sp, SemiBold) */
    val TitleSmall = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    )

    /** Section-Titel – z.B. Abschnitts-Überschriften (16sp, SemiBold) */
    val SectionTitle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    )

    /** Body-Text – Standardtext (14sp, Normal) */
    val Body = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    /** Body-Text fett (14sp, SemiBold) */
    val BodyBold = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    )

    /** Button-Text – einheitlich für alle Buttons (16sp, Medium) */
    val ButtonText = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    )

    /** Caption – kleine Beschreibungen, Labels (12sp, Normal) */
    val Caption = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    /** Small – sehr kleine Texte, Badges (10sp, Normal) */
    val Small = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )

    /** Field-Label – für Formularlabels (14sp, Medium) – gleich wie in DetailUiConstants */
    val FieldLabel = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    )
}
