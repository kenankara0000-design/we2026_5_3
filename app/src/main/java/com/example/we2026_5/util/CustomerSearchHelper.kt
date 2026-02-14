package com.example.we2026_5.util

import com.example.we2026_5.Customer

/**
 * Gemeinsame Kunden-Suchlogik für Erfassung, Belege und Kundenpreise.
 * Filtert Kunden nach Anzeigename (displayName), Groß-/Kleinschreibung ignoriert.
 */
object CustomerSearchHelper {

    /**
     * Filtert eine Kundenliste nach Suchbegriff im Anzeigenamen.
     * Leerer Query liefert leere Liste (keine Vollanzeige aller Kunden).
     */
    fun filterByDisplayName(customers: List<Customer>, query: String): List<Customer> {
        if (query.isBlank()) return emptyList()
        return customers.filter { it.displayName.contains(query, ignoreCase = true) }
    }
}
