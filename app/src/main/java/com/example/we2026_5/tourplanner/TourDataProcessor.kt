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
        // 1) Wochentagslisten – nur Gruppierung nach Tag, nur G/P (keine Tour), nur NICHT-erledigte
        allListen.filter { it.wochentag in 0..6 }.forEach { liste ->
            val kunden = allCustomers.filter { k ->
                (k.kundenArt == "Gewerblich" || k.kundenArt == "Privat") &&
                k.listeId.isEmpty() &&
                (k.defaultAbholungWochentag == liste.wochentag || k.defaultAuslieferungWochentag == liste.wochentag)
            }
            val fälligeKunden = kunden.filter { customer ->
                val kwErledigtAmTag = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
                    categorizer.getStartOfDay(customer.keinerWäscheErledigtAm) == viewDateStart
                val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTag

                // Erledigte G/P aus Wochentagslisten gehen in Erledigt-Bereich, nicht hier
                if (isDone) return@filter false

                val isOverdue = filter.istKundeUeberfaellig(customer, null, viewDateStart, heuteStart)
                if (isOverdue) return@filter true

                filter.hatKundeTerminAmDatum(customer, null, viewDateStart)
            }
            if (fälligeKunden.isNotEmpty()) {
                listenMitKunden[liste.id] = fälligeKunden.sortedBy { it.name }
            }
        }

        // 2) Alte Listen (listeId)
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
                    // Kunden mit Urlaub weiterhin anzeigen (mit U-Badge im Tourenplaner)
                    val kwErledigtAmTag = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
                        categorizer.getStartOfDay(customer.keinerWäscheErledigtAm) == viewDateStart
                    val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTag

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
                        val wurdeAmTagErledigt = wurdeAmTagVollstaendigErledigt(customer, viewDateStart, hatAbholungAmTag, hatAuslieferungAmTag, kwErledigtAmTag)
                        
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
        
        // Sammle Kunden-IDs: nur Tour-Listen (listeId), NICHT Wochentagslisten – G/P in Wochentagslisten gehen in Erledigt
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
                categorizer.getStartOfDay(customer.keinerWäscheErledigtAm) == viewDateStart
            val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTag

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
                val wurdeAmTagErledigt = wurdeAmTagVollstaendigErledigt(customer, viewDateStart, hatAbholungAmTag, hatAuslieferungAmTag, kwErledigtAmTag)
                
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
            // Überfällig = Fälligkeit VOR heute (terminStart < heuteStart). Nie „morgen“ als überfällig anzeigen.
            val nurHeuteOderVergangenheit = viewDateStart <= heuteStart
            val hatUeberfaelligeAbholung = nurHeuteOderVergangenheit && alleTermine.any { termin ->
                val terminStart = categorizer.getStartOfDay(termin.datum)
                val istAbholung = termin.typ == com.example.we2026_5.TerminTyp.ABHOLUNG
                val istNichtErledigt = !customer.abholungErfolgt
                val warVorHeuteFaellig = terminStart < heuteStart
                val istAmFaelligkeitstag = terminStart == viewDateStart
                val istHeute = viewDateStart == heuteStart
                val istHeuteFaellig = terminStart == heuteStart
                val istUeberfaellig = (istAmFaelligkeitstag || (warVorHeuteFaellig && istHeute)) && !istHeuteFaellig
                istAbholung && istNichtErledigt && istUeberfaellig
            }
            
            val hatUeberfaelligeAuslieferung = nurHeuteOderVergangenheit && alleTermine.any { termin ->
                val terminStart = categorizer.getStartOfDay(termin.datum)
                val istAuslieferung = termin.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG
                val istNichtErledigt = !customer.auslieferungErfolgt
                val warVorHeuteFaellig = terminStart < heuteStart
                val istAmFaelligkeitstag = terminStart == viewDateStart
                val istHeute = viewDateStart == heuteStart
                val istHeuteFaellig = terminStart == heuteStart
                val istUeberfaellig = (istAmFaelligkeitstag || (warVorHeuteFaellig && istHeute)) && !istHeuteFaellig
                istAuslieferung && istNichtErledigt && istUeberfaellig
            }
            
            val kwErledigtAmTag = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
                categorizer.getStartOfDay(customer.keinerWäscheErledigtAm) == viewDateStart
            val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTag

            // "Am Tag relevant" = fällig am Tag ODER überfällig und heute angezeigt (damit A+L beide nötig sind)
            val hatAbholungRelevantAmTag = hatAbholungAmTag || hatUeberfaelligeAbholung
            val hatAuslieferungRelevantAmTag = hatAuslieferungAmTag || hatUeberfaelligeAuslieferung
            val beideRelevantAmTag = hatAbholungRelevantAmTag && hatAuslieferungRelevantAmTag
            
            // Prüfe ob Kunde erledigt ist UND am angezeigten Tag einen Termin hat/hatte
            val sollAlsErledigtAnzeigen = if (isDone) {
                val wurdeAmTagErledigt = wurdeAmTagVollstaendigErledigt(customer, viewDateStart, hatAbholungRelevantAmTag, hatAuslieferungRelevantAmTag, kwErledigtAmTag)
                
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
                if (viewDateStart > heuteStart) return@filter false
                val terminStart = categorizer.getStartOfDay(termin.datum)
                val warVorHeuteFaellig = terminStart < heuteStart
                val istAmFaelligkeitstag = terminStart == viewDateStart
                val istHeute = viewDateStart == heuteStart
                val istHeuteFaellig = terminStart == heuteStart
                val istUeberfaellig = (istAmFaelligkeitstag || (warVorHeuteFaellig && istHeute)) && !istHeuteFaellig
                val istNichtErledigt = (termin.typ == com.example.we2026_5.TerminTyp.ABHOLUNG && !customer.abholungErfolgt) ||
                        (termin.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG && !customer.auslieferungErfolgt)
                istUeberfaellig && istNichtErledigt
            }.map { categorizer.getStartOfDay(it.datum) }
            
            ueberfaelligeDaten.minOrNull() ?: Long.MAX_VALUE
        }.thenBy { it.name })
        
        // 1. Überfällige Kunden ganz oben – einzeln mit Überfällig-Design (kein Container)
        overdueOhneListen.forEach { items.add(ListItem.CustomerItem(it, isOverdue = true)) }
        
        // 2. Kunden nach Listen gruppiert – Wochentagslisten: nur nicht-erledigte; Tour-Listen: nur nicht-erledigte
        // Erledigte Tour-Kunden gehen in Erledigt-Bereich unter ihrer Tour-Liste
        val tourListenErledigt = mutableListOf<Pair<KundenListe, List<Customer>>>()
        allListen.sortedBy { it.name }.forEach { liste ->
            val kundenInListe = listenMitKunden[liste.id] ?: return@forEach
            if (kundenInListe.isNotEmpty()) {
                val nichtErledigteKunden = mutableListOf<Customer>()
                val erledigteKundenInListe = mutableListOf<Customer>()
                
                kundenInListe.forEach { customer ->
                    val istUeberfaellig = filter.istKundeUeberfaellig(customer, liste, viewDateStart, heuteStart)
                    if (istUeberfaellig) return@forEach
                    
                    val kwErledigtAmTagListe = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
                        categorizer.getStartOfDay(customer.keinerWäscheErledigtAm) == viewDateStart
                    val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt || kwErledigtAmTagListe
                    val listeDone = liste.abholungErfolgt || liste.auslieferungErfolgt

                    val sollAlsErledigtAnzeigen = if (isDone || listeDone) {
                        val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                            customer = customer, liste = liste,
                            startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365), tageVoraus = 730
                        )
                        val termineAmTag = termine.filter { categorizer.getStartOfDay(it.datum) == viewDateStart }
                        val warUeberfaelligUndErledigtAmDatum = if (isDone && customer.faelligAmDatum > 0) {
                            val faelligAmStart = categorizer.getStartOfDay(customer.faelligAmDatum)
                            val erledigtAmStart = if (customer.abholungErledigtAm > 0) {
                                categorizer.getStartOfDay(customer.abholungErledigtAm)
                            } else if (customer.auslieferungErledigtAm > 0) {
                                categorizer.getStartOfDay(customer.auslieferungErledigtAm)
                            } else 0L
                            viewDateStart == faelligAmStart || (erledigtAmStart > 0 && viewDateStart == erledigtAmStart)
                        } else false
                        val hatAbholungAmTagListe = termineAmTag.any { it.typ == com.example.we2026_5.TerminTyp.ABHOLUNG }
                        val hatAuslieferungAmTagListe = termineAmTag.any { it.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG }
                        val wurdeAmTagErledigt = wurdeAmTagVollstaendigErledigt(customer, viewDateStart, hatAbholungAmTagListe, hatAuslieferungAmTagListe, kwErledigtAmTagListe)
                        warUeberfaelligUndErledigtAmDatum || wurdeAmTagErledigt
                    } else false
                    
                    if (sollAlsErledigtAnzeigen) {
                        erledigteKundenInListe.add(customer)
                    } else {
                        nichtErledigteKunden.add(customer)
                    }
                }
                
                // Nur nicht-erledigte in der Liste anzeigen
                nichtErledigteKunden.sortedBy { it.name }.forEach { items.add(ListItem.CustomerItem(it)) }
                // Tour-Listen (wochentag=-1): erledigte unter Erledigt-Bereich sammeln
                if (erledigteKundenInListe.isNotEmpty() && (liste.wochentag !in 0..6)) {
                    tourListenErledigt.add(liste to erledigteKundenInListe.sortedBy { it.name })
                }
            }
        }
        
        // 3. Normale Kunden
        val normalOhneListen = normalGewerblich.sortedBy { it.name }
        normalOhneListen.forEach { items.add(ListItem.CustomerItem(it)) }
        
        // 4. Erledigt-Bereich – G/P einzeln + Tour-Listen mit ihren erledigten Kunden
        val doneOhneListen = doneGewerblich.sortedBy { it.name }
        val erledigtGesamtCount = doneOhneListen.size + tourListenErledigt.sumOf { it.second.size }
        if (erledigtGesamtCount > 0) {
            items.add(ListItem.SectionHeader("ERLEDIGT", erledigtGesamtCount, erledigtGesamtCount, SectionType.DONE, doneOhneListen))
            if (SectionType.DONE in expandedSections) {
                doneOhneListen.forEach { items.add(ListItem.CustomerItem(it, isErledigtAmTag = true)) }
                tourListenErledigt.forEach { (liste, kunden) ->
                    items.add(ListItem.TourListeErledigt(liste.name, kunden))
                }
            }
        }
        
        return items
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
        val items = processTourData(allCustomers, allListen, selectedTimestamp, emptySet())
        return items.sumOf { item ->
            when (item) {
                is ListItem.CustomerItem -> if (item.isErledigtAmTag) 0 else 1
                is ListItem.SectionHeader -> if (item.sectionType == SectionType.OVERDUE) item.kunden.size else 0
                is ListItem.ListeHeader -> item.nichtErledigteKunden.size
                is ListItem.TourListeErledigt -> 0
            }
        }
    }
    
    /**
     * Prüft ob der Termin am angezeigten Tag vollständig erledigt wurde.
     * Regel: Wenn am Tag sowohl A (Abholung) als auch L (Auslieferung) fällig sind,
     * müssen BEIDE gedrückt sein, damit der Termin als erledigt zählt.
     * Keine Wäsche (KW) am Tag zählt ebenfalls als erledigt.
     */
    private fun wurdeAmTagVollstaendigErledigt(
        customer: Customer,
        viewDateStart: Long,
        hatAbholungAmTag: Boolean,
        hatAuslieferungAmTag: Boolean,
        kwErledigtAmTag: Boolean
    ): Boolean {
        val abholungErledigtAmTag = customer.abholungErledigtAm > 0 && getStartOfDay(customer.abholungErledigtAm) == viewDateStart
        val auslieferungErledigtAmTag = customer.auslieferungErledigtAm > 0 && getStartOfDay(customer.auslieferungErledigtAm) == viewDateStart
        return when {
            hatAbholungAmTag && hatAuslieferungAmTag -> abholungErledigtAmTag && auslieferungErledigtAmTag
            hatAbholungAmTag -> abholungErledigtAmTag
            hatAuslieferungAmTag -> auslieferungErledigtAmTag
            else -> kwErledigtAmTag
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
