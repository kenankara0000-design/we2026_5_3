package com.example.we2026_5.wasch

/**
 * Parst OCR-Text vom Wäscheliste-Formular (feste Reihenfolge: Name, Adresse, Telefon, dann Artikl+Mengen).
 * Zahlen werden erkannt; Striche (||||) in Phase 2.
 */
object WaeschelisteOcrParser {

    /** Ergebnis des Parsings – nur befüllte Felder (leer = nicht erkannt). */
    data class OcrResult(
        val name: String = "",
        val adresse: String = "",
        val telefon: String = "",
        val mengen: Map<String, Int> = emptyMap(),
        val sonstiges: String = ""
    )

    private val allKeys by lazy { WaeschelisteArtikel.allKeys().sortedByDescending { it.length } }

    fun parse(fullText: String): OcrResult {
        val lines = fullText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return OcrResult()

        var name = ""
        var adresse = ""
        var telefon = ""
        val mengen = mutableMapOf<String, Int>()
        val sonstigesLines = mutableListOf<String>()

        var index = 0
        if (lines.size > 0) name = lines[0]
        if (lines.size > 1) adresse = lines[1]
        if (lines.size > 2) telefon = lines[2]
        index = 3

        for (i in index until lines.size) {
            val line = lines[i]
            val (matchedKey, menge) = extractArtikelAndMenge(line)
            if (matchedKey != null && menge >= 0) {
                mengen[matchedKey] = menge
            } else if (line.isNotBlank()) {
                sonstigesLines.add(line)
            }
        }

        return OcrResult(
            name = name,
            adresse = adresse,
            telefon = telefon,
            mengen = mengen,
            sonstiges = sonstigesLines.joinToString("\n").trim()
        )
    }

    /** Sucht einen Artikel-Key in der Zeile (längste Übereinstimmung) und extrahiert die Menge (Zahl). */
    private fun extractArtikelAndMenge(line: String): Pair<String?, Int> {
        val normalizedLine = line.replace(",", ".").trim()
        for (key in allKeys) {
            if (!normalizedLine.contains(key, ignoreCase = true)) continue
            val menge = extractMengeFromLine(normalizedLine, key)
            return key to menge
        }
        return null to -1
    }

    /** Liest eine Zahl aus der Zeile – am Ende oder direkt nach dem Artikelnamen. */
    private fun extractMengeFromLine(line: String, articleKey: String): Int {
        val afterKey = line.substringAfter(articleKey, "").trim()
        val digitsOnly = afterKey.replace(Regex("[^0-9]"), " ")
        val numbers = Regex("\\d+").findAll(digitsOnly).map { it.value.toIntOrNull() ?: 0 }.toList()
        if (numbers.isNotEmpty()) return numbers.max().coerceIn(0, 9999)
        val atEnd = Regex("\\d+\\s*$").find(line)?.value?.trim()?.toIntOrNull()
        return (atEnd ?: 0).coerceIn(0, 9999)
    }
}
