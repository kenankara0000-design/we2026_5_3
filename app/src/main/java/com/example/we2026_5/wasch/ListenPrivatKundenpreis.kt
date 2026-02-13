package com.example.we2026_5.wasch

import com.google.firebase.database.IgnoreExtraProperties

/** Einzelpreis aus Listen- und Privat-Kundenpreisen (Listenkunden + Privat); articleId → priceNet, priceGross. */
@IgnoreExtraProperties
data class ListenPrivatKundenpreis(
    val articleId: String = "",
    val priceNet: Double = 0.0,
    val priceGross: Double = 0.0
)
