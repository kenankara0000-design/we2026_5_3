package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenTyp

/**
 * Migration für bestehende Kunden (ohne kundenTyp/listenWochentag).
 * - kundenTyp: REGELMAESSIG wenn intervalle nicht leer oder listeId gesetzt, sonst UNREGELMAESSIG
 * - listenWochentag: aus defaultAbholungWochentag ableiten falls >= 0, sonst 0
 */
fun Customer.migrateKundenTyp(): Customer {
    val newTyp = when {
        intervalle.isNotEmpty() || listeId.isNotEmpty() -> KundenTyp.REGELMAESSIG
        else -> KundenTyp.UNREGELMAESSIG
    }
    val newListen = when {
        listenWochentag >= 0 -> listenWochentag
        defaultAbholungWochentag in 0..6 -> defaultAbholungWochentag
        else -> 0
    }
    return if (kundenTyp != newTyp || listenWochentag != newListen) {
        copy(kundenTyp = newTyp, listenWochentag = newListen)
    } else {
        this
    }
}
