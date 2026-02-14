package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.ListItem
import com.example.we2026_5.SectionType
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminInfo
import java.util.concurrent.TimeUnit

/** Ergebnis von processTourData: Liste ohne Erledigt-Bereich + Erledigt-Daten für Button/Sheet. */
data class TourProcessResult(
    val items: List<ListItem>,
    val erledigtCount: Int,
    val erledigtDoneOhneListen: List<Customer>,
    val erledigtTourListen: List<Pair<String, List<Customer>>>
)

/**
 * Prozessor für Tour-Datenverarbeitung.
 * Extrahiert die komplexe Datenverarbeitungslogik aus TourPlannerViewModel.
 * Nutzt TerminCache für 365-Tage-Berechnung, filtert auf benötigtes Fenster.
 */
class TourDataProcessor(
    private val termincache: TerminCache
) {

    private val categorizer = TourDataCategorizer()
    private val filter = TourDataFilter(categorizer, termincache)
    private val erledigtDetector = TourErledigtDetector(categorizer)
    private val overdueDetector = TourOverdueDetector(categorizer)
    private val wochentagslistenProcessor = WochentagslistenProcessorImpl(categorizer, filter)
    private val tourListenProcessor = TourListenProcessorImpl(categorizer, filter, termincache, this::wurdeAmTagVollstaendigErledigt)

    fun processTourData(
        allCustomers: List<Customer>,
        allListen: List<KundenListe>,
        selectedTimestamp: Long,
        expandedSections: Set<SectionType>
    ): TourProcessResult {
        val viewDateStart = categorizer.getStartOfDay(selectedTimestamp)
        val heuteStart = categorizer.getStartOfDay(System.currentTimeMillis())
        
        // Alle Kunden nach Listen gruppieren
        val kundenNachListen = allCustomers.filter { it.listeId.isNotEmpty() }.groupBy { it.listeId }
        val kundenOhneListe = allCustomers.filter { it.listeId.isEmpty() }
        
        // Kunden in Listen filtern
        val listenMitKunden = mutableMapOf<String, List<Customer>>()
        wochentagslistenProcessor.fill(allCustomers, allListen, listenMitKunden, viewDateStart, heuteStart)
        tourListenProcessor.fill(kundenNachListen, allListen, listenMitKunden, viewDateStart, heuteStart)
        // Sammle Kunden-IDs: nur Listen ohne Wochentag (listeId), NICHT Wochentagslisten – G/P in Wochentagslisten gehen in Erledigt
        val alleKundenInListenIds = kundenNachListen.values.flatten().map { it.id }.toSet()
        val kundenInListenIds = listenMitKunden.values.flatten().map { it.id }.toSet()
        
        // Gewerblich- und Privat-Kunden ohne Liste filtern (Fällig-/Überfällig-Logik für alle Kundentypen)
        // WICHTIG: Filtere ALLE Kunden mit listeId heraus, unabhängig von kundenArt
        // Kunden aus Listen werden nur in Listen-Bereichen angezeigt, nicht in normalen Bereichen
        val kundenOhneListeMitTerminen = kundenOhneListe.filter { 
            (it.kundenArt == "Gewerblich" || it.kundenArt == "Privat") && it.listeId.isEmpty() && it.id !in alleKundenInListenIds 
        }
        val filteredGewerblich = kundenOhneListeMitTerminen.filter { customer ->
            val kwErledigtAmTag = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
                TerminBerechnungUtils.isTimestampInBerlinDay(customer.keinerWäscheErledigtAm, viewDateStart)
            val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTag

            // Erledigte Kunden: Prüfe ob am Tag ein Termin vorhanden ist
            if (isDone) {
                val termine = berechneAlleTermineFuerKunde(customer, allListen, viewDateStart, heuteStart)
                val termineAmTag = termine.filter { categorizer.getStartOfDay(it.datum) == viewDateStart }
                
                val warUeberfaelligUndErledigtAmDatum = erledigtDetector.warUeberfaelligUndErledigtAmDatum(customer, viewDateStart)
                
                // Prüfe ob am Tag erledigt wurde (wenn A und L am Tag: beide müssen erledigt sein)
                val hatAbholungAmTag = termineAmTag.any { it.typ == TerminTyp.ABHOLUNG }
                val hatAuslieferungAmTag = termineAmTag.any { it.typ == TerminTyp.AUSLIEFERUNG }
                val wurdeAmTagErledigt = erledigtDetector.wurdeAmTagVollstaendigErledigt(customer, viewDateStart, hatAbholungAmTag, hatAuslieferungAmTag, kwErledigtAmTag)
                
                // Erledigte Kunden anzeigen wenn: überfällig war und erledigt, oder am Tag vollständig erledigt (bei A+L: beide)
                if (warUeberfaelligUndErledigtAmDatum || wurdeAmTagErledigt) {
                    return@filter true
                }
            }
            
            // Nicht erledigte Kunden: Prüfe ob überfällig oder normal fällig (Kunden mit Urlaub weiterhin anzeigen mit U-Badge)
            val faelligAm = filter.customerFaelligAm(customer, null, viewDateStart)
            val isOverdue = filter.istKundeUeberfaellig(customer, null, viewDateStart, heuteStart)
            if (isOverdue) {
                return@filter true
            }
            
            filter.hatKundeTerminAmDatum(customer, null, viewDateStart)
        }
        
        // Liste mit Items erstellen
        val items = mutableListOf<ListItem>()
        
        // Gewerblich-Kunden OHNE Liste kategorisieren
        val overdueGewerblich = mutableListOf<Customer>()
        val normalGewerblich = mutableListOf<Customer>()
        val doneGewerblich = mutableListOf<Customer>()
        
        filteredGewerblich.forEach { customer ->
            // Sicherstellen, dass Kunden aus Listen nicht in normalen Bereichen erscheinen
            // WICHTIG: Prüfe sowohl listeId als auch alleKundenInListenIds
            if (customer.listeId.isNotEmpty() || customer.id in alleKundenInListenIds) {
                return@forEach
            }
            
            val alleTermine = berechneAlleTermineFuerKunde(customer, allListen, viewDateStart, heuteStart)
            val termineAmTag = alleTermine.filter { categorizer.getStartOfDay(it.datum) == viewDateStart }
            val hatAbholungAmTag = termineAmTag.any { it.typ == TerminTyp.ABHOLUNG }
            val hatAuslieferungAmTag = termineAmTag.any { it.typ == TerminTyp.AUSLIEFERUNG }
            val hatUeberfaelligeAbholung = overdueDetector.hatUeberfaelligeAbholung(customer, alleTermine, viewDateStart, heuteStart)
            val hatUeberfaelligeAuslieferung = overdueDetector.hatUeberfaelligeAuslieferung(customer, alleTermine, viewDateStart, heuteStart)
            
            val kwErledigtAmTag = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
                TerminBerechnungUtils.isTimestampInBerlinDay(customer.keinerWäscheErledigtAm, viewDateStart)
            val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTag

            // "Am Tag relevant" = fällig am Tag ODER überfällig und heute angezeigt (damit A+L beide nötig sind)
            val hatAbholungRelevantAmTag = hatAbholungAmTag || hatUeberfaelligeAbholung
            val hatAuslieferungRelevantAmTag = hatAuslieferungAmTag || hatUeberfaelligeAuslieferung
            val beideRelevantAmTag = hatAbholungRelevantAmTag && hatAuslieferungRelevantAmTag
            
            // Prüfe ob Kunde erledigt ist UND am angezeigten Tag einen Termin hat/hatte
                val sollAlsErledigtAnzeigen = if (isDone) {
                    val wurdeAmTagErledigt = erledigtDetector.wurdeAmTagVollstaendigErledigt(customer, viewDateStart, hatAbholungRelevantAmTag, hatAuslieferungRelevantAmTag, kwErledigtAmTag)
                
                // Wenn heute BEIDE A und L relevant (z. B. überfälliges A + fälliges L): nur vollständig erledigt zählt
                if (beideRelevantAmTag) {
                    wurdeAmTagErledigt
                } else {
                    erledigtDetector.warUeberfaelligUndErledigtAmDatum(customer, viewDateStart) || wurdeAmTagErledigt
                }
            } else {
                false
            }
            
            // LOGIK: Ein Kunde = eine Karte
            // Ü-Button zeigt an: "Dieser Kunde hat überfällige Termine"
            // A/L Buttons erledigen alle Termine dieses Typs (überfällig + normal)
            // Überfällige Termine werden nur am Fälligkeitstag (Tag X) und am heutigen Tag (Tag Y) angezeigt
            
            val hatUeberfaelligeTermine = hatUeberfaelligeAbholung || hatUeberfaelligeAuslieferung
            
            // 1. Überfällige Kunden → Überfällig-Bereich (unsichtbar, nur zur Trennung)
            if (hatUeberfaelligeTermine) {
                overdueGewerblich.add(customer)
            }
            // 2. Erledigte Kunden (nur wenn NICHT überfällig)
            else if (sollAlsErledigtAnzeigen) {
                doneGewerblich.add(customer)
            }
            // 3. Normale Kunden (nur normale Termine, keine überfälligen)
            else if (hatAbholungAmTag || hatAuslieferungAmTag) {
                normalGewerblich.add(customer)
            }
        }
        // REIHENFOLGE: 1. Überfällig (unsichtbar), 2. Listen, 3. Normal, 4. Erledigt
        
        // 1. Überfällige Kunden (unsichtbarer Bereich) - GANZ OBEN
        // Nur Kunden OHNE Liste ohne Wochentag (listeId.isEmpty); Listenkunden aus Listen ohne Wochentag bleiben in ihrer Listen-Card
        val alleUeberfaelligeKunden = mutableListOf<Customer>()
        alleUeberfaelligeKunden.addAll(overdueGewerblich)
        // Überfällige aus Wochentagslisten
        allListen.filter { it.wochentag in 0..6 }.forEach { liste ->
            listenMitKunden[liste.id]?.forEach { customer ->
                if (filter.istKundeUeberfaellig(customer, liste, viewDateStart, heuteStart)) {
                    alleUeberfaelligeKunden.add(customer)
                }
            }
        }
        // Überfällige aus Listen ohne Wochentag (Listenkunden-Listen) – erscheinen ebenfalls in Sektion „Überfällig“
        allListen.filter { it.wochentag !in 0..6 }.forEach { liste ->
            listenMitKunden[liste.id]?.forEach { customer ->
                if (filter.istKundeUeberfaellig(customer, liste, viewDateStart, heuteStart)) {
                    alleUeberfaelligeKunden.add(customer)
                }
            }
        }
        // Ein Kunde nur einmal in Überfällig (auch wenn in mehreren Listen)
        val alleUeberfaelligeEindeutig = alleUeberfaelligeKunden.distinctBy { it.id }
        val overdueOhneListen = alleUeberfaelligeEindeutig.sortedWith(compareBy<Customer> { customer ->
            val alleTermine = berechneAlleTermineFuerKunde(customer, allListen, viewDateStart, heuteStart)
            val ueberfaelligeDaten = alleTermine.filter { termin ->
                if (viewDateStart > heuteStart) return@filter false
                val terminStart = categorizer.getStartOfDay(termin.datum)
                val istUeberfaellig = overdueDetector.istTerminUeberfaellig(terminStart, viewDateStart, heuteStart)
                val istNichtErledigt = (termin.typ == TerminTyp.ABHOLUNG && !customer.abholungErfolgt) ||
                    (termin.typ == TerminTyp.AUSLIEFERUNG && !customer.auslieferungErfolgt)
                istUeberfaellig && istNichtErledigt
            }.map { categorizer.getStartOfDay(it.datum) }
            ueberfaelligeDaten.minOrNull() ?: Long.MAX_VALUE
        }.thenBy { it.name })
        
        // 1. Überfällige Kunden ganz oben – einzeln mit Überfällig-Design (kein Container)
        overdueOhneListen.forEach { c ->
            val liste = allListen.find { it.id == c.listeId }
            val alleTermineC = berechneAlleTermineFuerKunde(c, allListen, viewDateStart, heuteStart)
            val overdueSuffix = overdueDetector.getOverdueAlSuffix(c, alleTermineC, viewDateStart, heuteStart)
            items.add(ListItem.CustomerItem(c, statusBadgeText = TourPlannerStatusBadge.compute(c, viewDateStart, heuteStart, null, liste), isOverdue = true, overdueAlSuffix = overdueSuffix))
        }
        // Ein Kunde pro Tag nur einmal: bereits in Überfällig angezeigte nicht nochmal in Listen/Normal
        val bereitsAngezeigtCustomerIds = overdueOhneListen.map { it.id }.toSet().toMutableSet()

        val istVergangenheit = viewDateStart < heuteStart

        // 2. Kunden nach Listen: zuerst Listen ohne Wochentag (unter Überfällige, oberhalb normale Kunden), dann Wochentagslisten
        // Vergangenheit: keine „normalen“ Listen-Karten (nur Überfällig/Erledigt), aber Erledigt-Daten für Sheet brauchen wir immer
        val tourListenErledigt = mutableListOf<Pair<KundenListe, List<Customer>>>()
        val bereitsAngezeigtWochentag = mutableSetOf<Pair<Int, String>>()

        fun sammleErledigteInListen() {
            (allListen.filter { it.wochentag !in 0..6 } + allListen.filter { it.wochentag in 0..6 }).forEach { liste ->
                val kundenInListe = listenMitKunden[liste.id] ?: return@forEach
                if (kundenInListe.isEmpty()) return@forEach
                val erledigteKundenInListe = mutableListOf<Customer>()
                kundenInListe.forEach { customer ->
                    val kwErledigtAmTagListe = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
                        TerminBerechnungUtils.isTimestampInBerlinDay(customer.keinerWäscheErledigtAm, viewDateStart)
                    val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTagListe
                    val sollAlsErledigtAnzeigen = if (isDone) {
                        val termine = berechneAlleTermineFuerKunde(customer, allListen, viewDateStart, heuteStart)
                        val termineAmTag = termine.filter { categorizer.getStartOfDay(it.datum) == viewDateStart }
                        val warUeberfaelligUndErledigt = isDone && erledigtDetector.warUeberfaelligUndErledigtAmDatum(customer, viewDateStart)
                        val hatAbholungAmTagListe = termineAmTag.any { it.typ == TerminTyp.ABHOLUNG }
                        val hatAuslieferungAmTagListe = termineAmTag.any { it.typ == TerminTyp.AUSLIEFERUNG }
                        erledigtDetector.wurdeAmTagVollstaendigErledigt(customer, viewDateStart, hatAbholungAmTagListe, hatAuslieferungAmTagListe, kwErledigtAmTagListe) || warUeberfaelligUndErledigt
                    } else false
                    if (sollAlsErledigtAnzeigen) erledigteKundenInListe.add(customer)
                }
                if (erledigteKundenInListe.isNotEmpty()) {
                    tourListenErledigt.add(liste to erledigteKundenInListe.sortedBy { it.name })
                }
            }
        }

        if (!istVergangenheit) {
            // 2a. Listen ohne Wochentag (wochentag !in 0..6) – direkt unter Überfällige
            allListen.filter { it.wochentag !in 0..6 }.sortedBy { it.name }.forEach { liste ->
                val kundenInListe = listenMitKunden[liste.id] ?: return@forEach
                if (kundenInListe.isNotEmpty()) {
                    val nichtErledigteKunden = mutableListOf<Triple<Customer, Boolean, String?>>()
                    val erledigteKundenInListe = mutableListOf<Customer>()
                    kundenInListe.forEach { customer ->
                        val istUeberfaellig = filter.istKundeUeberfaellig(customer, liste, viewDateStart, heuteStart)
                        val kwErledigtAmTagListe = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
                            TerminBerechnungUtils.isTimestampInBerlinDay(customer.keinerWäscheErledigtAm, viewDateStart)
                        val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTagListe
                        val sollAlsErledigtAnzeigen = if (isDone) {
                            val termine = berechneAlleTermineFuerKunde(customer, allListen, viewDateStart, heuteStart)
                            val termineAmTag = termine.filter { categorizer.getStartOfDay(it.datum) == viewDateStart }
                            val warUeberfaelligUndErledigtAmDatum = isDone && erledigtDetector.warUeberfaelligUndErledigtAmDatum(customer, viewDateStart)
                            val hatAbholungAmTagListe = termineAmTag.any { it.typ == TerminTyp.ABHOLUNG }
                            val hatAuslieferungAmTagListe = termineAmTag.any { it.typ == TerminTyp.AUSLIEFERUNG }
                            erledigtDetector.wurdeAmTagVollstaendigErledigt(customer, viewDateStart, hatAbholungAmTagListe, hatAuslieferungAmTagListe, kwErledigtAmTagListe) || warUeberfaelligUndErledigtAmDatum
                        } else false
                        if (sollAlsErledigtAnzeigen) erledigteKundenInListe.add(customer)
                        else {
                            val alleTermineCust = berechneAlleTermineFuerKunde(customer, allListen, viewDateStart, heuteStart)
                            val suffix = if (istUeberfaellig) overdueDetector.getOverdueAlSuffix(customer, alleTermineCust, viewDateStart, heuteStart) else null
                            nichtErledigteKunden.add(Triple(customer, istUeberfaellig, suffix))
                        }
                    }
                    val kundenWithOverdue = nichtErledigteKunden.sortedBy { (c, _, _) -> c.name }
                    if (kundenWithOverdue.isNotEmpty()) {
                        var aCount = 0
                        var lCount = 0
                        kundenWithOverdue.forEach { (c, _, _) ->
                            bereitsAngezeigtCustomerIds.add(c.id)
                            val termine = berechneAlleTermineFuerKunde(c, allListen, viewDateStart, heuteStart)
                            val termineAmTag = termine.filter { categorizer.getStartOfDay(it.datum) == viewDateStart }
                            aCount += termineAmTag.count { it.typ == TerminTyp.ABHOLUNG }
                            lCount += termineAmTag.count { it.typ == TerminTyp.AUSLIEFERUNG }
                        }
                        items.add(ListItem.TourListeCard(liste, kundenWithOverdue, aCount, lCount))
                    }
                    if (erledigteKundenInListe.isNotEmpty()) {
                        tourListenErledigt.add(liste to erledigteKundenInListe.sortedBy { it.name })
                    }
                }
            }
            // 2b. Wochentagslisten (wochentag in 0..6) – unter Listen ohne Wochentag, oberhalb normale Kunden
            allListen.filter { it.wochentag in 0..6 }.sortedBy { it.name }.forEach { liste ->
                val kundenInListe = listenMitKunden[liste.id] ?: return@forEach
                if (kundenInListe.isNotEmpty()) {
                    val nichtErledigteKunden = mutableListOf<Pair<Customer, Boolean>>()
                    val erledigteKundenInListe = mutableListOf<Customer>()
                    kundenInListe.forEach { customer ->
                        val istUeberfaellig = filter.istKundeUeberfaellig(customer, liste, viewDateStart, heuteStart)
                        val kwErledigtAmTagListe = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
                            TerminBerechnungUtils.isTimestampInBerlinDay(customer.keinerWäscheErledigtAm, viewDateStart)
                        val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTagListe
                        val sollAlsErledigtAnzeigen = if (isDone) {
                            val termine = berechneAlleTermineFuerKunde(customer, allListen, viewDateStart, heuteStart)
                            val termineAmTag = termine.filter { categorizer.getStartOfDay(it.datum) == viewDateStart }
                            val warUeberfaelligUndErledigtAmDatum = isDone && erledigtDetector.warUeberfaelligUndErledigtAmDatum(customer, viewDateStart)
                            val hatAbholungAmTagListe = termineAmTag.any { it.typ == TerminTyp.ABHOLUNG }
                            val hatAuslieferungAmTagListe = termineAmTag.any { it.typ == TerminTyp.AUSLIEFERUNG }
                            erledigtDetector.wurdeAmTagVollstaendigErledigt(customer, viewDateStart, hatAbholungAmTagListe, hatAuslieferungAmTagListe, kwErledigtAmTagListe) || warUeberfaelligUndErledigtAmDatum
                        } else false
                        if (sollAlsErledigtAnzeigen) erledigteKundenInListe.add(customer)
                        else nichtErledigteKunden.add(customer to istUeberfaellig)
                        // Überfällige aus Wochentagslisten: bereits im Überfällig-Bereich, werden per bereitsAngezeigtCustomerIds übersprungen
                    }
                    nichtErledigteKunden.sortedBy { (c, _) -> c.name }.forEach { (customer, _) ->
                        if (customer.id in bereitsAngezeigtCustomerIds) return@forEach
                        val key = liste.wochentag to customer.id
                        if (key in bereitsAngezeigtWochentag) return@forEach
                        bereitsAngezeigtWochentag.add(key)
                        bereitsAngezeigtCustomerIds.add(customer.id)
                        items.add(ListItem.CustomerItem(customer, statusBadgeText = TourPlannerStatusBadge.compute(customer, viewDateStart, heuteStart)))
                    }
                }
            }
        } else {
            // Vergangenheit: nur Erledigt-Daten für Sheet sammeln (keine Listen-Karten)
            sammleErledigteInListen()
        }

        // 3. Normale Kunden (ohne solche, die bereits in einer Liste für diesen Tag stehen – z. B. Wochentagsliste)
        // Vergangenheit: keine normalen Karten; jeder Kunde pro Tag nur einmal
        val normalOhneListen = normalGewerblich.filter { it.id !in kundenInListenIds }.sortedBy { it.name }
        if (!istVergangenheit) {
            normalOhneListen.forEach { customer ->
                if (customer.id in bereitsAngezeigtCustomerIds) return@forEach
                bereitsAngezeigtCustomerIds.add(customer.id)
                items.add(ListItem.CustomerItem(customer, statusBadgeText = TourPlannerStatusBadge.compute(customer, viewDateStart, heuteStart)))
            }
        }
        // Erledigt-Daten für Button „Erledigte (N)“ und Bottom-Sheet: Sortierung neueste zuerst (zuletzt erledigt oben)
        val doneOhneListen = doneGewerblich.sortedWith(
            compareByDescending<Customer> { erledigtDetector.erledigtZeitstempelAmTag(it, viewDateStart) }.thenBy { it.name }
        )
        val tourListenPairs = tourListenErledigt.map { (liste, kunden) ->
            liste.name to kunden.sortedWith(
                compareByDescending<Customer> { erledigtDetector.erledigtZeitstempelAmTag(it, viewDateStart) }.thenBy { it.name }
            )
        }
        val erledigtGesamtCount = doneOhneListen.size + tourListenPairs.sumOf { it.second.size }
        return TourProcessResult(items, erledigtGesamtCount, doneOhneListen, tourListenPairs)
    }

    /**
     * Gibt die Anzahl der Termine zurück, die am angegebenen Tag (nur Heute) fällig oder überfällig
     * und noch nicht erledigt sind. Erledigte („erledigt heute“) werden nicht mitgezählt.
     */
    fun getFälligCount(
        allCustomers: List<Customer>,
        allListen: List<KundenListe>,
        selectedTimestamp: Long
    ): Int {
        val result = processTourData(allCustomers, allListen, selectedTimestamp, emptySet())
        return result.items.sumOf { item ->
            when (item) {
                is ListItem.CustomerItem -> if (item.isErledigtAmTag) 0 else 1
                is ListItem.SectionHeader -> if (item.sectionType == SectionType.OVERDUE) item.kunden.size else 0
                is ListItem.ListeHeader -> item.nichtErledigteKunden.size
                is ListItem.TourListeCard -> item.kunden.size
                is ListItem.TourListeErledigt -> 0
                is ListItem.ErledigtSection -> 0
            }
        }
    }
    
    /** Wrapper für TourListenProcessorImpl-Callback (delegiert an TourErledigtDetector). */
    private fun wurdeAmTagVollstaendigErledigt(
        customer: Customer,
        viewDateStart: Long,
        hatAbholungAmTag: Boolean,
        hatAuslieferungAmTag: Boolean,
        kwErledigtAmTag: Boolean
    ): Boolean = erledigtDetector.wurdeAmTagVollstaendigErledigt(customer, viewDateStart, hatAbholungAmTag, hatAuslieferungAmTag, kwErledigtAmTag)

    // Öffentliche Methoden für Zugriff auf Helper-Klassen (für TourPlannerWeekDataProcessor) (für TourPlannerWeekDataProcessor)
    fun getStartOfDay(ts: Long): Long = categorizer.getStartOfDay(ts)
    fun customerFaelligAm(c: Customer, liste: KundenListe? = null, abDatum: Long = System.currentTimeMillis()): Long = 
        filter.customerFaelligAm(c, liste, abDatum)
    fun hatKundeTerminAmDatum(customer: Customer, liste: KundenListe? = null, viewDateStart: Long): Boolean = 
        filter.hatKundeTerminAmDatum(customer, liste, viewDateStart)
    fun istKundeUeberfaellig(customer: Customer, liste: KundenListe? = null, viewDateStart: Long, heuteStart: Long): Boolean = 
        filter.istKundeUeberfaellig(customer, liste, viewDateStart, heuteStart)
    fun isIntervallFaelligAm(intervall: ListeIntervall, datum: Long): Boolean = 
        filter.isIntervallFaelligAm(intervall, datum)
    fun isIntervallFaelligInZukunft(intervall: ListeIntervall, abDatum: Long): Boolean = 
        filter.isIntervallFaelligInZukunft(intervall, abDatum)
    fun getNaechstesListeDatum(liste: KundenListe, abDatum: Long = System.currentTimeMillis(), geloeschteTermine: List<Long> = emptyList()): Long? = 
        categorizer.getNaechstesListeDatum(liste, abDatum, geloeschteTermine)

    /**
     * Liefert Termine aus Cache (365 Tage) gefiltert auf das benötigte Fenster.
     * Bei Listen-Kunden: liste mit listenTermine wird einbezogen.
     */
    private fun berechneAlleTermineFuerKunde(customer: Customer, allListen: List<KundenListe>, viewDateStart: Long, heuteStart: Long): List<TerminInfo> {
        val (startDatum, tageVoraus) = if (viewDateStart <= heuteStart) {
            Pair(heuteStart - TimeUnit.DAYS.toMillis(60), 63)
        } else {
            Pair(viewDateStart - TimeUnit.DAYS.toMillis(1), 3)
        }
        val liste = allListen.find { it.id == customer.listeId }
        return termincache.getTermineInRange(customer, startDatum, tageVoraus, liste)
    }
}
