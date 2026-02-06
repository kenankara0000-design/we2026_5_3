package com.example.we2026_5.wasch

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

/** Globaler Artikel (z. B. aus SevDesk importiert). Preis optional, später. */
@IgnoreExtraProperties
data class Article(
    val id: String = "",
    val name: String = "",
    /** Optional: Preis pro Stück, z. B. für Abrechnung. */
    val preis: Double = 0.0,
    /** Einheit (z. B. Stück, kg, m) – für Abrechnung/Anzeige. */
    val einheit: String = "",
    /** Externe ID (z. B. SevDesk) für Sync. */
    val sevDeskId: String? = null
) {
    @Exclude
    fun hasPrice(): Boolean = preis > 0
}
