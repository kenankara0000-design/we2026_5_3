package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.ListItem
import com.example.we2026_5.SectionType
import com.example.we2026_5.util.TerminFilterUtils
import java.util.concurrent.TimeUnit

/**
 * Prozessor für Tour-Datenverarbeitung.
 * Extrahiert die komplexe Datenverarbeitungslogik aus TourPlannerViewModel.
 */
class TourDataProcessor {
    
    private val categorizer = TourDataCategorizer()
    private val filter = TourDataFilter(categorizer)
    
    fun processTourData(
        allCustomers: List<Customer>,
        allListen: List<KundenListe>,
        selectedTimestamp: Long,
        expandedSections: Set<SectionType>
    ): List<ListItem> {
        val viewDateStart = categorizer.getStartOfDay(selectedTimestamp)
        val heuteStart = categorizer.getStartOfDay(System.currentTimeMillis())
        
        // Alle Kunden nach Listen gruppieren
        val kundenNachListen = allCustomers.filter { it.listeId.isNotEmpty() }.groupBy { it.listeId }
        val kundenOhneListe = allCustomers.filter { it.listeId.isEmpty() }
        
        // Kunden in Listen filtern
        val listenMitKunden = mutableMapOf<String, List<Customer>>()
        kundenNachListen.forEach { (listeId, kunden) ->
            if (listeId.isEmpty()) return@forEach
            val liste = allListen.find { it.id == listeId } ?: return@forEach
            
            val istFaellig = liste.intervalle.any { intervall ->
                filter.isIntervallFaelligAm(intervall, viewDateStart) || 
                filter.isIntervallFaelligInZukunft(intervall, viewDateStart)
            }
            
            if (istFaellig) {
                val fälligeKunden = kunden.filter { customer ->
                    val faelligAm = filter.customerFaelligAm(customer, liste, viewDateStart)
                    val customerImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                           faelligAm in customer.urlaubVon..customer.urlaubBis
                    val listeImUrlaub = liste?.let { 
                        it.urlaubVon > 0 && it.urlaubBis > 0 && faelligAm in it.urlaubVon..it.urlaubBis 
                    } ?: false
                    if (customerImUrlaub || listeImUrlaub) return@filter false
                    
                    val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt
                    
                    // WICHTIG: Erledigte Kunden aus Listen werden angezeigt wenn sie erledigt sind UND am Tag einen Termin haben
                    if (isDone) {
                        val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                            customer = customer,
                            liste = liste,
                            startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365),
                            tageVoraus = 730
                        )
                        val termineAmTag = termine.filter { categorizer.getStartOfDay(it.datum) == viewDateStart }
                        
                        // Prüfe ob überfällig war und am Datum erledigt wurde
                        val warUeberfaelligUndErledigtAmDatum = if (customer.faelligAmDatum > 0) {
                            val faelligAmStart = categorizer.getStartOfDay(customer.faelligAmDatum)
                            val erledigtAmStart = if (customer.abholungErledigtAm > 0) {
                                categorizer.getStartOfDay(customer.abholungErledigtAm)
                            } else if (customer.auslieferungErledigtAm > 0) {
                                categorizer.getStartOfDay(customer.auslieferungErledigtAm)
                            } else {
                                0L
                            }
                            viewDateStart == faelligAmStart || (erledigtAmStart > 0 && viewDateStart == erledigtAmStart)
                        } else {
                            false
                        }
                        
                        // Prüfe ob am angezeigten Tag ein Termin erledigt wurde (wenn A und L am Tag: beide müssen erledigt sein)
                        val hatAbholungAmTag = termineAmTag.any { it.typ == com.example.we2026_5.TerminTyp.ABHOLUNG }
                        val hatAuslieferungAmTag = termineAmTag.any { it.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG }
                        val wurdeAmTagErledigt = wurdeAmTagVollstaendigErledigt(customer, viewDateStart, hatAbholungAmTag, hatAuslieferungAmTag)
                        
                        // Erledigte Kunden anzeigen wenn:
                        // 1. Überfällig war und am Datum erledigt wurde, ODER
                        // 2. Am Tag vollständig erledigt (bei A+L: beide müssen erledigt sein)
                        if (warUeberfaelligUndErledigtAmDatum || wurdeAmTagErledigt) {
                            return@filter true
                        }
                    }
                    
                    // Nicht erledigte Kunden: Prüfe ob überfällig oder normal fällig
                    val isOverdue = filter.istKundeUeberfaellig(customer, liste, viewDateStart, heuteStart)
                    if (isOverdue) {
                        return@filter true
                    }
                    
                    filter.hatKundeTerminAmDatum(customer, liste, viewDateStart)
                }
                
                if (fälligeKunden.isNotEmpty()) {
                    listenMitKunden[listeId] = fälligeKunden.sortedBy { it.name }
                }
            }
        }
        
        // Sammle alle Kunden-IDs die in Listen sind (um Doppelungen zu vermeiden)
        // WICHTIG: Sammle ALLE Kunden aus Listen, nicht nur die fälligen
        val alleKundenInListenIds = kundenNachListen.values.flatten().map { it.id }.toSet()
        val kundenInListenIds = listenMitKunden.values.flatten().map { it.id }.toSet()
        
        // Gewerblich-Kunden ohne Liste filtern (Kunden aus Listen sind bereits oben angezeigt)
        // WICHTIG: Filtere ALLE Kunden mit listeId heraus, unabhängig von kundenArt
        // Kunden aus Listen werden nur in Listen-Bereichen angezeigt, nicht in normalen Bereichen
        val gewerblichKundenOhneListe = kundenOhneListe.filter { 
            it.kundenArt == "Gewerblich" && it.listeId.isEmpty() && it.id !in alleKundenInListenIds 
        }
        val filteredGewerblich = gewerblichKundenOhneListe.filter { customer ->
            val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt
            
            // Erledigte Kunden: Prüfe ob am Tag ein Termin vorhanden ist
            if (isDone) {
                val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                    customer = customer,
                    startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365),
                    tageVoraus = 730
                )
                val termineAmTag = termine.filter { categorizer.getStartOfDay(it.datum) == viewDateStart }
                
                // Prüfe ob überfällig war und am Datum erledigt wurde
                val warUeberfaelligUndErledigtAmDatum = if (customer.faelligAmDatum > 0) {
                    val faelligAmStart = categorizer.getStartOfDay(customer.faelligAmDatum)
                    val erledigtAmStart = if (customer.abholungErledigtAm > 0) {
                        categorizer.getStartOfDay(customer.abholungErledigtAm)
                    } else if (customer.auslieferungErledigtAm > 0) {
                        categorizer.getStartOfDay(customer.auslieferungErledigtAm)
                    } else {
                        0L
                    }
                    viewDateStart == faelligAmStart || (erledigtAmStart > 0 && viewDateStart == erledigtAmStart)
                } else {
                    false
                }
                
                // Prüfe ob am Tag erledigt wurde (wenn A und L am Tag: beide müssen erledigt sein)
                val hatAbholungAmTag = termineAmTag.any { it.typ == com.example.we2026_5.TerminTyp.ABHOLUNG }
                val hatAuslieferungAmTag = termineAmTag.any { it.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG }
                val wurdeAmTagErledigt = wurdeAmTagVollstaendigErledigt(customer, viewDateStart, hatAbholungAmTag, hatAuslieferungAmTag)
                
                // Erledigte Kunden anzeigen wenn: überfällig war und erledigt, oder am Tag vollständig erledigt (bei A+L: beide)
                if (warUeberfaelligUndErledigtAmDatum || wurdeAmTagErledigt) {
                    return@filter true
                }
            }
            
            // Nicht erledigte Kunden: Prüfe ob überfällig oder normal fällig
            val faelligAm = filter.customerFaelligAm(customer, null, viewDateStart)
            val faelligAmImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                   faelligAm in customer.urlaubVon..customer.urlaubBis
            if (faelligAmImUrlaub) return@filter false
            
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
            
            // Berechne ALLE Termine (nicht nur die am angezeigten Tag)
            val alleTermine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365),
                tageVoraus = 730
            )
            
            // Termine am angezeigten Tag (für normale Termine)
            val termineAmTag = alleTermine.filter { categorizer.getStartOfDay(it.datum) == viewDateStart }
            val hatAbholungAmTag = termineAmTag.any { it.typ == com.example.we2026_5.TerminTyp.ABHOLUNG }
            val hatAuslieferungAmTag = termineAmTag.any { it.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG }
            
            // Prüfe ob Kunde überfällige Termine hat (prüft ALLE Termine, nicht nur die am Tag)
            // LOGIK: Überfällige Termine werden nur am Fälligkeitstag (Tag X) und am heutigen Tag (Tag Y) angezeigt
            // - Am Tag X: Termin ist fällig und nicht erledigt → überfällig
            // - Am Tag Y (heute): Termin war vor heute fällig und nicht erledigt → überfällig
            // - Termine, die heute fällig sind (aber nicht überfällig) → normal behandelt
            val hatUeberfaelligeAbholung = alleTermine.any { termin ->
                val terminStart = categorizer.getStartOfDay(termin.datum)
                val istAbholung = termin.typ == com.example.we2026_5.TerminTyp.ABHOLUNG
                val istNichtErledigt = !customer.abholungErfolgt
                
                // Ein Termin ist überfällig, wenn:
                // 1. Er am Tag X fällig ist (terminStart == viewDateStart) UND nicht erledigt → überfällig am Tag X
                // 2. ODER er war vor heute fällig (terminStart < heuteStart) UND nicht erledigt UND wir sind heute (viewDateStart == heuteStart) → überfällig am Tag Y
                // NICHT überfällig: Termine, die heute fällig sind (terminStart == heuteStart) → normal behandelt
                
                val istAmTagXFaellig = terminStart == viewDateStart // Termin ist am angezeigten Tag (Tag X) fällig
                val warVorHeuteFaellig = terminStart < heuteStart // Termin war vor heute fällig
                val istHeute = viewDateStart == heuteStart // Wir sind am heutigen Tag (Tag Y)
                val istHeuteFaellig = terminStart == heuteStart // Termin ist heute fällig
                
                // Überfällig wenn:
                // - Termin ist am Tag X fällig UND nicht erledigt (wird am Tag X angezeigt)
                // - ODER Termin war vor heute fällig UND nicht erledigt UND wir sind heute (wird am Tag Y angezeigt)
                // NICHT überfällig: Termine, die heute fällig sind (werden normal behandelt)
                val istUeberfaellig = (istAmTagXFaellig || (warVorHeuteFaellig && istHeute)) && !istHeuteFaellig
                
                istAbholung && istNichtErledigt && istUeberfaellig
            }
            
            val hatUeberfaelligeAuslieferung = alleTermine.any { termin ->
                val terminStart = categorizer.getStartOfDay(termin.datum)
                val istAuslieferung = termin.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG
                val istNichtErledigt = !customer.auslieferungErfolgt
                
                val istAmTagXFaellig = terminStart == viewDateStart
                val warVorHeuteFaellig = terminStart < heuteStart
                val istHeute = viewDateStart == heuteStart
                val istHeuteFaellig = terminStart == heuteStart
                
                val istUeberfaellig = (istAmTagXFaellig || (warVorHeuteFaellig && istHeute)) && !istHeuteFaellig
                
                istAuslieferung && istNichtErledigt && istUeberfaellig
            }
            
            val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt
            
            // "Am Tag relevant" = fällig am Tag ODER überfällig und heute angezeigt (damit A+L beide nötig sind)
            val hatAbholungRelevantAmTag = hatAbholungAmTag || hatUeberfaelligeAbholung
            val hatAuslieferungRelevantAmTag = hatAuslieferungAmTag || hatUeberfaelligeAuslieferung
            val beideRelevantAmTag = hatAbholungRelevantAmTag && hatAuslieferungRelevantAmTag
            
            // Prüfe ob Kunde erledigt ist UND am angezeigten Tag einen Termin hat/hatte
            val sollAlsErledigtAnzeigen = if (isDone) {
                val wurdeAmTagErledigt = wurdeAmTagVollstaendigErledigt(customer, viewDateStart, hatAbholungRelevantAmTag, hatAuslieferungRelevantAmTag)
                
                // Wenn heute BEIDE A und L relevant (z. B. überfälliges A + fälliges L): nur vollständig erledigt zählt
                if (beideRelevantAmTag) {
                    wurdeAmTagErledigt
                } else {
                    // Sonst: überfällig war und erledigt ODER am Tag vollständig erledigt
                    val warUeberfaelligUndErledigt = if (customer.faelligAmDatum > 0) {
                        val faelligAmStart = categorizer.getStartOfDay(customer.faelligAmDatum)
                        val erledigtAmStart = if (customer.abholungErledigtAm > 0) {
                            categorizer.getStartOfDay(customer.abholungErledigtAm)
                        } else if (customer.auslieferungErledigtAm > 0) {
                            categorizer.getStartOfDay(customer.auslieferungErledigtAm)
                        } else {
                            0L
                        }
                        viewDateStart == faelligAmStart || (erledigtAmStart > 0 && viewDateStart == erledigtAmStart)
                    } else {
                        false
                    }
                    warUeberfaelligUndErledigt || wurdeAmTagErledigt
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
        
        // 1. Überfällige Kunden (unsichtbarer Bereich, nur zur Trennung) - GANZ OBEN
        // Sammle ALLE überfälligen Kunden (mit und ohne Liste)
        val alleUeberfaelligeKunden = mutableListOf<Customer>()
        
        // Überfällige Kunden ohne Liste
        alleUeberfaelligeKunden.addAll(overdueGewerblich)
        
        // Überfällige Kunden aus Listen hinzufügen
        listenMitKunden.values.flatten().forEach { customer ->
            val istUeberfaellig = filter.istKundeUeberfaellig(customer, allListen.find { it.id == customer.listeId }, viewDateStart, heuteStart)
            if (istUeberfaellig) {
                alleUeberfaelligeKunden.add(customer)
            }
        }
        
        val overdueOhneListen = alleUeberfaelligeKunden.sortedWith(compareBy<Customer> { customer ->
            // Sortiere nach ältestem Überfälligkeitsdatum
            val liste = if (customer.listeId.isNotEmpty()) allListen.find { it.id == customer.listeId } else null
            val alleTermine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                liste = liste,
                startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365),
                tageVoraus = 730
            )
            val ueberfaelligeDaten = alleTermine.filter { termin ->
                val terminStart = categorizer.getStartOfDay(termin.datum)
                val istAmTagXFaellig = terminStart == viewDateStart
                val warVorHeuteFaellig = terminStart < heuteStart
                val istHeute = viewDateStart == heuteStart
                val istHeuteFaellig = terminStart == heuteStart
                val istUeberfaellig = (istAmTagXFaellig || (warVorHeuteFaellig && istHeute)) && !istHeuteFaellig
                val istNichtErledigt = (termin.typ == com.example.we2026_5.TerminTyp.ABHOLUNG && !customer.abholungErfolgt) ||
                        (termin.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG && !customer.auslieferungErfolgt)
                istUeberfaellig && istNichtErledigt
            }.map { categorizer.getStartOfDay(it.datum) }
            
            ueberfaelligeDaten.minOrNull() ?: Long.MAX_VALUE
        }.thenBy { it.name })
        
        // 1. Überfällige Kunden ganz oben, ohne Container, sortiert nach ältester Überfälligkeit
        if (overdueOhneListen.isNotEmpty()) {
            overdueOhneListen.forEach { items.add(ListItem.CustomerItem(it)) }
        }
        
        // 2. Kunden nach Listen gruppiert
        allListen.sortedBy { it.name }.forEach { liste ->
            val kundenInListe = listenMitKunden[liste.id] ?: return@forEach
            if (kundenInListe.isNotEmpty()) {
                // Trenne Kunden in erledigt und nicht erledigt
                val nichtErledigteKunden = mutableListOf<Customer>()
                val erledigteKundenInListe = mutableListOf<Customer>()
                
                kundenInListe.forEach { customer ->
                    // Überfällige Kunden aus Listen werden oben im Überfällig-Bereich angezeigt, nicht hier
                    val istUeberfaellig = filter.istKundeUeberfaellig(customer, liste, viewDateStart, heuteStart)
                    if (istUeberfaellig) {
                        return@forEach // Überspringe überfällige Kunden - sie werden oben angezeigt
                    }
                    
                    val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt
                    val listeDone = liste.abholungErfolgt || liste.auslieferungErfolgt
                    
                    // Prüfe ob Kunde erledigt ist UND am angezeigten Tag einen Termin hat/hatte
                    val sollAlsErledigtAnzeigen = if (isDone || listeDone) {
                        // Berechne alle Termine für den Kunden
                        val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                            customer = customer,
                            liste = liste,
                            startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365),
                            tageVoraus = 730
                        )
                        val termineAmTag = termine.filter { categorizer.getStartOfDay(it.datum) == viewDateStart }
                        
                        // Prüfe ob überfällig war und am Datum erledigt wurde
                        val warUeberfaelligUndErledigtAmDatum = if (isDone && customer.faelligAmDatum > 0) {
                            val faelligAmStart = categorizer.getStartOfDay(customer.faelligAmDatum)
                            val erledigtAmStart = if (customer.abholungErledigtAm > 0) {
                                categorizer.getStartOfDay(customer.abholungErledigtAm)
                            } else if (customer.auslieferungErledigtAm > 0) {
                                categorizer.getStartOfDay(customer.auslieferungErledigtAm)
                            } else {
                                0L
                            }
                            viewDateStart == faelligAmStart || (erledigtAmStart > 0 && viewDateStart == erledigtAmStart)
                        } else {
                            false
                        }
                        
                        // Prüfe ob am angezeigten Tag ein Termin erledigt wurde (wenn A und L am Tag: beide müssen erledigt sein)
                        val hatAbholungAmTagListe = termineAmTag.any { it.typ == com.example.we2026_5.TerminTyp.ABHOLUNG }
                        val hatAuslieferungAmTagListe = termineAmTag.any { it.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG }
                        val wurdeAmTagErledigt = wurdeAmTagVollstaendigErledigt(customer, viewDateStart, hatAbholungAmTagListe, hatAuslieferungAmTagListe)
                        
                        // Erledigte Kunden anzeigen wenn:
                        // 1. Überfällig war und am Datum erledigt wurde, ODER
                        // 2. Am Tag vollständig erledigt (bei A+L: beide müssen erledigt sein)
                        warUeberfaelligUndErledigtAmDatum || wurdeAmTagErledigt
                    } else {
                        false
                    }
                    
                    if (sollAlsErledigtAnzeigen) {
                        erledigteKundenInListe.add(customer)
                    } else {
                        nichtErledigteKunden.add(customer)
                    }
                }
                
                val gesamtKunden = nichtErledigteKunden.size + erledigteKundenInListe.size
                // Kunden direkt im Header speichern - keine separaten Items nötig!
                items.add(ListItem.ListeHeader(
                    liste.name, 
                    gesamtKunden, 
                    erledigteKundenInListe.size, 
                    liste.id,
                    nichtErledigteKunden.sortedBy { it.name },
                    erledigteKundenInListe.sortedBy { it.name }
                ))
            }
        }
        
        // 3. Normale Kunden
        val normalOhneListen = normalGewerblich.sortedBy { it.name }
        normalOhneListen.forEach { items.add(ListItem.CustomerItem(it)) }
        
        // 4. Erledigt-Bereich – Kunden im Header-Container (wie Überfällig)
        val doneOhneListen = doneGewerblich.sortedBy { it.name }
        if (doneOhneListen.isNotEmpty()) {
            items.add(ListItem.SectionHeader("ERLEDIGT", doneOhneListen.size, doneOhneListen.size, SectionType.DONE, doneOhneListen))
        }
        
        return items
    }
    
    /**
     * Prüft ob der Termin am angezeigten Tag vollständig erledigt wurde.
     * Regel: Wenn am Tag sowohl A (Abholung) als auch L (Auslieferung) fällig sind,
     * müssen BEIDE gedrückt sein, damit der Termin als erledigt zählt.
     */
    private fun wurdeAmTagVollstaendigErledigt(
        customer: Customer,
        viewDateStart: Long,
        hatAbholungAmTag: Boolean,
        hatAuslieferungAmTag: Boolean
    ): Boolean {
        val abholungErledigtAmTag = customer.abholungErledigtAm > 0 && getStartOfDay(customer.abholungErledigtAm) == viewDateStart
        val auslieferungErledigtAmTag = customer.auslieferungErledigtAm > 0 && getStartOfDay(customer.auslieferungErledigtAm) == viewDateStart
        return when {
            hatAbholungAmTag && hatAuslieferungAmTag -> abholungErledigtAmTag && auslieferungErledigtAmTag
            hatAbholungAmTag -> abholungErledigtAmTag
            hatAuslieferungAmTag -> auslieferungErledigtAmTag
            else -> false
        }
    }
    
    // Öffentliche Methoden für Zugriff auf Helper-Klassen (für TourPlannerWeekDataProcessor)
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
}
