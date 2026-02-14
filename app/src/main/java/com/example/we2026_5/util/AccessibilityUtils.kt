package com.example.we2026_5.util

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * WCAG-Kontrast-Utilities für Barrierefreiheit.
 * WCAG AA: 4.5:1 für normalen Text, 3:1 für großen Text.
 *
 * Nutzung: Prüfung von Text-Hintergrund-Kombinationen beim Design.
 */
object AccessibilityUtils {

    /**
     * Berechnet relative Luminanz (0–1) für WCAG-Kontrast.
     * https://www.w3.org/TR/WCAG20-TECHS/G17.html
     */
    fun luminance(color: Color): Double {
        fun channel(c: Float): Double {
            val s = if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).toDouble().pow(2.4)
            return s.toDouble()
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    /**
     * Kontrastverhältnis (1–21) gemäß WCAG.
     * ≥4.5 = AA normal, ≥3 = AA groß, ≥7 = AAA normal.
     */
    fun contrastRatio(foreground: Color, background: Color): Double {
        val l1 = luminance(foreground).coerceAtLeast(0.0)
        val l2 = luminance(background).coerceAtLeast(0.0)
        val lighter = maxOf(l1, l2) + 0.05
        val darker = minOf(l1, l2) + 0.05
        return lighter / darker
    }

    /**
     * true wenn Kontrast ≥ 4.5:1 (WCAG AA normaler Text).
     */
    fun meetsAANormalText(foreground: Color, background: Color): Boolean =
        contrastRatio(foreground, background) >= 4.5

    /**
     * true wenn Kontrast ≥ 3:1 (WCAG AA großer Text / UI).
     */
    fun meetsAALargeText(foreground: Color, background: Color): Boolean =
        contrastRatio(foreground, background) >= 3.0
}
