package com.example.we2026_5.wasch

/**
 * Feste Liste der Artikel im Wäscheliste-Formular.
 * Eine zentrale Quelle – Änderungen (z. B. neue Positionen) nur hier.
 * Key = ID für ErfassungPosition.articleId / Mengen-Map.
 */
data class WaeschelisteArtikelItem(val key: String, val label: String)

object WaeschelisteArtikel {

    private val SPALTE_LINKS = listOf(
        "Bettbezüge", "Bettlagen", "Kopfkissenbezüge", "Spannbettlagen",
        "Couverts", "Tafeltücher", "Tischdecken", "Servietten", "Geschirrtücher",
        "Handtücher", "Badetücher", "Waschlappen", "Bademantel", "Babydecken",
        "Wolldecken", "Steppdecken", "Taschentücher", "Unterwäsche"
    )

    private val SPALTE_RECHTS_OHNE_KATEGORIE = listOf(
        "Nachthemden", "Schlafanzüge", "Oberhemden", "Blusen",
        "Textilbekleid. (waschbar)", "Store", "Übergardinen", "Kissenhüllen"
    )

    private val BERUFSWAESCHE_WEISS = listOf(
        "Kittel", "Jacken", "Hosen", "Mützen", "Overall", "Vorstrecker"
    ).map { "Berufswäsche Weiss: $it" }

    private val BERUFSWAESCHE_DUNKEL = listOf(
        "Kittel", "Jacken", "Hosen", "Latzhosen", "Overall", "Wattejacken"
    ).map { "Berufswäsche Dunkel: $it" }

    /** Alle Keys in Reihenfolge (linke Spalte, dann rechte inkl. Kategorien). */
    fun allKeys(): List<String> = buildList {
        addAll(SPALTE_LINKS)
        addAll(SPALTE_RECHTS_OHNE_KATEGORIE)
        addAll(BERUFSWAESCHE_WEISS)
        addAll(BERUFSWAESCHE_DUNKEL)
    }

    /** Für UI: linke Spalte (key, label). */
    fun spalteLinks(): List<WaeschelisteArtikelItem> =
        SPALTE_LINKS.map { WaeschelisteArtikelItem(it, it) }

    /** Für UI: rechte Spalte (key, label inkl. Kategorien-Prefix). */
    fun spalteRechts(): List<WaeschelisteArtikelItem> = buildList {
        addAll(SPALTE_RECHTS_OHNE_KATEGORIE.map { WaeschelisteArtikelItem(it, it) })
        addAll(BERUFSWAESCHE_WEISS.map { WaeschelisteArtikelItem(it, it) })
        addAll(BERUFSWAESCHE_DUNKEL.map { WaeschelisteArtikelItem(it, it) })
    }
}
