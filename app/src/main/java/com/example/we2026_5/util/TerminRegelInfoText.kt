package com.example.we2026_5.util

import com.example.we2026_5.TerminRegel
import com.example.we2026_5.ui.common.getWochentagFullResIds

/**
 * Erstellt einen lesbaren Info-Text für eine Termin-Regel (z. B. für Dialoge).
 * @param getString Funktion zum Abrufen von String-Ressourcen (z. B. context::getString)
 */
fun buildTerminRegelInfoText(regel: TerminRegel, getString: (Int) -> String): String = buildString {
    val wochentage = getWochentagFullResIds().map { getString(it) }
        append("Name: ${regel.name}\n\n")

        if (regel.beschreibung.isNotEmpty()) {
            append("Beschreibung: ${regel.beschreibung}\n\n")
        }

        if (regel.taeglich) {
            append("Typ: Täglich\n\n")
            if (regel.startDatum > 0) {
                append("Startdatum: ${DateFormatter.formatDateWithLeadingZeros(regel.startDatum)}\n")
            }
            append("Termine jeden Tag ab Startdatum (Abholung und Auslieferung am selben Tag).\n\n")
        } else if (regel.wochentagBasiert) {
            append("Typ: Wochentag-basiert\n\n")

            if (regel.startDatum > 0) {
                append("Startdatum: ${DateFormatter.formatDateWithLeadingZeros(regel.startDatum)}\n")
            }

            val abholTage = regel.abholungWochentage?.filter { it in 0..6 }?.distinct()?.sorted()
                ?: (if (regel.abholungWochentag in 0..6) listOf(regel.abholungWochentag) else emptyList())
            if (abholTage.isNotEmpty()) {
                append("Abholung: ${abholTage.joinToString(", ") { wochentage.getOrNull(it) ?: "" }}\n")
            }

            val auslTage = regel.auslieferungWochentage?.filter { it in 0..6 }?.distinct()?.sorted()
                ?: (if (regel.auslieferungWochentag in 0..6) listOf(regel.auslieferungWochentag) else emptyList())
            if (auslTage.isNotEmpty()) {
                append("Auslieferung: ${auslTage.joinToString(", ") { wochentage.getOrNull(it) ?: "" }}\n")
            }
            append("\n")
        } else {
            append("Typ: Datum-basiert\n\n")

            val abholungText = if (regel.abholungDatum > 0) {
                DateFormatter.formatDateWithLeadingZeros(regel.abholungDatum)
            } else "Heute"
            append("Abholung: $abholungText\n")

            val auslieferungText = if (regel.auslieferungDatum > 0) {
                DateFormatter.formatDateWithLeadingZeros(regel.auslieferungDatum)
            } else "Heute"
            append("Auslieferung: $auslieferungText\n\n")
        }

        if (regel.wiederholen) {
            append("Wiederholen: Ja\n")
            append("Intervall: Alle ${regel.intervallTage} Tage\n")
            if (regel.intervallAnzahl > 0) {
                append("Anzahl: ${regel.intervallAnzahl} Wiederholungen\n")
            } else {
                append("Anzahl: Unbegrenzt\n")
            }
        } else {
            append("Wiederholen: Nein\n")
        }

        append("\nVerwendungsanzahl: ${regel.verwendungsanzahl}x")
}
