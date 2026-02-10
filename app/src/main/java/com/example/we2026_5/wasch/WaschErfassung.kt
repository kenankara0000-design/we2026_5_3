package com.example.we2026_5.wasch

import com.google.firebase.database.IgnoreExtraProperties

/** Eine Position in einer Wäsche-Erfassung (Artikel + Menge + Einheit). Menge als Double für kg (z. B. 5,5). */
@IgnoreExtraProperties
data class ErfassungPosition(
    val articleId: String = "",
    val menge: Double = 0.0,
    /** Einheit (z. B. Stk, kg) – von Artikel übernommen oder überschrieben. */
    val einheit: String = ""
)

/** Eine Erfassung pro Kunde: Datum (+ optional Zeit) + Artikelzeilen. */
@IgnoreExtraProperties
data class WaschErfassung(
    val id: String = "",
    val customerId: String = "",
    /** Erfassungsdatum (Tagesanfang). */
    val datum: Long = 0L,
    /** Optional, z. B. "14:30" für Anzeige. */
    val zeit: String = "",
    val positionen: List<ErfassungPosition> = emptyList(),
    /** Optional: z. B. "Paket 2" oder Notiz. */
    val notiz: String = "",
    /** Beleg als erledigt markiert (verriegelt) – erscheint nur im Erledigt-Bereich. */
    val erledigt: Boolean = false
)
