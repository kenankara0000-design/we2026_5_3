package com.example.we2026_5

import com.example.we2026_5.util.TerminFilterUtils
import com.example.we2026_5.util.TerminBerechnungUtils
import com.google.firebase.database.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.concurrent.TimeUnit

/** Ein Urlaubseintrag (Von–Bis). Mehrere pro Kunde möglich. */
data class UrlaubEintrag(
    val von: Long = 0L,
    val bis: Long = 0L
)

@IgnoreExtraProperties
data class Customer(
    val id: String = "",
    val name: String = "",
    val adresse: String = "",
    val telefon: String = "",
    val notizen: String = "",
    val stadt: String = "",
    val plz: String = "",
    val tags: List<String> = emptyList(),
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
    val wochentagOld: Int = 0,
    @Deprecated("Verwende defaultAbholungWochentag/defaultAuslieferungWochentag")
    val wochentag: String = "",
    val kundenTyp: KundenTyp = KundenTyp.REGELMAESSIG,
    @Deprecated("Verwende defaultAbholungWochentag/defaultAuslieferungWochentag")
    val listenWochentag: Int = -1,
    val kundennummer: String = "", // Optionale externe Referenz
    val defaultAbholungWochentag: Int = -1,
    val defaultAuslieferungWochentag: Int = -1,
    val defaultUhrzeit: String = "",
    val defaultZeitfenster: Zeitfenster? = null,
    val adHocTemplate: AdHocTemplate? = null,
    val aktiveTerminRegelId: String = "",
    val status: CustomerStatus = CustomerStatus.AKTIV,
    val pauseStart: Long = 0,
    val pauseEnde: Long = 0,
    val pauseGrund: String = "",
    val reaktivierungsDatum: Long = 0,
    val letzteErinnerung: Long = 0,
    val tourSlotId: String = "",
    val tourSlot: TourSlot? = null,
    val tourNotizen: String = "",
    val regelHistorie: List<RegelHistorieEintrag> = emptyList(),
    
    // Status
    val abholungErfolgt: Boolean = false,
    val auslieferungErfolgt: Boolean = false,
    /** Keine Wäsche (KW): A+KW = erledigt Abholungstag, L+KW = erledigt Auslieferungstag */
    val keinerWäscheErfolgt: Boolean = false,
    val keinerWäscheErledigtAm: Long = 0,
    val urlaubVon: Long = 0,
    val urlaubBis: Long = 0,
    /** Mehrere Urlaubseinträge pro Kunde. Falls leer, zählt weiterhin urlaubVon/urlaubBis. */
    val urlaubEintraege: List<UrlaubEintrag> = emptyList(),
    val urlaubAutoPause: Boolean = true,
    
    // Erledigungsdaten und Zeitstempel
    val abholungErledigtAm: Long = 0, // Datum wann Abholung erledigt wurde (nur Datum, ohne Uhrzeit)
    val auslieferungErledigtAm: Long = 0, // Datum wann Auslieferung erledigt wurde (nur Datum, ohne Uhrzeit)
    val abholungZeitstempel: Long = 0, // Zeitstempel mit Uhrzeit wann Abholung erledigt wurde
    val auslieferungZeitstempel: Long = 0, // Zeitstempel mit Uhrzeit wann Auslieferung erledigt wurde
    val faelligAmDatum: Long = 0, // Fälligkeitsdatum (für überfällige Kunden: speichert das Datum, an dem der Termin fällig war)
    
    // Verschieben-Logik - ERWEITERT für einzelne Termine (@Exclude: wird manuell in Repository gelesen/geschrieben, vermeidet Enum-Crash bei alter DB)
    @Exclude
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
                !com.example.we2026_5.util.TerminFilterUtils.istTerminGeloescht(it.datum, geloeschteTermine)
            }
            return naechstesTermin?.datum ?: 0L
        }
        
        // ALTE STRUKTUR: Rückwärtskompatibilität
        // Diese Logik sollte idealerweise migriert und dann entfernt werden.
        // If there are no new intervals, we fall back to the old single abholungDatum/wiederholen logic if present.
        if (abholungDatum > 0 || auslieferungDatum > 0) {
            val heuteForLegacy = System.currentTimeMillis() // 'heute' für Legacy-Code
            val altesIntervall = CustomerIntervall(
                id = "legacy",
                abholungDatum = abholungDatum,
                auslieferungDatum = auslieferungDatum,
                wiederholen = wiederholen,
                intervallTage = intervallTage,
                intervallAnzahl = 0 // Alte Struktur hat keine Anzahl
            )
            val termine = TerminBerechnungUtils.berechneTermineFuerIntervall(
                intervall = altesIntervall,
                startDatum = heuteForLegacy,
                tageVoraus = 365,
                geloeschteTermine = geloeschteTermine,
                verschobeneTermine = verschobeneTermine
            )
            val naechstesTermin = termine.firstOrNull { 
                it.datum >= TerminBerechnungUtils.getStartOfDay(heuteForLegacy) &&
                !TerminFilterUtils.istTerminGeloescht(it.datum, geloeschteTermine)
            }
            return naechstesTermin?.datum ?: 0L
        }

        return 0L    }
}