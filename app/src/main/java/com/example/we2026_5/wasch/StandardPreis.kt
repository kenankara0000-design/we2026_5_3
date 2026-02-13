package com.example.we2026_5.wasch

import com.google.firebase.database.IgnoreExtraProperties

/** Einheitlicher Standardpreis (Listenkunden + Privat); articleId → priceNet, priceGross. */
@IgnoreExtraProperties
data class StandardPreis(
    val articleId: String = "",
    val priceNet: Double = 0.0,
    val priceGross: Double = 0.0
)
