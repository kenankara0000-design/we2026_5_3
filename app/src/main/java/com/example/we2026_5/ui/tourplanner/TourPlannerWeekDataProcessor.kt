package com.example.we2026_5.ui.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListItem
import com.example.we2026_5.SectionType
import com.example.we2026_5.tourplanner.TourDataProcessor
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Helper-Klasse für Wochenansicht-Datenverarbeitung in TourPlannerViewModel
 */
class TourPlannerWeekDataProcessor(
    private val dataProcessor: TourDataProcessor
) {
    
    /**
     * Verarbeitet Daten für die Wochenansicht
     */
    fun processWeekData(
        allCustomers: List<Customer>,
        allListen: List<KundenListe>,
        weekStartTimestamp: Long,
        isSectionExpanded: (SectionType) -> Boolean
    ): Map<Int, List<ListItem>> {
        val heuteStart = dataProcessor.getStartOfDay(System.currentTimeMillis())
        
        // Woche startet am Montag
        val weekStart = Calendar.getInstance().apply {
            timeInMillis = weekStartTimestamp
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val weekData = mutableMapOf<Int, MutableList<ListItem>>()
        
        // Für jeden Tag der Woche (0=Montag bis 6=Sonntag)
        for (dayOffset in 0..6) {
            val dayTimestamp = weekStart + TimeUnit.DAYS.toMillis(dayOffset.toLong())
            val dayStart = dataProcessor.getStartOfDay(dayTimestamp)
            
            val dayItems = mutableListOf<ListItem>()
            
            // Alle Kunden nach Listen gruppieren (sowohl Privat als auch Gewerblich)
            val kundenNachListen = allCustomers.filter { it.listeId.isNotEmpty() }.groupBy { it.listeId }
            val kundenOhneListe = allCustomers.filter { it.listeId.isEmpty() }
            val listenMitKunden = mutableMapOf<String, List<Customer>>()
            
            kundenNachListen.forEach { (listeId, kunden) ->
                if (listeId.isEmpty()) return@forEach
                val liste = allListen.find { it.id == listeId } ?: return@forEach
                
                // Prüfe ob mindestens ein Intervall an diesem Tag oder innerhalb von 365 Tagen fällig ist
                val istFaellig = liste.intervalle.any { intervall ->
                    dataProcessor.isIntervallFaelligAm(intervall, dayStart) || 
                    dataProcessor.isIntervallFaelligInZukunft(intervall, dayStart)
                }
                
                if (istFaellig) {
                    val fälligeKunden = kunden.filter { customer ->
                        val faelligAm = dataProcessor.customerFaelligAm(customer, liste, dayStart)
                        val faelligAmImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                               faelligAm in customer.urlaubVon..customer.urlaubBis
                        if (faelligAmImUrlaub) return@filter false
                        
                        val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                        val isOverdue = dataProcessor.istKundeUeberfaellig(customer, liste, dayStart, heuteStart)
                        
                        // Überfällige Termine: Zeige sie nur vom Termin-Tag bis zum aktuellen Datum (nicht in Zukunft)
                        if (isOverdue) {
                            // Helper-Funktion prüft bereits ob angezeigt werden soll
                            return@filter true
                        }
                        
                        // Normale Termine: Nur anzeigen, wenn genau an diesem Tag ein Termin ist
                        dataProcessor.hatKundeTerminAmDatum(customer, liste, dayStart)
                    }
                    
                    if (fälligeKunden.isNotEmpty()) {
                        listenMitKunden[listeId] = fälligeKunden.sortedBy { it.name }
                    }
                }
            }
            
            // Sammle alle Kunden-IDs die in Listen sind (um Doppelungen zu vermeiden)
            val kundenInListenIds = listenMitKunden.values.flatten().map { it.id }.toSet()
            
            // Kunden nach Listen gruppiert anzeigen (sowohl Privat als auch Gewerblich)
            allListen.sortedBy { it.name }.forEach { liste ->
                val kundenInListe = listenMitKunden[liste.id] ?: return@forEach
                if (kundenInListe.isNotEmpty()) {
                    // Zähle erledigte Kunden (sowohl Kunde als auch Liste Status prüfen)
                    val erledigteKunden = kundenInListe.count { customer ->
                        val customerDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                        val listeDone = liste.abholungErfolgt && liste.auslieferungErfolgt
                        customerDone || listeDone
                    }
                    dayItems.add(ListItem.ListeHeader(liste.name, kundenInListe.size, erledigteKunden, liste.id))
                    kundenInListe.forEach { dayItems.add(ListItem.CustomerItem(it)) }
                }
            }
            
            // Gewerblich-Kunden ohne Liste filtern (Kunden aus Listen sind bereits oben angezeigt)
            val gewerblichKundenOhneListe = kundenOhneListe.filter { it.kundenArt == "Gewerblich" && it.id !in kundenInListenIds }
            val filteredGewerblich = gewerblichKundenOhneListe.filter { customer ->
                // Wochentag-Filterung entfernt - wird nicht mehr verwendet
                if (!customer.wiederholen) {
                    val abholungAm = dataProcessor.getStartOfDay(customer.abholungDatum)
                    val auslieferungAm = dataProcessor.getStartOfDay(customer.auslieferungDatum)
                    if (abholungAm != dayStart && auslieferungAm != dayStart) return@filter false
                }
                
                val faelligAm = dataProcessor.customerFaelligAm(customer, null, dayStart)
                val faelligAmImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                       faelligAm in customer.urlaubVon..customer.urlaubBis
                if (faelligAmImUrlaub) return@filter false
                
                val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt // Mit A oder L oder beiden
                val isOverdue = dataProcessor.istKundeUeberfaellig(customer, null, dayStart, heuteStart)
                
                // Überfällige Termine: Zeige sie nur vom Termin-Tag bis zum aktuellen Datum (nicht in Zukunft)
                if (isOverdue) {
                    // Helper-Funktion prüft bereits ob angezeigt werden soll
                    return@filter true
                }
                
                // Normale Termine: Nur anzeigen, wenn genau an diesem Tag ein Termin ist
                dataProcessor.hatKundeTerminAmDatum(customer, null, dayStart)
            }
            
            val overdueGewerblich = mutableListOf<Customer>()
            val normalGewerblich = mutableListOf<Customer>()
            val doneGewerblich = mutableListOf<Customer>()
            
            filteredGewerblich.forEach { customer ->
                val isOverdue = dataProcessor.istKundeUeberfaellig(customer, null, dayStart, heuteStart)
                
                // Prüfe ob überfälliger Kunde erledigt wurde und am Erledigungstag angezeigt werden soll
                val warUeberfaelligUndErledigt = if ((customer.abholungErfolgt || customer.auslieferungErfolgt) && customer.faelligAmDatum > 0) {
                    val faelligAmStart = dataProcessor.getStartOfDay(customer.faelligAmDatum)
                    val erledigtAmStart = if (customer.abholungErledigtAm > 0) {
                        dataProcessor.getStartOfDay(customer.abholungErledigtAm)
                    } else if (customer.auslieferungErledigtAm > 0) {
                        dataProcessor.getStartOfDay(customer.auslieferungErledigtAm)
                    } else {
                        0L
                    }
                    // Zeige im Erledigt-Bereich, wenn angezeigtes Datum = Fälligkeitstag ODER Erledigungstag
                    dayStart == faelligAmStart || (erledigtAmStart > 0 && dayStart == erledigtAmStart)
                } else {
                    false
                }
                
                // WICHTIG: Prüfe ob am angezeigten Tag tatsächlich ein erledigter Termin fällig war
                val hatErledigtenTerminAmDatum = if (customer.abholungErfolgt || customer.auslieferungErfolgt) {
                    val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                        customer = customer,
                        startDatum = dayStart - TimeUnit.DAYS.toMillis(365),
                        tageVoraus = 730
                    )
                    
                    // Prüfe ob A erledigt ist und am angezeigten Tag A-Termin fällig war oder A erledigt wurde
                    val hatErledigtenATermin = if (customer.abholungErfolgt) {
                        val abholungErledigtAmStart = if (customer.abholungErledigtAm > 0) dataProcessor.getStartOfDay(customer.abholungErledigtAm) else 0L
                        if (abholungErledigtAmStart > 0 && dayStart == abholungErledigtAmStart) {
                            true
                        } else {
                            termine.any { termin ->
                                termin.typ == com.example.we2026_5.TerminTyp.ABHOLUNG &&
                                dataProcessor.getStartOfDay(termin.datum) == dayStart
                            }
                        }
                    } else {
                        false
                    }
                    
                    // Prüfe ob L erledigt ist und am angezeigten Tag L-Termin fällig war oder L erledigt wurde
                    val hatErledigtenLTermin = if (customer.auslieferungErfolgt) {
                        val auslieferungErledigtAmStart = if (customer.auslieferungErledigtAm > 0) dataProcessor.getStartOfDay(customer.auslieferungErledigtAm) else 0L
                        if (auslieferungErledigtAmStart > 0 && dayStart == auslieferungErledigtAmStart) {
                            true
                        } else {
                            termine.any { termin ->
                                termin.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG &&
                                dataProcessor.getStartOfDay(termin.datum) == dayStart
                            }
                        }
                    } else {
                        false
                    }
                    
                    hatErledigtenATermin || hatErledigtenLTermin
                } else {
                    false
                }
                
                when {
                    warUeberfaelligUndErledigt -> doneGewerblich.add(customer) // Überfällige erledigte Kunden auch im Erledigt-Bereich
                    hatErledigtenTerminAmDatum -> doneGewerblich.add(customer) // Normale erledigte Kunden, aber nur wenn am angezeigten Tag ein erledigter Termin fällig war
                    isOverdue -> overdueGewerblich.add(customer)
                    else -> normalGewerblich.add(customer)
                }
            }
            
            overdueGewerblich.sortBy { it.name }
            normalGewerblich.sortBy { it.name }
            doneGewerblich.sortBy { it.name }
            
            // REIHENFOLGE: 1. Überfällig, 2. Listen, 3. Normal, 4. Erledigt
            
            // 1. Überfällige Kunden (ganz oben)
            if (overdueGewerblich.isNotEmpty()) {
                // Überfällige Kunden sind per Definition nicht erledigt (0 erledigt)
                dayItems.add(ListItem.SectionHeader("ÜBERFÄLLIG", overdueGewerblich.size, 0, SectionType.OVERDUE))
                if (isSectionExpanded(SectionType.OVERDUE)) {
                    overdueGewerblich.forEach { dayItems.add(ListItem.CustomerItem(it)) }
                }
            }
            
            // 2. Listen sind bereits oben hinzugefügt
            
            // 3. Normale Kunden
            normalGewerblich.forEach { dayItems.add(ListItem.CustomerItem(it)) }
            
            // 4. Erledigt-Bereich (ganz unten) - IMMER sichtbar
            // Erledigte Kunden sind alle erledigt (alle erledigt)
            dayItems.add(ListItem.SectionHeader("ERLEDIGT", doneGewerblich.size, doneGewerblich.size, SectionType.DONE))
            if (isSectionExpanded(SectionType.DONE)) {
                doneGewerblich.forEach { dayItems.add(ListItem.CustomerItem(it)) }
            }
            
            weekData[dayOffset] = dayItems
        }
        
        return weekData
    }
}
