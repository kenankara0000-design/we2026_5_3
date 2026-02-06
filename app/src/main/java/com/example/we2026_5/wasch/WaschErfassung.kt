package com.example.we2026_5.wasch

import com.google.firebase.database.IgnoreExtraProperties

/** Eine Position in einer Wäsche-Erfassung (Artikel + Menge). */
@IgnoreExtraProperties
data class ErfassungPosition(
    val articleId: String = "",
    val menge: Int = 0
)

/** Eine Erfassung (Liste) pro Kunde: Datum + Artikel mit Stückzahlen. */
@IgnoreExtraProperties
data class WaschErfassung(
    val id: String = "",
    val customerId: String = "",
    /** Erfassungsdatum (Tagesanfang oder Zeitstempel). */
    val datum: Long = 0L,
    val positionen: List<ErfassungPosition> = emptyList(),
    /** Optional: z. B. "Paket 2" oder Notiz. */
    val notiz: String = ""
)
