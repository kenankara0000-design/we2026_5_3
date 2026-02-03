package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenTyp

/**
 * Migration für bestehende Kunden (ohne kundenTyp).
 * kundenTyp: REGELMAESSIG wenn intervalle nicht leer oder listeId gesetzt, sonst UNREGELMAESSIG.
 * listenWochentag/wochentag werden nicht mehr gesetzt – A/L-Tag (defaultAbholungWochentag, defaultAuslieferungWochentag) sind die Quelle.
 */
fun Customer.migrateKundenTyp(): Customer {
    val newTyp = when {
        intervalle.isNotEmpty() || listeId.isNotEmpty() -> KundenTyp.REGELMAESSIG
        else -> KundenTyp.UNREGELMAESSIG
    }
    return if (kundenTyp != newTyp) copy(kundenTyp = newTyp) else this
}
