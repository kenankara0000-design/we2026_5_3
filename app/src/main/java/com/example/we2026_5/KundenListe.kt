package com.example.we2026_5

/**
 * Repräsentiert eine Liste für Kunden (z.B. "Borna P", "Kitzscher P")
 * Jede Liste kann mehrere Intervalle haben (bis zu 12)
 * Jedes Intervall hat eigene Abholungs- und Auslieferungsdaten
 */
data class KundenListe(
    val id: String = "",
    val name: String = "", // z.B. "Borna P", "Kitzscher P"
    val intervalle: List<ListeIntervall> = emptyList(), // Liste der Intervalle (bis zu 12)
    val erstelltAm: Long = System.currentTimeMillis()
) {
    // Rückwärtskompatibilität: Alte Felder für Migration
    @Deprecated("Verwende intervalle statt abholungWochentag")
    val abholungWochentag: Int = 0
    
    @Deprecated("Verwende intervalle statt auslieferungWochentag")
    val auslieferungWochentag: Int = 0
    
    @Deprecated("Verwende intervalle statt wiederholen")
    val wiederholen: Boolean = true
}
