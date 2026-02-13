package com.example.we2026_5

/**
 * Repräsentiert eine Liste für Kunden (z.B. "Borna P", "Kitzscher P")
 * Jede Liste kann mehrere Intervalle haben (bis zu 12)
 * Jedes Intervall hat eigene Abholungs- und Auslieferungsdaten
 */
data class KundenListe(
    val id: String = "",
    val name: String = "", // z.B. "Borna P", "Kitzscher P" oder "Dienstag" für Wochentagslisten
    val listeArt: String = "Gewerbe", // "Gewerbe", "Privat" oder "Listenkunden"
    val wochentag: Int = -1, // 0=Mo..6=So für Wochentagslisten; -1=alte Listen (Ort-basiert)
    val intervalle: List<ListeIntervall> = emptyList(), // Liste der Intervalle (bis zu 12)
    val erstelltAm: Long = System.currentTimeMillis(),
    
    // Status (ähnlich wie bei Customer)
    val abholungErfolgt: Boolean = false,
    val auslieferungErfolgt: Boolean = false,
    val urlaubVon: Long = 0,
    val urlaubBis: Long = 0,
    
    // Verschieben-Logik - ERWEITERT für einzelne Termine
    val verschobeneTermine: List<VerschobenerTermin> = emptyList(), // NEUE Logik: Einzelne Termine verschieben
    
    val geloeschteTermine: List<Long> = listOf(), // Liste von gelöschten Termin-Daten (für einzelne Termin-Löschungen)

    /** Listen-Termine: A/L-Termine für die gesamte Liste (gilt für alle Kunden in dieser Liste). Struktur wie KundenTermin. */
    val listenTermine: List<KundenTermin> = emptyList(),

    /** Für Listen ohne Wochentag (wochentag !in 0..6): Wochentag A (0=Mo..6=So). Bei Termin anlegen wird nächster A an diesem Tag berechnet. */
    val wochentagA: Int? = null,
    /** Für Listen ohne Wochentag: Tage zwischen A und L (L = A + tageAzuL). */
    val tageAzuL: Int = 7
) {
    // Rückwärtskompatibilität: Alte Felder für Migration
    @Deprecated("Verwende intervalle statt abholungWochentag")
    val abholungWochentag: Int = 0
    
    @Deprecated("Verwende intervalle statt auslieferungWochentag")
    val auslieferungWochentag: Int = 0
    
    @Deprecated("Verwende intervalle statt wiederholen")
    val wiederholen: Boolean = true
}
