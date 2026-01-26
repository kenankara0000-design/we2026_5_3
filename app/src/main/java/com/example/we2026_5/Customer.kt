package com.example.we2026_5

import com.google.firebase.database.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.concurrent.TimeUnit

@IgnoreExtraProperties
data class Customer(
    val id: String = "",
    val name: String = "",
    val adresse: String = "",
    val telefon: String = "",
    val notizen: String = "",
    // Kunden-Art und Liste
    val kundenArt: String = "Gewerblich", // "Privat" oder "Gewerblich"
    val listeId: String = "", // ID der Liste (nur für Privat-Kunden)
    // Termine - NEUE STRUKTUR: Mehrere Intervalle pro Kunde
    val intervalle: List<CustomerIntervall> = emptyList(), // Liste der Intervalle (für Gewerblich-Kunden)
    
    // Termine - ALTE STRUKTUR (für Rückwärtskompatibilität, @Deprecated)
    @Deprecated("Verwende intervalle statt einzelner Felder. Wird für Migration beibehalten.")
    val abholungDatum: Long = 0, // Erstes Abholungsdatum
    @Deprecated("Verwende intervalle statt einzelner Felder. Wird für Migration beibehalten.")
    val auslieferungDatum: Long = 0, // Erstes Auslieferungsdatum
    @Deprecated("Verwende intervalle statt einzelner Felder. Wird für Migration beibehalten.")
    val wiederholen: Boolean = false, // Ob Termine wiederholt werden sollen
    @Deprecated("Verwende intervalle statt einzelner Felder. Wird für Migration beibehalten.")
    val intervallTage: Int = 7, // Wiederholungs-Intervall (nur wenn wiederholen = true)
    @Deprecated("Verwende intervalle statt einzelner Felder. Wird für Migration beibehalten.")
    val letzterTermin: Long = 0,
    @Deprecated("Wird nicht mehr verwendet")
    val wochentag: Int = 0, // 0=Montag, 1=Dienstag, ..., 6=Sonntag (wird von Liste übernommen für Privat-Kunden)
    
    // Status
    val abholungErfolgt: Boolean = false,
    val auslieferungErfolgt: Boolean = false,
    val urlaubVon: Long = 0,
    val urlaubBis: Long = 0,
    
    // Verschieben-Logik - ERWEITERT für einzelne Termine
    @Deprecated("Verwende verschobeneTermine für einzelne Termine. Wird für Migration beibehalten.")
    val verschobenAufDatum: Long = 0, // Alte Logik: Verschiebt alle Termine
    val verschobeneTermine: List<VerschobenerTermin> = emptyList(), // NEUE Logik: Einzelne Termine verschieben
    
    val fotoUrls: List<String> = listOf(),
    val istImUrlaub: Boolean = false,
    val geloeschteTermine: List<Long> = listOf(), // Liste von gelöschten Termin-Daten (für einzelne Termin-Löschungen)
    // Dummy-Feld für Realtime Database - wird ignoriert beim Speichern/Laden
    // Verhindert Warnung "No setter/field for faelligAm found"
    @Exclude
    private val faelligAm: Long = 0
) {
    /**
     * Berechnet das nächste Fälligkeitsdatum basierend auf letzterTermin und Intervall.
     * Berücksichtigt verschobenAufDatum.
     * Wenn wiederholen = false, wird abholungDatum zurückgegeben.
     * 
     * @deprecated Verwende TerminBerechnungUtils.berechneAlleTermineFuerKunde() für neue Struktur
     * Diese Funktion bleibt für Rückwärtskompatibilität erhalten.
     */
    fun getFaelligAm(): Long {
        // NEUE STRUKTUR: Verwende Intervalle-Liste wenn vorhanden
        if (intervalle.isNotEmpty()) {
            val heute = System.currentTimeMillis()
            val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = this,
                startDatum = heute,
                tageVoraus = 365
            )
            // Nächstes fälliges Datum (nicht gelöscht, nicht in Vergangenheit)
            val naechstesTermin = termine.firstOrNull { 
                it.datum >= com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(heute) &&
                !com.example.we2026_5.util.TerminBerechnungUtils.istTerminGeloescht(it.datum, geloeschteTermine)
            }
            return naechstesTermin?.datum ?: 0L
        }
        
        // ALTE STRUKTUR: Rückwärtskompatibilität
        if (!wiederholen) {
            // Einmaliger Termin: Abholungsdatum verwenden
            return if (verschobenAufDatum > 0) verschobenAufDatum else abholungDatum
        }
        // Wiederholender Termin: Alte Logik
        if (verschobenAufDatum > 0) return verschobenAufDatum
        return letzterTermin + TimeUnit.DAYS.toMillis(intervallTage.toLong())
    }
}