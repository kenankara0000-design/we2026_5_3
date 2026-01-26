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
            processTourData(customers, listen, timestamp, expandedSections)
        }
    }.asLiveData()
    
    private val _weekItems = MutableLiveData<Map<Int, List<ListItem>>>()
    val weekItems: LiveData<Map<Int, List<ListItem>>> = _weekItems
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // Sections standardmäßig expanded, damit sie sichtbar sind
    private val expandedSections = mutableSetOf<SectionType>(SectionType.OVERDUE, SectionType.DONE)
    
    fun loadTourData(selectedTimestamp: Long, isSectionExpanded: (SectionType) -> Boolean) {
        // Aktualisiere selectedTimestamp - Flow wird automatisch aktualisiert
        selectedTimestampFlow.value = selectedTimestamp
        // Aktualisiere expandedSections
        expandedSectionsFlow.value = expandedSections.toSet()
    }
    
    // Interne Funktion zur Verarbeitung der Tour-Daten
    private fun processTourData(
        allCustomers: List<Customer>,
        allListen: List<KundenListe>,
        selectedTimestamp: Long,
        expandedSections: Set<SectionType>
    ): List<ListItem> {
        val viewDateStart = getStartOfDay(selectedTimestamp)
        val heuteStart = getStartOfDay(System.currentTimeMillis())
                
                // Alle Kunden nach Listen gruppieren (sowohl Privat als auch Gewerblich)
                val kundenNachListen = allCustomers.filter { it.listeId.isNotEmpty() }.groupBy { it.listeId }
                val kundenOhneListe = allCustomers.filter { it.listeId.isEmpty() }
                
                // Kunden in Listen filtern: Prüfe ob Liste an diesem Tag fällig ist
                val listenMitKunden = mutableMapOf<String, List<Customer>>()
                kundenNachListen.forEach { (listeId, kunden) ->
                    if (listeId.isEmpty()) return@forEach
                    val liste = allListen.find { it.id == listeId } ?: return@forEach
                    
                    // Prüfe ob mindestens ein Intervall an diesem Datum oder innerhalb von 365 Tagen fällig ist
                    val istFaellig = liste.intervalle.any { intervall ->
                        isIntervallFaelligAm(intervall, viewDateStart) || 
                        isIntervallFaelligInZukunft(intervall, viewDateStart)
                    }
                    
                    if (istFaellig) {
                        // Filtere Kunden die an diesem Tag einen Termin haben
                        val fälligeKunden = kunden.filter { customer ->
                            val faelligAm = customerFaelligAm(customer, liste, viewDateStart)
                            // Prüfe Urlaub: Kunde UND Liste (wenn vorhanden)
                            val customerImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                                   faelligAm in customer.urlaubVon..customer.urlaubBis
                            val listeImUrlaub = liste?.let { 
                                it.urlaubVon > 0 && it.urlaubBis > 0 && faelligAm in it.urlaubVon..it.urlaubBis 
                            } ?: false
                            if (customerImUrlaub || listeImUrlaub) return@filter false
                            
                            val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                            val isOverdue = istKundeUeberfaellig(customer, liste, viewDateStart, heuteStart)
                            
                            // Überfällige Termine: Zeige sie nur vom Termin-Tag bis zum aktuellen Datum (nicht in Zukunft)
                            if (isOverdue) {
                                // Helper-Funktion prüft bereits ob angezeigt werden soll
                                return@filter true
                            }
                            
                            // Normale Termine: Nur anzeigen, wenn genau an diesem Tag ein Termin ist
                            hatKundeTerminAmDatum(customer, liste, viewDateStart)
                        }
                        
                        if (fälligeKunden.isNotEmpty()) {
                            listenMitKunden[listeId] = fälligeKunden.sortedBy { it.name }
                        }
                    }
                }
                
                // Gewerblich-Kunden ohne Liste filtern
                val gewerblichKundenOhneListe = kundenOhneListe.filter { it.kundenArt == "Gewerblich" }
                val filteredGewerblich = gewerblichKundenOhneListe.filter { customer ->
                    val faelligAm = customerFaelligAm(customer, null, viewDateStart)
                    val faelligAmImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                           faelligAm in customer.urlaubVon..customer.urlaubBis
                    if (faelligAmImUrlaub) return@filter false
                    
                    val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                    val isOverdue = istKundeUeberfaellig(customer, null, viewDateStart, heuteStart)
                    
                    // Überfällige Termine: Zeige sie nur vom Termin-Tag bis zum aktuellen Datum (nicht in Zukunft)
                    if (isOverdue) {
                        // Helper-Funktion prüft bereits ob angezeigt werden soll
                        return@filter true
                    }
                    
                    // Normale Termine: Nur anzeigen, wenn genau an diesem Tag ein Termin ist
                    hatKundeTerminAmDatum(customer, null, viewDateStart)
                }
                
                // Liste mit Items erstellen
                val items = mutableListOf<ListItem>()
                
                // Listen-Kunden in Sections kategorisieren
                val overdueListenKunden = mutableListOf<Customer>()
                val normalListenKunden = mutableListOf<Customer>()
                val doneListenKunden = mutableListOf<Customer>()
                
                // Kunden nach Listen gruppiert kategorisieren (sowohl Privat als auch Gewerblich)
                allListen.sortedBy { it.name }.forEach { liste ->
                    val kundenInListe = listenMitKunden[liste.id] ?: return@forEach
                    if (kundenInListe.isNotEmpty()) {
                        // Kunden in Sections kategorisieren
                        kundenInListe.forEach { customer ->
                            val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                            val isOverdue = istKundeUeberfaellig(customer, liste, viewDateStart, heuteStart)
                            
                            when {
                                isDone -> doneListenKunden.add(customer)
                                isOverdue -> overdueListenKunden.add(customer)
                                else -> normalListenKunden.add(customer)
                            }
                        }
                    }
                }
                
                // Gewerblich-Kunden (alte Logik)
                val overdueGewerblich = mutableListOf<Customer>()
                val normalGewerblich = mutableListOf<Customer>()
                val doneGewerblich = mutableListOf<Customer>()
                
                filteredGewerblich.forEach { customer ->
                    val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                    val isOverdue = istKundeUeberfaellig(customer, null, viewDateStart, heuteStart)
                    
                    when {
                        isDone -> doneGewerblich.add(customer)
                        isOverdue -> overdueGewerblich.add(customer)
                        else -> normalGewerblich.add(customer)
                    }
                }
                
                // Alle Kunden (Listen + Gewerblich) in Sections zusammenführen
                val allOverdue = (overdueListenKunden + overdueGewerblich).sortedBy { it.name }
                val allNormal = (normalListenKunden + normalGewerblich).sortedBy { it.name }
                val allDone = (doneListenKunden + doneGewerblich).sortedBy { it.name }
                
                // Kunden nach Listen gruppiert anzeigen (sowohl Privat als auch Gewerblich)
                allListen.sortedBy { it.name }.forEach { liste ->
                    val kundenInListe = listenMitKunden[liste.id] ?: return@forEach
                    if (kundenInListe.isNotEmpty()) {
                        items.add(ListItem.ListeHeader(liste.name, kundenInListe.size, liste.id))
                        // Kunden werden nur angezeigt wenn Liste expanded ist (wird im Adapter gehandhabt)
                        kundenInListe.forEach { items.add(ListItem.CustomerItem(it)) }
                    }
                }
        
        // Sections für alle Kunden (Listen + Gewerblich)
        if (allOverdue.isNotEmpty()) {
            items.add(ListItem.SectionHeader("ÜBERFÄLLIG", allOverdue.size, SectionType.OVERDUE))
            if (expandedSections.contains(SectionType.OVERDUE)) {
                allOverdue.forEach { items.add(ListItem.CustomerItem(it)) }
            }
        }
        
        allNormal.forEach { items.add(ListItem.CustomerItem(it)) }
        
        if (allDone.isNotEmpty()) {
            items.add(ListItem.SectionHeader("ERLEDIGT", allDone.size, SectionType.DONE))
            if (expandedSections.contains(SectionType.DONE)) {
                allDone.forEach { items.add(ListItem.CustomerItem(it)) }
            }
        }
        
        return items
    }
    
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
    
    private fun customerFaelligAm(c: Customer, liste: KundenListe? = null, abDatum: Long = System.currentTimeMillis()): Long {
        // NEUE STRUKTUR: Verwende Intervalle-Liste wenn vorhanden
        if (c.intervalle.isNotEmpty()) {
            val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = c,
                liste = liste,
                startDatum = abDatum,
                tageVoraus = 365
            )
            // Nächstes fälliges Datum ab dem angezeigten Datum
            val naechstesTermin = termine.firstOrNull { 
                it.datum >= getStartOfDay(abDatum)
            }
            return naechstesTermin?.datum ?: 0L
        }
        
        // Für Kunden in Listen: Daten der Liste verwenden
        if (c.listeId.isNotEmpty() && liste != null) {
            // Wenn verschoben, verschobenes Datum verwenden
            if (c.verschobenAufDatum > 0) {
                val verschobenStart = getStartOfDay(c.verschobenAufDatum)
                // Prüfe ob verschobenes Datum gelöscht wurde
                if (c.geloeschteTermine.contains(verschobenStart)) {
                    // Wenn verschobenes Datum gelöscht wurde, berechne nächstes Datum
                    val naechstesDatum = getNaechstesListeDatum(liste, verschobenStart + TimeUnit.DAYS.toMillis(1))
                    return naechstesDatum ?: abDatum
                }
                return c.verschobenAufDatum
            }
            
            // Nächstes Datum aus den Intervallen der Liste berechnen (ab dem angezeigten Datum)
            val naechstesDatum = getNaechstesListeDatum(liste, abDatum, c.geloeschteTermine)
            return naechstesDatum ?: 0L  // Nicht abDatum zurückgeben, wenn kein Termin gefunden wird
        }
        
        // Für Kunden ohne Liste: Normale Logik
        if (!c.wiederholen) {
            // Einmaliger Termin: Berücksichtige sowohl Abholungs- als auch Auslieferungsdatum
            val abholungStart = getStartOfDay(c.abholungDatum)
            val auslieferungStart = getStartOfDay(c.auslieferungDatum)
            val abDatumStart = getStartOfDay(abDatum)
            
            // Prüfe ob verschoben
            if (c.verschobenAufDatum > 0) {
                val verschobenStart = getStartOfDay(c.verschobenAufDatum)
                if (c.geloeschteTermine.contains(verschobenStart)) {
                    // Verschobenes Datum wurde gelöscht - keine weiteren Termine
                    return 0L
                }
                // Wenn abDatum auf verschobenem Datum liegt, gib es zurück
                if (abDatumStart == verschobenStart) return c.verschobenAufDatum
                // Wenn abDatum vor verschobenem Datum liegt, gib verschobenes Datum zurück
                if (abDatumStart < verschobenStart) return c.verschobenAufDatum
                // Wenn abDatum nach verschobenem Datum liegt, keine weiteren Termine
                return 0L
            }
            
            // Prüfe ob Termine gelöscht wurden
            val abholungGeloescht = c.geloeschteTermine.contains(abholungStart)
            val auslieferungGeloescht = c.geloeschteTermine.contains(auslieferungStart)
            
            // Wenn beide Termine gelöscht wurden, keine weiteren Termine
            if (abholungGeloescht && auslieferungGeloescht) return 0L
            
            // Wenn abDatum genau auf Abholungstag liegt und nicht gelöscht
            if (abDatumStart == abholungStart && !abholungGeloescht) {
                return c.abholungDatum
            }
            
            // Wenn abDatum genau auf Auslieferungstag liegt und nicht gelöscht
            if (abDatumStart == auslieferungStart && !auslieferungGeloescht) {
                return c.auslieferungDatum
            }
            
            // Wenn abDatum zwischen beiden liegt: Gib das nächste fällige Datum zurück
            if (abDatumStart > abholungStart && abDatumStart < auslieferungStart) {
                // Zwischen Abholung und Auslieferung: Gib Auslieferungsdatum zurück (nächstes fälliges)
                return if (!auslieferungGeloescht) c.auslieferungDatum else 0L
            }
            if (abDatumStart > auslieferungStart && abDatumStart < abholungStart) {
                // Zwischen Auslieferung und Abholung: Gib Abholungsdatum zurück (nächstes fälliges)
                return if (!abholungGeloescht) c.abholungDatum else 0L
            }
            
            // Wenn abDatum vor beiden liegt: Gib Abholungsdatum zurück (nächstes fälliges)
            if (abDatumStart < abholungStart && abDatumStart < auslieferungStart) {
                return if (!abholungGeloescht) c.abholungDatum else 
                       if (!auslieferungGeloescht) c.auslieferungDatum else 0L
            }
            
            // Wenn abDatum nach beiden liegt: Keine weiteren Termine
            if (abDatumStart > abholungStart && abDatumStart > auslieferungStart) {
                return 0L
            }
            
            // Fallback: Gib das nächste fällige Datum zurück
            if (!abholungGeloescht && !auslieferungGeloescht) {
                return minOf(c.abholungDatum, c.auslieferungDatum)
            }
            if (!abholungGeloescht) return c.abholungDatum
            if (!auslieferungGeloescht) return c.auslieferungDatum
            return 0L
        }
        
        // Wiederholender Termin: Alte Logik
        val faelligAm = c.getFaelligAm()
        val faelligAmStart = getStartOfDay(faelligAm)
        // Prüfe ob Termin gelöscht wurde
        if (c.geloeschteTermine.contains(faelligAmStart)) {
            // Wenn Termin gelöscht wurde, berechne nächstes Datum
            if (c.wiederholen && c.letzterTermin > 0) {
                return c.letzterTermin + TimeUnit.DAYS.toMillis(c.intervallTage.toLong())
            }
            return faelligAm + TimeUnit.DAYS.toMillis(1) // Fallback: nächster Tag
        }
        return faelligAm
    }
    
    /**
     * Prüft ob ein Kunde an einem bestimmten Datum einen Termin hat (genau an diesem Tag)
     * Unterstützt sowohl neue (intervalle) als auch alte Struktur
     */
    private fun hatKundeTerminAmDatum(
        customer: Customer,
        liste: KundenListe? = null,
        viewDateStart: Long
    ): Boolean {
        // NEUE STRUKTUR: Verwende Intervalle-Liste
        if (customer.intervalle.isNotEmpty() || (customer.listeId.isNotEmpty() && liste != null)) {
            val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                liste = liste,
                startDatum = viewDateStart - java.util.concurrent.TimeUnit.DAYS.toMillis(1),
                tageVoraus = 2 // Nur 2 Tage (gestern, heute, morgen) für Performance
            )
            // Prüfe ob ein Termin genau am viewDateStart liegt
            // Kombiniere gelöschte Termine von Kunde UND Liste
            val alleGeloeschteTermine = if (liste != null) {
                (customer.geloeschteTermine + liste.geloeschteTermine).distinct()
            } else {
                customer.geloeschteTermine
            }
            return termine.any { termin ->
                val terminStart = com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(termin.datum)
                terminStart == viewDateStart &&
                !com.example.we2026_5.util.TerminBerechnungUtils.istTerminGeloescht(termin.datum, alleGeloeschteTermine)
            }
        }
        
        // ALTE STRUKTUR: Rückwärtskompatibilität
        val faelligAm = customerFaelligAm(customer, liste, viewDateStart)
        val faelligAmStart = getStartOfDay(faelligAm)
        return faelligAmStart == viewDateStart && faelligAm > 0
    }
    
    /**
     * Prüft ob ein Kunde überfällig ist und ob er an einem bestimmten Datum angezeigt werden soll
     * Unterstützt sowohl neue (intervalle) als auch alte Struktur
     * Berücksichtigt auch Listen-Status-Felder
     */
    private fun istKundeUeberfaellig(
        customer: Customer,
        liste: KundenListe? = null,
        viewDateStart: Long,
        heuteStart: Long
    ): Boolean {
        // Prüfe Status: Kunde UND Liste (wenn vorhanden)
        val customerDone = customer.abholungErfolgt && customer.auslieferungErfolgt
        val listeDone = liste?.let { it.abholungErfolgt && it.auslieferungErfolgt } ?: false
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
            return termine.any { termin ->
                val istUeberfaellig = com.example.we2026_5.util.TerminBerechnungUtils.istUeberfaellig(
                    terminDatum = termin.datum,
                    aktuellesDatum = heuteStart,
                    erledigt = false // Wird separat geprüft
                )
                val sollAnzeigen = com.example.we2026_5.util.TerminBerechnungUtils.sollUeberfaelligAnzeigen(
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
    
    /**
     * Prüft ob ein Intervall innerhalb der nächsten 365 Tage fällig ist
     */
    private fun isIntervallFaelligInZukunft(intervall: ListeIntervall, abDatum: Long): Boolean {
        val abDatumStart = getStartOfDay(abDatum)
        val maxZukunft = abDatumStart + TimeUnit.DAYS.toMillis(365)
        
        if (!intervall.wiederholen) {
            // Einmaliges Intervall: Prüfe ob innerhalb von 365 Tagen
            val abholungStart = getStartOfDay(intervall.abholungDatum)
            val auslieferungStart = getStartOfDay(intervall.auslieferungDatum)
            return (abholungStart >= abDatumStart && abholungStart <= maxZukunft) ||
                   (auslieferungStart >= abDatumStart && auslieferungStart <= maxZukunft)
        }
        
        // Wiederholendes Intervall: Prüfe ob innerhalb von 365 Tagen ein Termin fällig ist
        val intervallTage = intervall.intervallTage.coerceIn(1, 365)
        val abholungStart = getStartOfDay(intervall.abholungDatum)
        val auslieferungStart = getStartOfDay(intervall.auslieferungDatum)
        
        // Prüfe Abholungstermine
        if (abDatumStart <= maxZukunft) {
            var naechsteAbholung = if (abDatumStart >= abholungStart) {
                val tageSeitAbholung = TimeUnit.MILLISECONDS.toDays(abDatumStart - abholungStart)
                val naechsterZyklus = ((tageSeitAbholung / intervallTage) + 1) * intervallTage
                abholungStart + TimeUnit.DAYS.toMillis(naechsterZyklus)
            } else {
                abholungStart
            }
            
            if (naechsteAbholung <= maxZukunft) {
                return true
            }
        }
        
        // Prüfe Auslieferungstermine
        if (abDatumStart <= maxZukunft) {
            var naechsteAuslieferung = if (abDatumStart >= auslieferungStart) {
                val tageSeitAuslieferung = TimeUnit.MILLISECONDS.toDays(abDatumStart - auslieferungStart)
                val naechsterZyklus = ((tageSeitAuslieferung / intervallTage) + 1) * intervallTage
                auslieferungStart + TimeUnit.DAYS.toMillis(naechsterZyklus)
            } else {
                auslieferungStart
            }
            
            if (naechsteAuslieferung <= maxZukunft) {
                return true
            }
        }
        
        return false
    }
    
    fun loadWeekData(weekStartTimestamp: Long, isSectionExpanded: (SectionType) -> Boolean) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                // Verwende die aktuellen Werte aus den Flows (Echtzeit-Updates)
                val allCustomers = customersFlow.value ?: emptyList()
                val allListen = listenFlow.value ?: emptyList()
                val heuteStart = getStartOfDay(System.currentTimeMillis())
                
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
                    val dayStart = getStartOfDay(dayTimestamp)
                    
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
                            isIntervallFaelligAm(intervall, dayStart) || 
                            isIntervallFaelligInZukunft(intervall, dayStart)
                        }
                        
                        if (istFaellig) {
                            val fälligeKunden = kunden.filter { customer ->
                                val faelligAm = customerFaelligAm(customer, liste, dayStart)
                                val faelligAmImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                                       faelligAm in customer.urlaubVon..customer.urlaubBis
                                if (faelligAmImUrlaub) return@filter false
                                
                                val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                                val isOverdue = istKundeUeberfaellig(customer, liste, dayStart, heuteStart)
                                
                                // Überfällige Termine: Zeige sie nur vom Termin-Tag bis zum aktuellen Datum (nicht in Zukunft)
                                if (isOverdue) {
                                    // Helper-Funktion prüft bereits ob angezeigt werden soll
                                    return@filter true
                                }
                                
                                // Normale Termine: Nur anzeigen, wenn genau an diesem Tag ein Termin ist
                                hatKundeTerminAmDatum(customer, liste, dayStart)
                            }
                            
                            if (fälligeKunden.isNotEmpty()) {
                                listenMitKunden[listeId] = fälligeKunden.sortedBy { it.name }
                            }
                        }
                    }
                    
                    // Kunden nach Listen gruppiert anzeigen (sowohl Privat als auch Gewerblich)
                    allListen.sortedBy { it.name }.forEach { liste ->
                        val kundenInListe = listenMitKunden[liste.id] ?: return@forEach
                        if (kundenInListe.isNotEmpty()) {
                            dayItems.add(ListItem.ListeHeader(liste.name, kundenInListe.size, liste.id))
                            kundenInListe.forEach { dayItems.add(ListItem.CustomerItem(it)) }
                        }
                    }
                    
                    // Gewerblich-Kunden ohne Liste filtern
                    val gewerblichKundenOhneListe = kundenOhneListe.filter { it.kundenArt == "Gewerblich" }
                    val filteredGewerblich = gewerblichKundenOhneListe.filter { customer ->
                        // Wochentag-Filterung entfernt - wird nicht mehr verwendet
                        if (!customer.wiederholen) {
                            val abholungAm = getStartOfDay(customer.abholungDatum)
                            val auslieferungAm = getStartOfDay(customer.auslieferungDatum)
                            if (abholungAm != dayStart && auslieferungAm != dayStart) return@filter false
                        }
                        
                        val faelligAm = customerFaelligAm(customer, null, dayStart)
                        val faelligAmImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                               faelligAm in customer.urlaubVon..customer.urlaubBis
                        if (faelligAmImUrlaub) return@filter false
                        
                        val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                        val isOverdue = istKundeUeberfaellig(customer, null, dayStart, heuteStart)
                        
                        // Überfällige Termine: Zeige sie nur vom Termin-Tag bis zum aktuellen Datum (nicht in Zukunft)
                        if (isOverdue) {
                            // Helper-Funktion prüft bereits ob angezeigt werden soll
                            return@filter true
                        }
                        
                        // Normale Termine: Nur anzeigen, wenn genau an diesem Tag ein Termin ist
                        hatKundeTerminAmDatum(customer, null, dayStart)
                    }
                    
                    val overdueGewerblich = mutableListOf<Customer>()
                    val normalGewerblich = mutableListOf<Customer>()
                    val doneGewerblich = mutableListOf<Customer>()
                    
                    filteredGewerblich.forEach { customer ->
                        val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                        val isOverdue = istKundeUeberfaellig(customer, null, dayStart, heuteStart)
                        
                        when {
                            isDone -> doneGewerblich.add(customer)
                            isOverdue -> overdueGewerblich.add(customer)
                            else -> normalGewerblich.add(customer)
                        }
                    }
                    
                    overdueGewerblich.sortBy { it.name }
                    normalGewerblich.sortBy { it.name }
                    doneGewerblich.sortBy { it.name }
                    
                    if (overdueGewerblich.isNotEmpty()) {
                        dayItems.add(ListItem.SectionHeader("ÜBERFÄLLIG", overdueGewerblich.size, SectionType.OVERDUE))
                        if (isSectionExpanded(SectionType.OVERDUE)) {
                            overdueGewerblich.forEach { dayItems.add(ListItem.CustomerItem(it)) }
                        }
                    }
                    
                    normalGewerblich.forEach { dayItems.add(ListItem.CustomerItem(it)) }
                    
                    if (doneGewerblich.isNotEmpty()) {
                        dayItems.add(ListItem.SectionHeader("ERLEDIGT", doneGewerblich.size, SectionType.DONE))
                        if (isSectionExpanded(SectionType.DONE)) {
                            doneGewerblich.forEach { dayItems.add(ListItem.CustomerItem(it)) }
                        }
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
    
    private fun getStartOfDay(ts: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    /**
     * Prüft ob ein Intervall an einem bestimmten Datum fällig ist
     * Berücksichtigt Termine für die nächsten 365 Tage
     */
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
    
}
