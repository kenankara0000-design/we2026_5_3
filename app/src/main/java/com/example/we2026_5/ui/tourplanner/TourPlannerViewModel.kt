package com.example.we2026_5.ui.tourplanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.ListItem
import com.example.we2026_5.SectionType
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.tourplanner.TourDataProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class TourPlannerViewModel(
    private val repository: CustomerRepository,
    private val listeRepository: KundenListeRepository
) : ViewModel() {
    
    // Datenverarbeitungsprozessor
    private val dataProcessor = TourDataProcessor()
    
    // Echtzeit-Listener: StateFlows für automatische Updates (können .value verwendet werden)
    private val _customersStateFlow = MutableStateFlow<List<Customer>>(emptyList())
    private val customersFlow: StateFlow<List<Customer>> = _customersStateFlow
    
    private val _listenStateFlow = MutableStateFlow<List<KundenListe>>(emptyList())
    private val listenFlow: StateFlow<List<KundenListe>> = _listenStateFlow
    
    init {
        // Sammle Updates von Firebase-Flows und aktualisiere StateFlows
        viewModelScope.launch {
            repository.getAllCustomersFlow().collect { customers ->
                _customersStateFlow.value = customers
            }
        }
        viewModelScope.launch {
            listeRepository.getAllListenFlow().collect { listen ->
                _listenStateFlow.value = listen
            }
        }
    }
    
    // StateFlow für ausgewähltes Datum
    private val selectedTimestampFlow = MutableStateFlow<Long?>(null)
    
    // StateFlow für erweiterte Sections
    private val expandedSectionsFlow = MutableStateFlow<Set<SectionType>>(emptySet())
    
    // Kombiniere alle Flows für Tour-Items
    val tourItems: LiveData<List<ListItem>> = combine(
        customersFlow,
        listenFlow,
        selectedTimestampFlow,
        expandedSectionsFlow
    ) { customers, listen, timestamp, expandedSections ->
        if (timestamp == null) {
            emptyList()
        } else {
            dataProcessor.processTourData(customers, listen, timestamp, expandedSections)
        }
    }.asLiveData()
    
    private val _weekItems = MutableLiveData<Map<Int, List<ListItem>>>()
    val weekItems: LiveData<Map<Int, List<ListItem>>> = _weekItems
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // Sections standardmäßig eingeklappt (collapsed)
    private val expandedSections = mutableSetOf<SectionType>()
    
    fun loadTourData(selectedTimestamp: Long, isSectionExpanded: (SectionType) -> Boolean) {
        // Aktualisiere selectedTimestamp - Flow wird automatisch aktualisiert
        selectedTimestampFlow.value = selectedTimestamp
        // Aktualisiere expandedSections
        expandedSectionsFlow.value = expandedSections.toSet()
    }
    
    // processTourData Funktion entfernt - jetzt in TourDataProcessor
    
    fun toggleSection(sectionType: SectionType) {
        val current = expandedSections.toMutableSet()
        if (current.contains(sectionType)) {
            current.remove(sectionType)
        } else {
            current.add(sectionType)
        }
        expandedSections.clear()
        expandedSections.addAll(current)
        expandedSectionsFlow.value = current
    }
    
    fun isSectionExpanded(sectionType: SectionType): Boolean {
        return expandedSections.contains(sectionType)
    }
    
    // Alle Datenverarbeitungsfunktionen entfernt - jetzt in TourDataProcessor
    
    // istKundeUeberfaellig Funktion entfernt - jetzt in TourDataProcessor
    /*
    private fun istKundeUeberfaellig(
        customer: Customer,
        liste: KundenListe? = null,
        viewDateStart: Long,
        heuteStart: Long
    ): Boolean {
        // Prüfe Status: Kunde mit A oder L oder beiden
        val customerDone = customer.abholungErfolgt || customer.auslieferungErfolgt
        val listeDone = liste?.let { it.abholungErfolgt || it.auslieferungErfolgt } ?: false
        val isDone = customerDone || listeDone
        if (isDone) return false
        
        // NEUE STRUKTUR: Verwende Intervalle-Liste
        if (customer.intervalle.isNotEmpty() || (customer.listeId.isNotEmpty() && liste != null)) {
            // WICHTIG: Prüfe zuerst ob viewDateStart in der Zukunft liegt - dann keine überfälligen Termine anzeigen
            if (viewDateStart > heuteStart) {
                return false // Keine überfälligen Termine in der Zukunft anzeigen
            }
            
            val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                liste = liste,
                startDatum = heuteStart - java.util.concurrent.TimeUnit.DAYS.toMillis(365), // 365 Tage zurück
                tageVoraus = 730 // 365 zurück + 365 voraus
            )
            
            // Prüfe ob ein Termin überfällig ist und am angezeigten Datum sichtbar sein soll
            // WICHTIG: Ein Termin ist nur überfällig, wenn er in der Vergangenheit liegt UND nicht erledigt ist
            // UND am angezeigten Datum angezeigt werden soll
            // WICHTIG: Wenn der Termin genau am angezeigten Tag liegt (viewDateStart == terminStart), ist er NICHT überfällig
            return termine.any { termin ->
                val terminStart = getStartOfDay(termin.datum)
                val istUeberfaellig = terminStart < heuteStart // Termin liegt in der Vergangenheit
                // WICHTIG: Wenn der Termin genau am angezeigten Tag liegt, ist er NICHT überfällig (sondern normal fällig)
                if (terminStart == viewDateStart) return@any false
                val sollAnzeigen = com.example.we2026_5.util.TerminFilterUtils.sollUeberfaelligAnzeigen(
                    terminDatum = termin.datum,
                    anzeigeDatum = viewDateStart,
                    aktuellesDatum = heuteStart
                )
                istUeberfaellig && sollAnzeigen
            }
        }
        
        // ALTE STRUKTUR: Rückwärtskompatibilität
        val faelligAm = customerFaelligAm(customer, liste, viewDateStart)
        val abholungStart = getStartOfDay(customer.abholungDatum)
        val auslieferungStart = getStartOfDay(customer.auslieferungDatum)
        val abholungUeberfaellig = !customer.abholungErfolgt && customer.abholungDatum > 0 && 
                                   abholungStart < heuteStart
        val auslieferungUeberfaellig = !customer.auslieferungErfolgt && customer.auslieferungDatum > 0 && 
                                      auslieferungStart < heuteStart
        val wiederholendUeberfaellig = customer.wiederholen && faelligAm < heuteStart && faelligAm > 0
        
        // Überfällig nur anzeigen: Ab Termin-Datum bis zum aktuellen Datum (nicht in Zukunft)
        return (abholungUeberfaellig && viewDateStart >= abholungStart && viewDateStart <= heuteStart) ||
               (auslieferungUeberfaellig && viewDateStart >= auslieferungStart && viewDateStart <= heuteStart) ||
               (wiederholendUeberfaellig && viewDateStart >= faelligAm && viewDateStart <= heuteStart)
    }
    
    fun loadWeekData(weekStartTimestamp: Long, isSectionExpanded: (SectionType) -> Boolean) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                // Verwende die aktuellen Werte aus den Flows (Echtzeit-Updates)
                val allCustomers = customersFlow.value ?: emptyList()
                val allListen = listenFlow.value ?: emptyList()
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
                                startDatum = dayStart - java.util.concurrent.TimeUnit.DAYS.toMillis(365),
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
                
                _weekItems.value = weekData
            } catch (e: Exception) {
                _error.value = e.message ?: "Fehler beim Laden der Wochenansicht"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // getStartOfDay Funktion entfernt - jetzt in TourDataProcessor
    // isIntervallFaelligAm Funktion entfernt - jetzt in TourDataProcessor
    // getNaechstesListeDatum Funktion entfernt - jetzt in TourDataProcessor
    /*
    private fun getStartOfDay(ts: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    private fun isIntervallFaelligAm(intervall: ListeIntervall, datum: Long): Boolean {
        val datumStart = getStartOfDay(datum)
        val abholungStart = getStartOfDay(intervall.abholungDatum)
        val auslieferungStart = getStartOfDay(intervall.auslieferungDatum)
        
        if (!intervall.wiederholen) {
            // Einmaliges Intervall: Prüfe ob Datum genau Abholungs- oder Auslieferungsdatum ist
            return datumStart == abholungStart || datumStart == auslieferungStart
        }
        
        // Wiederholendes Intervall: Prüfe ob Datum auf einem Wiederholungszyklus liegt
        val intervallTage = intervall.intervallTage.coerceIn(1, 365)
        
        // Prüfe Abholungsdatum - generiere Termine für 365 Tage
        if (datumStart >= abholungStart) {
            val tageSeitAbholung = TimeUnit.MILLISECONDS.toDays(datumStart - abholungStart)
            // Prüfe ob das Datum auf einem Zyklus liegt (innerhalb von 365 Tagen)
            if (tageSeitAbholung <= 365 && tageSeitAbholung % intervallTage == 0L) {
                val erwartetesDatum = abholungStart + TimeUnit.DAYS.toMillis(tageSeitAbholung)
                if (datumStart == erwartetesDatum) {
                    return true
                }
            }
        } else {
            // Datum liegt vor dem Startdatum - prüfe ob es ein zukünftiger Termin ist (innerhalb von 365 Tagen)
            val tageBisAbholung = TimeUnit.MILLISECONDS.toDays(abholungStart - datumStart)
            if (tageBisAbholung <= 365 && datumStart == abholungStart) {
                return true
            }
        }
        
        // Prüfe Auslieferungsdatum - generiere Termine für 365 Tage
        if (datumStart >= auslieferungStart) {
            val tageSeitAuslieferung = TimeUnit.MILLISECONDS.toDays(datumStart - auslieferungStart)
            // Prüfe ob das Datum auf einem Zyklus liegt (innerhalb von 365 Tagen)
            if (tageSeitAuslieferung <= 365 && tageSeitAuslieferung % intervallTage == 0L) {
                val erwartetesDatum = auslieferungStart + TimeUnit.DAYS.toMillis(tageSeitAuslieferung)
                if (datumStart == erwartetesDatum) {
                    return true
                }
            }
        } else {
            // Datum liegt vor dem Startdatum - prüfe ob es ein zukünftiger Termin ist (innerhalb von 365 Tagen)
            val tageBisAuslieferung = TimeUnit.MILLISECONDS.toDays(auslieferungStart - datumStart)
            if (tageBisAuslieferung <= 365 && datumStart == auslieferungStart) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Berechnet das nächste fällige Datum für einen Kunden in einer Liste
     */
    private fun getNaechstesListeDatum(liste: KundenListe, abDatum: Long = System.currentTimeMillis(), geloeschteTermine: List<Long> = emptyList()): Long? {
        val abDatumStart = getStartOfDay(abDatum)
        var naechstesDatum: Long? = null
        
        // Durch alle Intervalle iterieren und das nächste Datum finden (überspringe gelöschte Termine)
        liste.intervalle.forEach { intervall ->
            if (!intervall.wiederholen) {
                // Einmaliges Intervall
                val abholungStart = getStartOfDay(intervall.abholungDatum)
                val auslieferungStart = getStartOfDay(intervall.auslieferungDatum)
                
                if (abholungStart >= abDatumStart && !geloeschteTermine.contains(abholungStart) && (naechstesDatum == null || abholungStart < naechstesDatum!!)) {
                    naechstesDatum = abholungStart
                }
                if (auslieferungStart >= abDatumStart && !geloeschteTermine.contains(auslieferungStart) && (naechstesDatum == null || auslieferungStart < naechstesDatum!!)) {
                    naechstesDatum = auslieferungStart
                }
            } else {
                // Wiederholendes Intervall - überspringe gelöschte Termine
                val intervallTage = intervall.intervallTage.coerceIn(1, 365)
                val abholungStart = getStartOfDay(intervall.abholungDatum)
                val auslieferungStart = getStartOfDay(intervall.auslieferungDatum)
                
                // Nächstes Abholungsdatum (überspringe gelöschte)
                var naechsteAbholung: Long? = null
                if (abDatumStart >= abholungStart) {
                    val tageSeitAbholung = TimeUnit.MILLISECONDS.toDays(abDatumStart - abholungStart)
                    // Prüfe zuerst, ob abDatumStart genau auf einem Zyklus liegt
                    val zyklusAktuell = tageSeitAbholung / intervallTage
                    val aktuellesDatum = abholungStart + TimeUnit.DAYS.toMillis(zyklusAktuell * intervallTage)
                    val aktuellesDatumStart = getStartOfDay(aktuellesDatum)
                    
                    if (aktuellesDatumStart == abDatumStart && !geloeschteTermine.contains(aktuellesDatumStart)) {
                        naechsteAbholung = aktuellesDatumStart
                    } else {
                        // Suche nächsten Zyklus
                        var zyklus = (tageSeitAbholung / intervallTage + 1).toInt()
                        var versuche = 0
                        while (versuche < 100) { // Max 100 Versuche
                            val kandidat = abholungStart + TimeUnit.DAYS.toMillis((zyklus * intervallTage).toLong())
                            val kandidatStart = getStartOfDay(kandidat)
                            if (kandidatStart >= abDatumStart && !geloeschteTermine.contains(kandidatStart)) {
                                naechsteAbholung = kandidatStart
                                break
                            }
                            zyklus++
                            versuche++
                        }
                    }
                } else {
                    if (!geloeschteTermine.contains(abholungStart)) {
                        naechsteAbholung = abholungStart
                    } else {
                        var zyklus = 1
                        var versuche = 0
                        while (versuche < 100) {
                            val kandidat = abholungStart + TimeUnit.DAYS.toMillis((zyklus * intervallTage).toLong())
                            val kandidatStart = getStartOfDay(kandidat)
                            if (kandidatStart >= abDatumStart && !geloeschteTermine.contains(kandidatStart)) {
                                naechsteAbholung = kandidatStart
                                break
                            }
                            zyklus++
                            versuche++
                        }
                    }
                }
                
                if (naechsteAbholung != null && (naechstesDatum == null || naechsteAbholung < naechstesDatum!!)) {
                    naechstesDatum = naechsteAbholung
                }
                
                // Nächstes Auslieferungsdatum (überspringe gelöschte)
                var naechsteAuslieferung: Long? = null
                if (abDatumStart >= auslieferungStart) {
                    val tageSeitAuslieferung = TimeUnit.MILLISECONDS.toDays(abDatumStart - auslieferungStart)
                    // Prüfe zuerst, ob abDatumStart genau auf einem Zyklus liegt
                    val zyklusAktuell = tageSeitAuslieferung / intervallTage
                    val aktuellesDatum = auslieferungStart + TimeUnit.DAYS.toMillis(zyklusAktuell * intervallTage)
                    val aktuellesDatumStart = getStartOfDay(aktuellesDatum)
                    
                    if (aktuellesDatumStart == abDatumStart && !geloeschteTermine.contains(aktuellesDatumStart)) {
                        naechsteAuslieferung = aktuellesDatumStart
                    } else {
                        // Suche nächsten Zyklus
                        var zyklus = (tageSeitAuslieferung / intervallTage + 1).toInt()
                        var versuche = 0
                        while (versuche < 100) {
                            val kandidat = auslieferungStart + TimeUnit.DAYS.toMillis((zyklus * intervallTage).toLong())
                            val kandidatStart = getStartOfDay(kandidat)
                            if (kandidatStart >= abDatumStart && !geloeschteTermine.contains(kandidatStart)) {
                                naechsteAuslieferung = kandidatStart
                                break
                            }
                            zyklus++
                            versuche++
                        }
                    }
                } else {
                    if (!geloeschteTermine.contains(auslieferungStart)) {
                        naechsteAuslieferung = auslieferungStart
                    } else {
                        var zyklus = 1
                        var versuche = 0
                        while (versuche < 100) {
                            val kandidat = auslieferungStart + TimeUnit.DAYS.toMillis((zyklus * intervallTage).toLong())
                            val kandidatStart = getStartOfDay(kandidat)
                            if (kandidatStart >= abDatumStart && !geloeschteTermine.contains(kandidatStart)) {
                                naechsteAuslieferung = kandidatStart
                                break
                            }
                            zyklus++
                            versuche++
                        }
                    }
                }
                
                if (naechsteAuslieferung != null && (naechstesDatum == null || naechsteAuslieferung < naechstesDatum!!)) {
                    naechstesDatum = naechsteAuslieferung
                }
            }
        }
        
        return naechstesDatum
    }
    */
    
}
