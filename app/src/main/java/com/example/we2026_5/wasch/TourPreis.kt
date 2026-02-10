package com.example.we2026_5.wasch

import com.google.firebase.database.IgnoreExtraProperties

/** Einheitlicher Preis für Tour-Kunden (articleId → priceNet, priceGross). */
@IgnoreExtraProperties
data class TourPreis(
    val articleId: String = "",
    val priceNet: Double = 0.0,
    val priceGross: Double = 0.0
)
