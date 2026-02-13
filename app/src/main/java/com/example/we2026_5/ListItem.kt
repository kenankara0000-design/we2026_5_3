package com.example.we2026_5

/**
 * Einträge für die Tourenplaner-Liste (Compose).
 */
sealed class ListItem {
    data class CustomerItem(
        val customer: Customer,
        val statusBadgeText: String = "",
        val isOverdue: Boolean = false,
        /** Bei isOverdue: „A“, „L“ oder „AL“ für Badge neben Überfällig */
        val overdueAlSuffix: String? = null,
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
    /** Listen-Card (ohne Wochentag): Eine äußere Card mit Kunden-Karten darin. Nur Listen mit wochentag !in 0..6. */
    data class TourListeCard(
        val liste: KundenListe,
        /** Customer, isOverdue, overdueAlSuffix („A“/„L“/„AL“ oder null) */
        val kunden: List<Triple<Customer, Boolean, String?>>,
        val aCount: Int = 0,
        val lCount: Int = 0
    ) : ListItem()
    /** Liste ohne Wochentag mit erledigten Kunden im Erledigt-Bereich */
    data class TourListeErledigt(
        val listeName: String,
        val erledigteKunden: List<Customer>
    ) : ListItem()
    /** Gesamter Erledigt-Bereich in einer Card (Header + Einzelkunden + Listen ohne Wochentag) */
    data class ErledigtSection(
        val title: String,
        val count: Int,
        val erledigtCount: Int,
        val doneOhneListen: List<Customer>,
        val tourListenErledigt: List<Pair<String, List<Customer>>>
    ) : ListItem()
}

enum class SectionType {
    OVERDUE, DONE, LISTE
}
