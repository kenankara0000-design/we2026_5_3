package com.example.we2026_5

import com.example.we2026_5.util.TerminBerechnungUtils
import com.google.firebase.database.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlin.DeprecationLevel
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
    /** Anzeigename in der App; für Rechnung wird der offizielle Name (name) verwendet. Leer = name verwenden. */
    val alias: String = "",
    val adresse: String = "",
    /** Optionale GPS-Koordinaten für Navigation (z. B. aus Geocoding oder manuell). */
    val latitude: Double? = null,
    val longitude: Double? = null,
    val telefon: String = "",
    val notizen: String = "",
    val stadt: String = "",
    val plz: String = "",
    val tags: List<String> = emptyList(),
    // Kunden-Art und Liste
    val kundenArt: String = "Gewerblich", // "Gewerblich", "Privat" oder "Listenkunden"
    val listeId: String = "", // ID der Liste (nur für Privat-Kunden)
    // Termine - NEUE STRUKTUR: Mehrere Intervalle pro Kunde
    val intervalle: List<CustomerIntervall> = emptyList(), // Liste der Intervalle (für Gewerblich-Kunden)
    
    // Termine - ALTE STRUKTUR (@Exclude: nicht mehr schreiben; nur optional aus DB lesen für Migration/Export)
    @Deprecated("Verwende intervalle statt einzelner Felder. Wird für Migration beibehalten.")
    @Exclude
    val abholungDatum: Long = 0, // Erstes Abholungsdatum
    @Deprecated("Verwende intervalle statt einzelner Felder. Wird für Migration beibehalten.")
    @Exclude
    val auslieferungDatum: Long = 0, // Erstes Auslieferungsdatum
    @Deprecated("Verwende intervalle statt einzelner Felder. Wird für Migration beibehalten.")
    @Exclude
    val wiederholen: Boolean = false, // Ob Termine wiederholt werden sollen
    @Deprecated("Verwende intervalle statt einzelner Felder. Wird für Migration beibehalten.")
    @Exclude
    val intervallTage: Int = 7, // Wiederholungs-Intervall (nur wenn wiederholen = true)
    @Deprecated("Verwende intervalle statt einzelner Felder. Wird für Migration beibehalten.")
    @Exclude
    val letzterTermin: Long = 0,
    @Deprecated("Wird nicht mehr verwendet")
    val wochentagOld: Int = 0,
    @Deprecated("Verwende defaultAbholungWochentag/defaultAuslieferungWochentag")
    val wochentag: String = "",
    val kundenTyp: KundenTyp = KundenTyp.REGELMAESSIG,
    @Deprecated("Verwende defaultAbholungWochentag/defaultAuslieferungWochentag")
    val listenWochentag: Int = -1,
    val kundennummer: String = "", // Optionale externe Referenz
    /** Erstellungsdatum (Tagesanfang); wenn > 0, Wochentags-Termine nur ab diesem Datum (keine Vergangenheit für neue Kunden), Überfällig weiter über Intervall/Vergangenheit. */
    val erstelltAm: Long = 0L,
    val defaultAbholungWochentag: Int = -1,
    val defaultAuslieferungWochentag: Int = -1,
    /** Mehrere A-Tage (0=Mo..6=So). Wenn leer, zählt defaultAbholungWochentag. */
    val defaultAbholungWochentage: List<Int> = emptyList(),
    /** Mehrere L-Tage (0=Mo..6=So). Wenn leer, zählt defaultAuslieferungWochentag. */
    val defaultAuslieferungWochentage: List<Int> = emptyList(),
    /** Gespeicherte Tage A→L (0–365). Wenn gesetzt, wird dieser Wert überall verwendet (z. B. neuer Kunden-Termin, unregelmäßig). Sonst Ableitung aus erstem Intervall. */
    val tageAzuL: Int? = null,
    /** Wenn A- und L-Wochentage gleich: 0 = L am selben Tag (L=A+0), 7 = L eine Woche später (L=A+7). null = Fallback 0. Nur relevant wenn effectiveAbholungWochentage == effectiveAuslieferungWochentage. */
    val sameDayLStrategy: Int? = null,
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
    /** true = Kunde wird nicht angefahren, bringt/holt selbst („Ohne Tour“). Tourenplaner zeigt ihn nicht. */
    val ohneTour: Boolean = false,
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
    
    /** Ausnahme-Termine (einmalig A oder L), Sonderfälle ohne Bezug zu regulären Terminen. @Exclude, manuell lesen/schreiben. */
    @Exclude
    val ausnahmeTermine: List<AusnahmeTermin> = emptyList(),

    /** Kunden-Termine: vom Kunden vorgegebene Abholung/Lieferung (z. B. fürs Jahr). Eigenständig, getrennt von Ausnahme-Terminen. @Exclude, manuell lesen/schreiben. */
    @Exclude
    val kundenTermine: List<KundenTermin> = emptyList(),

    /** Termine von der Tour-Liste (listenTermine) auf den Kunden übertragen. Bei Änderung der Liste werden alle Kunden der Liste aktualisiert; beim Verlassen der Liste geleert. @Exclude, manuell lesen/schreiben. */
    @Exclude
    val termineVonListe: List<KundenTermin> = emptyList(),

    val fotoUrls: List<String> = listOf(),
    /** Thumbnail-URLs für Listen/Vorschau (Prio 3 PLAN_PERFORMANCE_OFFLINE); gleiche Reihenfolge wie fotoUrls; Fallback: fotoUrls. */
    val fotoThumbUrls: List<String> = listOf(),
    val istImUrlaub: Boolean = false,
    val geloeschteTermine: List<Long> = listOf(), // Liste von gelöschten Termin-Daten (für einzelne Termin-Löschungen)
    // Dummy-Feld für Realtime Database - wird ignoriert beim Speichern/Laden
    // Verhindert Warnung "No setter/field for faelligAm found"
    @Exclude
    private val faelligAm: Long = 0
) {
    /** Anzeigename in der App: Alias, falls gesetzt, sonst name. Leer oder "null" → "–" (z. B. SevDesk-Personen ohne Namen). @Exclude: nur berechnet, nie in Firebase speichern. */
    @get:Exclude
    val displayName: String
        get() {
            val a = alias.trim()
            if (a.isNotEmpty()) return a
            val n = name.trim()
            if (n.isNotEmpty() && n.equals("null", ignoreCase = true).not()) return n
            return "–"
        }

    /**
     * Berechnet das nächste Fälligkeitsdatum basierend auf letzterTermin und Intervall.
     * Berücksichtigt verschobenAufDatum und gelöschte Termine.
     * Nutzt intern bereits [intervalle] bzw. Legacy-Felder; nur für Rückwärtskompatibilität.
     */
    @Deprecated(
        message = "Bevorzugt TerminBerechnungUtils.naechstesFaelligAmDatum(customer) nutzen.",
        level = DeprecationLevel.WARNING
    )
    fun getFaelligAm(): Long = com.example.we2026_5.util.TerminBerechnungUtils.naechstesFaelligAmDatum(this)

    /** Effektive A-Tage: Liste wenn gesetzt, sonst einzelner defaultAbholungWochentag falls gültig. @Exclude: nur berechnet, nie in Firebase speichern. */
    @get:Exclude
    val effectiveAbholungWochentage: List<Int>
        get() = if (defaultAbholungWochentage.isNotEmpty()) defaultAbholungWochentage
        else if (defaultAbholungWochentag in 0..6) listOf(defaultAbholungWochentag)
        else emptyList()

    /** Effektive L-Tage: Liste wenn gesetzt, sonst einzelner defaultAuslieferungWochentag falls gültig. @Exclude: nur berechnet, nie in Firebase speichern. */
    @get:Exclude
    val effectiveAuslieferungWochentage: List<Int>
        get() = if (defaultAuslieferungWochentage.isNotEmpty()) defaultAuslieferungWochentage
        else if (defaultAuslieferungWochentag in 0..6) listOf(defaultAuslieferungWochentag)
        else emptyList()
}