package com.example.we2026_5.wasch

/**
 * Parst OCR-Text vom Wäscheliste-Formular.
 * Es wird nur der Bereich zwischen "Wäscheliste" und "Vielen Dank für Ihren Auftrag!" ausgewertet;
 * Kopf- und Fußbereich werden ignoriert. Unlesbares wird nicht in Sonstiges geschrieben.
 * Name, Adresse, Telefon per Label; danach Artikl+Mengen. Sonstiges nur manuell.
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
        val allLines = fullText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (allLines.isEmpty()) return OcrResult()

        // Nur den Bereich zwischen "Wäscheliste" und "Vielen Dank für Ihren Auftrag!" auswerten
        val startIdx = allLines.indexOfFirst { it.uppercase().contains("WÄSCHELISTE") }.takeIf { it >= 0 } ?: 0
        val endIdx = allLines.indexOfFirst { it.uppercase().contains("VIELEN DANK") }.takeIf { it >= 0 } ?: allLines.size
        val lines = if (startIdx < endIdx) allLines.subList(startIdx, endIdx) else emptyList()

        var name = ""
        var adresse = ""
        var telefon = ""
        val mengen = mutableMapOf<String, Int>()
        val consumedIndices = mutableSetOf<Int>()

        // Name, Adresse, Telefon per Label (NAME / ADRESSE / TELEFON)
        for (i in lines.indices) {
            val line = lines[i]
            val upper = line.uppercase()
            when {
                upper.startsWith("NAME") -> {
                    var value = line.substringAfter("NAME", "").trim().ifBlank { null }
                        ?: line.substringAfter("name", "").trim().ifBlank { null }
                    if (value.isNullOrBlank() && i + 1 < lines.size) {
                        value = lines[i + 1].trim()
                        if (value.isNotBlank()) consumedIndices.add(i + 1)
                    }
                    if (!value.isNullOrBlank()) name = value
                }
                upper.startsWith("ADRESSE") -> {
                    var value = line.substringAfter("ADRESSE", "").trim().ifBlank { null }
                        ?: line.substringAfter("adresse", "").trim().ifBlank { null }
                    if (value.isNullOrBlank() && i + 1 < lines.size) {
                        value = lines[i + 1].trim()
                        if (value.isNotBlank()) consumedIndices.add(i + 1)
                    }
                    if (!value.isNullOrBlank()) adresse = value
                }
                upper.startsWith("TELEFON") -> {
                    var value = line.substringAfter("TELEFON", "").trim().ifBlank { null }
                        ?: line.substringAfter("telefon", "").trim().ifBlank { null }
                    if (value.isNullOrBlank() && i + 1 < lines.size) {
                        value = lines[i + 1].trim()
                        if (value.isNotBlank()) consumedIndices.add(i + 1)
                    }
                    if (!value.isNullOrBlank() && !isBusinessContact(value)) telefon = value
                }
            }
        }

        for (i in lines.indices) {
            if (i in consumedIndices) continue
            val line = lines[i]
            val (matchedKey, menge) = extractArtikelAndMenge(line)
            if (matchedKey != null && menge >= 0) {
                mengen[matchedKey] = menge
            }
            // Unlesbares oder nicht zugeordnetes wird nicht in Sonstiges geschrieben – nur manuell ergänzen
        }

        return OcrResult(
            name = name,
            adresse = adresse,
            telefon = telefon,
            mengen = mengen,
            sonstiges = ""
        )
    }

    /** Geschäfts-Kontaktdaten aus dem Fußbereich (nicht als Kunden-Telefon übernehmen). */
    private fun isBusinessContact(value: String): Boolean {
        val v = value.replace(" ", "").replace("/", "")
        return v.contains("034206") || v.contains("279500") || v.contains("0176")
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

    /** Häkchen-Zeichen entfernen, damit OCR-Fehler (✔ als 4 gelesen) die Menge nicht verfälschen. */
    private fun stripCheckmarks(s: String): String = s.replace(Regex("[✔✓☑√✗✘]"), " ")

    /**
     * Liest die Menge aus der Zeile – nur Ziffern zählen; Häkchen ignorieren.
     * Nimmt die **erste** sinnvolle Zahl (1–999), nicht das Maximum, damit "2✔" (als 2+4 erkannt) → 2.
     */
    private fun extractMengeFromLine(line: String, articleKey: String): Int {
        val clean = stripCheckmarks(line)
        val afterKey = clean.substringAfter(articleKey, "").trim()
        val beforeKey = clean.substringBefore(articleKey, "").trim()
        val numbersAfter = Regex("\\d+").findAll(afterKey.replace(Regex("[^0-9]"), " ")).map { it.value.toIntOrNull() ?: 0 }.filter { it in 1..999 }.toList()
        val numbersBefore = Regex("\\d+").findAll(beforeKey.replace(Regex("[^0-9]"), " ")).map { it.value.toIntOrNull() ?: 0 }.filter { it in 1..999 }.toList()
        val firstReasonable = numbersBefore.firstOrNull() ?: numbersAfter.firstOrNull()
        if (firstReasonable != null) return firstReasonable.coerceIn(0, 9999)
        val any = Regex("\\d+").find(clean)?.value?.toIntOrNull()
        return (any ?: 0).coerceIn(0, 9999)
    }
}
