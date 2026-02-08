package com.example.we2026_5.wasch

import com.google.firebase.database.IgnoreExtraProperties

/** Kundenspezifischer Preis für einen Artikel (aus SevDesk PartContactPrice oder manuell). */
@IgnoreExtraProperties
data class KundenPreis(
    val customerId: String = "",
    val articleId: String = "",
    val priceNet: Double = 0.0,
    val priceGross: Double = 0.0
)
