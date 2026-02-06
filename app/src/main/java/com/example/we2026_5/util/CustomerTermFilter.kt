package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerStatus

/**
 * Zentrale Logik: Kunden, die für Termine/Touren (Fällig, Überfällig, Tourenplaner) berücksichtigt werden.
 * Pausierte Kunden (PAUSIERT) sind raus, außer die Pause ist befristet und bereits beendet (now > pauseEnde).
 * Unbestimmt pausiert (pauseEnde == 0) = nie berücksichtigen, bis wieder aktiv gesetzt.
 */
object CustomerTermFilter {

    /**
     * true = Kunde wird bei Terminberechnung, Fällig, Überfällig, Tourenplaner berücksichtigt.
     * false = Kunde erscheint nur im Kunden Manager.
     */
    @JvmStatic
    fun Customer.isActiveForTerms(now: Long = System.currentTimeMillis()): Boolean {
        if (status != CustomerStatus.PAUSIERT) return true
        if (pauseEnde <= 0L) return false // unbestimmt pausiert
        return now > pauseEnde // Pause befristet und beendet
    }

    /**
     * Filtert die Liste auf Kunden, die für Terme/Touren aktiv sind.
     */
    @JvmStatic
    fun filterActiveForTerms(customers: List<Customer>, now: Long = System.currentTimeMillis()): List<Customer> =
        customers.filter { it.isActiveForTerms(now) }
}

/** Extension: Liste auf für Terme aktive Kunden filtern. */
fun List<Customer>.filterActiveForTerms(now: Long = System.currentTimeMillis()): List<Customer> =
    CustomerTermFilter.filterActiveForTerms(this, now)
