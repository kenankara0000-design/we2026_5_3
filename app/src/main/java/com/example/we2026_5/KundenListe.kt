package com.example.we2026_5

/**
 * Repräsentiert eine Liste für Privat-Kunden (z.B. "Borna P", "Kitzscher P")
 * Jede Liste hat feste Wochentage für Abholung und Auslieferung
 * Listen können wöchentlich wiederholt werden
 */
data class KundenListe(
    val id: String = "",
    val name: String = "", // z.B. "Borna P", "Kitzscher P"
    val abholungWochentag: Int = 0, // 0=Montag, 1=Dienstag, ..., 6=Sonntag
    val auslieferungWochentag: Int = 0, // 0=Montag, 1=Dienstag, ..., 6=Sonntag
    val wiederholen: Boolean = true, // Ob die Liste wöchentlich wiederholt wird
    val erstelltAm: Long = System.currentTimeMillis()
)
