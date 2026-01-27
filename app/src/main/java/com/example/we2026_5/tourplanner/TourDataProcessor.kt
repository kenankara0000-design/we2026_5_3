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
                    
                    // WICHTIG: Erledigte Kunden aus Listen werden IMMER angezeigt (in ihrem Erledigt-Bereich)
                    // Prüfe ob Kunde am angezeigten Tag erledigt wurde oder erledigt sein soll
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
                    
                    // Prüfe ob Kunde am angezeigten Tag erledigt wurde
                    val hatErledigtenTerminAmDatum = if (isDone) {
                        val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                            customer = customer,
                            liste = liste,
                            startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365),
                            tageVoraus = 730
                        )
                        val termineAmTag = termine.filter { categorizer.getStartOfDay(it.datum) == viewDateStart }
                        val hatErledigtenATermin = if (customer.abholungErfolgt) {
                            val abholungErledigtAmStart = if (customer.abholungErledigtAm > 0) getStartOfDay(customer.abholungErledigtAm) else 0L
                            if (abholungErledigtAmStart > 0 && viewDateStart == abholungErledigtAmStart) {
                                true
                            } else {
                                termineAmTag.any { termin ->
                                    termin.typ == com.example.we2026_5.TerminTyp.ABHOLUNG
                                }
                            }
                        } else {
                            false
                        }
                        val hatErledigtenLTermin = if (customer.auslieferungErfolgt) {
                            val auslieferungErledigtAmStart = if (customer.auslieferungErledigtAm > 0) getStartOfDay(customer.auslieferungErledigtAm) else 0L
                            if (auslieferungErledigtAmStart > 0 && viewDateStart == auslieferungErledigtAmStart) {
                                true
                            } else {
                                termineAmTag.any { termin ->
                                    termin.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG
                                }
                            }
                        } else {
                            false
                        }
                        hatErledigtenATermin || hatErledigtenLTermin
                    } else {
                        false
                    }
                    
                    // Erledigte Kunden werden immer angezeigt (in ihrem Erledigt-Bereich innerhalb der Liste)
                    if (warUeberfaelligUndErledigtAmDatum || hatErledigtenTerminAmDatum) {
                        return@filter true
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
            
            if (warUeberfaelligUndErledigtAmDatum) {
                return@filter true
            }
            
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
            
            // Prüfe welche Termine am angezeigten Tag fällig sind
            val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365),
                tageVoraus = 730
            )
            
            val termineAmTag = termine.filter { categorizer.getStartOfDay(it.datum) == viewDateStart }
            val hatAbholungAmTag = termineAmTag.any { it.typ == com.example.we2026_5.TerminTyp.ABHOLUNG }
            val hatAuslieferungAmTag = termineAmTag.any { it.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG }
            
            // Prüfe ob Abholung überfällig ist
            val abholungUeberfaellig = if (hatAbholungAmTag) {
                val abholungTermin = termineAmTag.firstOrNull { it.typ == com.example.we2026_5.TerminTyp.ABHOLUNG }
                if (abholungTermin != null) {
                    val terminStart = categorizer.getStartOfDay(abholungTermin.datum)
                    val istUeberfaellig = terminStart < heuteStart
                    if (istUeberfaellig && !customer.abholungErfolgt) {
                        com.example.we2026_5.util.TerminFilterUtils.sollUeberfaelligAnzeigen(
                            terminDatum = abholungTermin.datum,
                            anzeigeDatum = viewDateStart,
                            aktuellesDatum = heuteStart
                        )
                    } else {
                        false
                    }
                } else {
                    false
                }
            } else {
                false
            }
            
            // Prüfe ob Auslieferung überfällig ist
            val auslieferungUeberfaellig = if (hatAuslieferungAmTag) {
                val auslieferungTermin = termineAmTag.firstOrNull { it.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG }
                if (auslieferungTermin != null) {
                    val terminStart = categorizer.getStartOfDay(auslieferungTermin.datum)
                    val istUeberfaellig = terminStart < heuteStart
                    if (istUeberfaellig && !customer.auslieferungErfolgt) {
                        com.example.we2026_5.util.TerminFilterUtils.sollUeberfaelligAnzeigen(
                            terminDatum = auslieferungTermin.datum,
                            anzeigeDatum = viewDateStart,
                            aktuellesDatum = heuteStart
                        )
                    } else {
                        false
                    }
                } else {
                    false
                }
            } else {
                false
            }
            
            val warUeberfaelligUndErledigt = if ((customer.abholungErfolgt || customer.auslieferungErfolgt) && customer.faelligAmDatum > 0) {
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
            
            val hatErledigtenTerminAmDatum = if (customer.abholungErfolgt || customer.auslieferungErfolgt) {
                val hatErledigtenATermin = if (customer.abholungErfolgt) {
                    val abholungErledigtAmStart = if (customer.abholungErledigtAm > 0) getStartOfDay(customer.abholungErledigtAm) else 0L
                    if (abholungErledigtAmStart > 0 && viewDateStart == abholungErledigtAmStart) {
                        true
                    } else {
                        termineAmTag.any { termin ->
                            termin.typ == com.example.we2026_5.TerminTyp.ABHOLUNG
                        }
                    }
                } else {
                    false
                }
                
                val hatErledigtenLTermin = if (customer.auslieferungErfolgt) {
                    val auslieferungErledigtAmStart = if (customer.auslieferungErledigtAm > 0) getStartOfDay(customer.auslieferungErledigtAm) else 0L
                    if (auslieferungErledigtAmStart > 0 && viewDateStart == auslieferungErledigtAmStart) {
                        true
                    } else {
                        termineAmTag.any { termin ->
                            termin.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG
                        }
                    }
                } else {
                    false
                }
                
                hatErledigtenATermin || hatErledigtenLTermin
            } else {
                false
            }
            
            // WICHTIG: Wenn Kunde sowohl überfälligen Abholtermin als auch normalen Auslieferungstermin hat,
            // füge ihn zu beiden Bereichen hinzu
            when {
                warUeberfaelligUndErledigt -> doneGewerblich.add(customer)
                hatErledigtenTerminAmDatum -> doneGewerblich.add(customer)
                abholungUeberfaellig && hatAuslieferungAmTag && !auslieferungUeberfaellig -> {
                    // Abholung überfällig, Auslieferung normal -> beide Bereiche
                    overdueGewerblich.add(customer)
                    normalGewerblich.add(customer)
                }
                abholungUeberfaellig -> overdueGewerblich.add(customer)
                auslieferungUeberfaellig -> overdueGewerblich.add(customer)
                hatAbholungAmTag || hatAuslieferungAmTag -> normalGewerblich.add(customer)
            }
        }
        
        // REIHENFOLGE: 1. Überfällig, 2. Listen, 3. Normal, 4. Erledigt
        
        // 1. Überfällige Kunden
        val overdueOhneListen = overdueGewerblich.sortedBy { it.name }
        if (overdueOhneListen.isNotEmpty()) {
            items.add(ListItem.SectionHeader("ÜBERFÄLLIG", overdueOhneListen.size, 0, SectionType.OVERDUE))
            if (expandedSections.contains(SectionType.OVERDUE)) {
                overdueOhneListen.forEach { items.add(ListItem.CustomerItem(it)) }
            }
        }
        
        // 2. Kunden nach Listen gruppiert
        allListen.sortedBy { it.name }.forEach { liste ->
            val kundenInListe = listenMitKunden[liste.id] ?: return@forEach
            if (kundenInListe.isNotEmpty()) {
                // Trenne Kunden in erledigt und nicht erledigt
                val nichtErledigteKunden = mutableListOf<Customer>()
                val erledigteKundenInListe = mutableListOf<Customer>()
                
                kundenInListe.forEach { customer ->
                    val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt
                    val listeDone = liste.abholungErfolgt || liste.auslieferungErfolgt
                    
                    // Prüfe ob Kunde am angezeigten Tag erledigt wurde oder erledigt sein soll
                    val sollAlsErledigtAnzeigen = if (isDone || listeDone) {
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
                        
                        val hatErledigtenTerminAmDatum = if (isDone) {
                            val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                                customer = customer,
                                liste = liste,
                                startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365),
                                tageVoraus = 730
                            )
                            
                            val hatErledigtenATermin = if (customer.abholungErfolgt) {
                                val abholungErledigtAmStart = if (customer.abholungErledigtAm > 0) categorizer.getStartOfDay(customer.abholungErledigtAm) else 0L
                                if (abholungErledigtAmStart > 0 && viewDateStart == abholungErledigtAmStart) {
                                    true
                                } else {
                                    termine.any { termin ->
                                        termin.typ == com.example.we2026_5.TerminTyp.ABHOLUNG &&
                                        categorizer.getStartOfDay(termin.datum) == viewDateStart
                                    }
                                }
                            } else {
                                false
                            }
                            
                            val hatErledigtenLTermin = if (customer.auslieferungErfolgt) {
                                val auslieferungErledigtAmStart = if (customer.auslieferungErledigtAm > 0) categorizer.getStartOfDay(customer.auslieferungErledigtAm) else 0L
                                if (auslieferungErledigtAmStart > 0 && viewDateStart == auslieferungErledigtAmStart) {
                                    true
                                } else {
                                    termine.any { termin ->
                                        termin.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG &&
                                        categorizer.getStartOfDay(termin.datum) == viewDateStart
                                    }
                                }
                            } else {
                                false
                            }
                            
                            hatErledigtenATermin || hatErledigtenLTermin
                        } else {
                            false
                        }
                        
                        warUeberfaelligUndErledigtAmDatum || hatErledigtenTerminAmDatum
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
                items.add(ListItem.ListeHeader(liste.name, gesamtKunden, erledigteKundenInListe.size, liste.id))
                
                // Zeige zuerst nicht erledigte Kunden (nur wenn vorhanden)
                if (nichtErledigteKunden.isNotEmpty()) {
                    nichtErledigteKunden.sortedBy { it.name }.forEach { items.add(ListItem.CustomerItem(it)) }
                }
                
                // Dann Erledigt-Bereich innerhalb der Liste (wenn es erledigte Kunden gibt)
                // WICHTIG: Erledigte Kunden aus Listen erscheinen NUR hier, nicht in anderen Bereichen
                if (erledigteKundenInListe.isNotEmpty()) {
                    // Erledigte Kunden innerhalb der Liste hinzufügen
                    // Sie werden im Adapter mit "ERLEDIGT" Label angezeigt
                    erledigteKundenInListe.sortedBy { it.name }.forEach { items.add(ListItem.CustomerItem(it)) }
                }
            }
        }
        
        // 3. Normale Kunden
        val normalOhneListen = normalGewerblich.sortedBy { it.name }
        normalOhneListen.forEach { items.add(ListItem.CustomerItem(it)) }
        
        // 4. Erledigt-Bereich
        val doneOhneListen = doneGewerblich.sortedBy { it.name }
        items.add(ListItem.SectionHeader("ERLEDIGT", doneOhneListen.size, doneOhneListen.size, SectionType.DONE))
        if (expandedSections.contains(SectionType.DONE)) {
            doneOhneListen.forEach { items.add(ListItem.CustomerItem(it)) }
        }
        
        return items
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
