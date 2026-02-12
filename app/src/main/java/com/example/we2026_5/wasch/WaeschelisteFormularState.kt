package com.example.we2026_5.wasch

/**
 * Zustand des Wäscheliste-Formulars (Kundendaten + Mengen pro Artikel + Sonstiges).
 * Getrennt vom ViewModel gehalten, damit Formular-Logik und spätere OCR-Befüllung
 * einfach wartbar bleiben.
 */
data class WaeschelisteFormularState(
    val name: String = "",
    val adresse: String = "",
    val telefon: String = "",
    /** Menge pro Artikel-Key (nur Keys mit Menge > 0 müssen gespeichert werden). */
    val mengen: Map<String, Int> = emptyMap(),
    val sonstiges: String = ""
) {
    fun mengeForKey(key: String): Int = mengen[key] ?: 0

    fun withMenge(key: String, value: Int): WaeschelisteFormularState {
        val newMap = if (value == 0) mengen - key else mengen + (key to value)
        return copy(mengen = newMap)
    }

    fun withKundendaten(name: String, adresse: String, telefon: String): WaeschelisteFormularState =
        copy(name = name, adresse = adresse, telefon = telefon)

    fun withSonstiges(sonstiges: String): WaeschelisteFormularState = copy(sonstiges = sonstiges)

    /** Summe aller Stückzahlen (für Anzeige GESAMT). */
    fun gesamtStueck(): Int = mengen.values.sum()

    /** Übernimmt OCR-Ergebnis: nur nicht-leere Felder überschreiben, Mengen zusammenführen. */
    fun mergeOcrResult(ocr: WaeschelisteOcrParser.OcrResult): WaeschelisteFormularState {
        var s = this
        if (ocr.name.isNotBlank()) s = s.copy(name = ocr.name)
        if (ocr.adresse.isNotBlank()) s = s.copy(adresse = ocr.adresse)
        if (ocr.telefon.isNotBlank()) s = s.copy(telefon = ocr.telefon)
        if (ocr.sonstiges.isNotBlank()) s = s.copy(sonstiges = ocr.sonstiges)
        ocr.mengen.forEach { (key, value) -> if (value > 0) s = s.withMenge(key, value) }
        return s
    }
}
