package com.example.we2026_5

/**
 * Einträge für die Tourenplaner-Liste (Compose).
 */
sealed class ListItem {
    data class CustomerItem(
        val customer: Customer,
        val isOverdue: Boolean = false,
        val isVerschobenAmFaelligkeitstag: Boolean = false,
        val verschobenInfo: String? = null,
        /** Am neuen Tag: "Verschoben von [Datum]" */
        val verschobenVonInfo: String? = null,
        /** true = am Tag erledigt, zählt nicht in Fällig-Count */
        val isErledigtAmTag: Boolean = false
    ) : ListItem()
    data class SectionHeader(
        val title: String,
        val count: Int,
        val erledigtCount: Int,
        val sectionType: SectionType,
        val kunden: List<Customer> = emptyList()
    ) : ListItem()
    data class ListeHeader(
        val listeName: String,
        val kundenCount: Int,
        val erledigtCount: Int,
        val listeId: String,
        val nichtErledigteKunden: List<Customer> = emptyList(),
        val erledigteKunden: List<Customer> = emptyList()
    ) : ListItem()
    /** Tour-Liste mit erledigten Kunden im Erledigt-Bereich */
    data class TourListeErledigt(
        val listeName: String,
        val erledigteKunden: List<Customer>
    ) : ListItem()
}

enum class SectionType {
    OVERDUE, DONE, LISTE
}
