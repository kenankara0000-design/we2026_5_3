package com.example.we2026_5

/**
 * Repräsentiert eine Liste für Kunden (z.B. "Borna P", "Kitzscher P")
 * Jede Liste kann mehrere Intervalle haben (bis zu 12)
 * Jedes Intervall hat eigene Abholungs- und Auslieferungsdaten
 */
data class KundenListe(
    val id: String = "",
    val name: String = "", // z.B. "Borna P", "Kitzscher P"
    val listeArt: String = "Gewerbe", // "Gewerbe", "Privat" oder "Liste"
    val intervalle: List<ListeIntervall> = emptyList(), // Liste der Intervalle (bis zu 12)
    val erstelltAm: Long = System.currentTimeMillis(),
    
    // Status (ähnlich wie bei Customer)
    val abholungErfolgt: Boolean = false,
    val auslieferungErfolgt: Boolean = false,
    val urlaubVon: Long = 0,
    val urlaubBis: Long = 0,
    
    // Verschieben-Logik - ERWEITERT für einzelne Termine
    val verschobeneTermine: List<VerschobenerTermin> = emptyList(), // NEUE Logik: Einzelne Termine verschieben
    
    val geloeschteTermine: List<Long> = listOf() // Liste von gelöschten Termin-Daten (für einzelne Termin-Löschungen)
) {
    // Rückwärtskompatibilität: Alte Felder für Migration
    @Deprecated("Verwende intervalle statt abholungWochentag")
    val abholungWochentag: Int = 0
    
    @Deprecated("Verwende intervalle statt auslieferungWochentag")
    val auslieferungWochentag: Int = 0
    
    @Deprecated("Verwende intervalle statt wiederholen")
    val wiederholen: Boolean = true
}
