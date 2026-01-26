package com.example.we2026_5.ui.tourplanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.ListItem
import com.example.we2026_5.SectionType
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class TourPlannerViewModel(
    private val repository: CustomerRepository,
    private val listeRepository: KundenListeRepository
) : ViewModel() {
    
    private val _tourItems = MutableLiveData<List<ListItem>>()
    val tourItems: LiveData<List<ListItem>> = _tourItems
    
    private val _weekItems = MutableLiveData<Map<Int, List<ListItem>>>()
    val weekItems: LiveData<Map<Int, List<ListItem>>> = _weekItems
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val expandedSections = mutableSetOf<SectionType>()
    
    fun loadTourData(selectedTimestamp: Long, isSectionExpanded: (SectionType) -> Boolean) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val allCustomers = repository.getAllCustomers()
                val allListen = listeRepository.getAllListen()
                val viewDateStart = getStartOfDay(selectedTimestamp)
                val heuteStart = getStartOfDay(System.currentTimeMillis())
                
                // Wochentag berechnen (0=Montag, 6=Sonntag)
                val calViewDate = Calendar.getInstance().apply {
                    timeInMillis = viewDateStart
                }
                val viewDateWochentag = (calViewDate.get(Calendar.DAY_OF_WEEK) + 5) % 7
                
                // Alle Kunden nach Listen gruppieren (sowohl Privat als auch Gewerblich)
                val kundenNachListen = allCustomers.filter { it.listeId.isNotEmpty() }.groupBy { it.listeId }
                val kundenOhneListe = allCustomers.filter { it.listeId.isEmpty() }
                
                // Kunden in Listen filtern: Prüfe ob Liste an diesem Tag fällig ist
                val listenMitKunden = mutableMapOf<String, List<Customer>>()
                kundenNachListen.forEach { (listeId, kunden) ->
                    if (listeId.isEmpty()) return@forEach
                    val liste = allListen.find { it.id == listeId } ?: return@forEach
                    
                    // Prüfe ob mindestens ein Intervall an diesem Datum fällig ist
                    val istFaellig = liste.intervalle.any { intervall ->
                        isIntervallFaelligAm(intervall, viewDateStart)
                    }
                    
                    if (istFaellig) {
                        // Filtere Kunden die an diesem Tag fällig sind
                        val fälligeKunden = kunden.filter { customer ->
                            val faelligAm = customerFaelligAm(customer, liste)
                            val faelligAmImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                                   faelligAm in customer.urlaubVon..customer.urlaubBis
                            if (faelligAmImUrlaub) return@filter false
                            
                            val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                            val isOverdue = !isDone && faelligAm < heuteStart
                            
                            if (isOverdue && viewDateStart > heuteStart) return@filter false
                            
                            faelligAm <= viewDateStart + TimeUnit.DAYS.toMillis(1)
                        }
                        
                        if (fälligeKunden.isNotEmpty()) {
                            listenMitKunden[listeId] = fälligeKunden.sortedBy { it.reihenfolge }
                        }
                    }
                }
                
                // Gewerblich-Kunden ohne Liste filtern (alte Logik)
                val gewerblichKundenOhneListe = kundenOhneListe.filter { it.kundenArt == "Gewerblich" }
                val filteredGewerblich = gewerblichKundenOhneListe.filter { customer ->
                    if (customer.wiederholen && customer.wochentag != viewDateWochentag) return@filter false
                    if (!customer.wiederholen) {
                        // Einmaliger Termin: Prüfe ob Abholungsdatum an diesem Tag liegt
                        val abholungAm = getStartOfDay(customer.abholungDatum)
                        val auslieferungAm = getStartOfDay(customer.auslieferungDatum)
                        if (abholungAm != viewDateStart && auslieferungAm != viewDateStart) return@filter false
                    }
                    
                    val faelligAm = customerFaelligAm(customer)
                    val faelligAmImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                           faelligAm in customer.urlaubVon..customer.urlaubBis
                    if (faelligAmImUrlaub) return@filter false
                    
                    val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                    val isOverdue = !isDone && faelligAm < heuteStart
                    
                    if (isOverdue && viewDateStart > heuteStart) return@filter false
                    
                    faelligAm <= viewDateStart + TimeUnit.DAYS.toMillis(1)
                }
                
                // Liste mit Items erstellen
                val items = mutableListOf<ListItem>()
                
                // Kunden nach Listen gruppiert anzeigen (sowohl Privat als auch Gewerblich)
                allListen.sortedBy { it.name }.forEach { liste ->
                    val kundenInListe = listenMitKunden[liste.id] ?: return@forEach
                    if (kundenInListe.isNotEmpty()) {
                        items.add(ListItem.ListeHeader(liste.name, kundenInListe.size, liste.id))
                        // Kunden werden nur angezeigt wenn Liste expanded ist (wird im Adapter gehandhabt)
                        kundenInListe.forEach { items.add(ListItem.CustomerItem(it)) }
                    }
                }
                
                // Gewerblich-Kunden (alte Logik)
                val overdueGewerblich = mutableListOf<Customer>()
                val normalGewerblich = mutableListOf<Customer>()
                val doneGewerblich = mutableListOf<Customer>()
                
                filteredGewerblich.forEach { customer ->
                    val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                    val faelligAm = customerFaelligAm(customer)
                    val isOverdue = !isDone && faelligAm < heuteStart && viewDateStart <= heuteStart
                    
                    when {
                        isDone -> doneGewerblich.add(customer)
                        isOverdue -> overdueGewerblich.add(customer)
                        else -> normalGewerblich.add(customer)
                    }
                }
                
                overdueGewerblich.sortBy { it.reihenfolge }
                normalGewerblich.sortBy { it.reihenfolge }
                doneGewerblich.sortBy { it.reihenfolge }
                
                if (overdueGewerblich.isNotEmpty()) {
                    items.add(ListItem.SectionHeader("ÜBERFÄLLIG", overdueGewerblich.size, SectionType.OVERDUE))
                    if (isSectionExpanded(SectionType.OVERDUE)) {
                        overdueGewerblich.forEach { items.add(ListItem.CustomerItem(it)) }
                    }
                }
                
                normalGewerblich.forEach { items.add(ListItem.CustomerItem(it)) }
                
                if (doneGewerblich.isNotEmpty()) {
                    items.add(ListItem.SectionHeader("ERLEDIGT", doneGewerblich.size, SectionType.DONE))
                    if (isSectionExpanded(SectionType.DONE)) {
                        doneGewerblich.forEach { items.add(ListItem.CustomerItem(it)) }
                    }
                }
                
                _tourItems.value = items
            } catch (e: Exception) {
                _error.value = e.message ?: "Fehler beim Laden der Touren"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun toggleSection(sectionType: SectionType) {
        if (expandedSections.contains(sectionType)) {
            expandedSections.remove(sectionType)
        } else {
            expandedSections.add(sectionType)
        }
    }
    
    fun isSectionExpanded(sectionType: SectionType): Boolean {
        return expandedSections.contains(sectionType)
    }
    
    private fun customerFaelligAm(c: Customer, liste: KundenListe? = null): Long {
        // Für Kunden in Listen: Daten der Liste verwenden
        if (c.listeId.isNotEmpty() && liste != null) {
            // Wenn verschoben, verschobenes Datum verwenden
            if (c.verschobenAufDatum > 0) return c.verschobenAufDatum
            
            // Nächstes Datum aus den Intervallen der Liste berechnen
            val naechstesDatum = getNaechstesListeDatum(liste)
            return naechstesDatum ?: System.currentTimeMillis()
        }
        
        // Für Kunden ohne Liste: Normale Logik
        return c.getFaelligAm()
    }
    
    fun loadWeekData(weekStartTimestamp: Long, isSectionExpanded: (SectionType) -> Boolean) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val allCustomers = repository.getAllCustomers()
                val allListen = listeRepository.getAllListen()
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
                    val dayWochentag = dayOffset // 0=Montag, 6=Sonntag
                    
                    val dayItems = mutableListOf<ListItem>()
                    
                    // Alle Kunden nach Listen gruppieren (sowohl Privat als auch Gewerblich)
                    val kundenNachListen = allCustomers.filter { it.listeId.isNotEmpty() }.groupBy { it.listeId }
                    val kundenOhneListe = allCustomers.filter { it.listeId.isEmpty() }
                    val listenMitKunden = mutableMapOf<String, List<Customer>>()
                    
                    kundenNachListen.forEach { (listeId, kunden) ->
                        if (listeId.isEmpty()) return@forEach
                        val liste = allListen.find { it.id == listeId } ?: return@forEach
                        
                        // Prüfe ob mindestens ein Intervall an diesem Datum fällig ist
                        val istFaellig = liste.intervalle.any { intervall ->
                            isIntervallFaelligAm(intervall, dayStart)
                        }
                        
                        if (istFaellig) {
                            val fälligeKunden = kunden.filter { customer ->
                                val faelligAm = customerFaelligAm(customer, liste)
                                val faelligAmImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                                       faelligAm in customer.urlaubVon..customer.urlaubBis
                                if (faelligAmImUrlaub) return@filter false
                                
                                val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                                val isOverdue = !isDone && faelligAm < heuteStart
                                
                                if (isOverdue && dayStart > heuteStart) return@filter false
                                
                                faelligAm <= dayStart + TimeUnit.DAYS.toMillis(1)
                            }
                            
                            if (fälligeKunden.isNotEmpty()) {
                                listenMitKunden[listeId] = fälligeKunden.sortedBy { it.reihenfolge }
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
                        if (customer.wiederholen && customer.wochentag != dayWochentag) return@filter false
                        if (!customer.wiederholen) {
                            val abholungAm = getStartOfDay(customer.abholungDatum)
                            val auslieferungAm = getStartOfDay(customer.auslieferungDatum)
                            if (abholungAm != dayStart && auslieferungAm != dayStart) return@filter false
                        }
                        
                        val faelligAm = customerFaelligAm(customer)
                        val faelligAmImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                               faelligAm in customer.urlaubVon..customer.urlaubBis
                        if (faelligAmImUrlaub) return@filter false
                        
                        val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                        val isOverdue = !isDone && faelligAm < heuteStart
                        
                        if (isOverdue && dayStart > heuteStart) return@filter false
                        
                        faelligAm <= dayStart + TimeUnit.DAYS.toMillis(1)
                    }
                    
                    val overdueGewerblich = mutableListOf<Customer>()
                    val normalGewerblich = mutableListOf<Customer>()
                    val doneGewerblich = mutableListOf<Customer>()
                    
                    filteredGewerblich.forEach { customer ->
                        val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                        val faelligAm = customerFaelligAm(customer)
                        val isOverdue = !isDone && faelligAm < heuteStart && dayStart <= heuteStart
                        
                        when {
                            isDone -> doneGewerblich.add(customer)
                            isOverdue -> overdueGewerblich.add(customer)
                            else -> normalGewerblich.add(customer)
                        }
                    }
                    
                    overdueGewerblich.sortBy { it.reihenfolge }
                    normalGewerblich.sortBy { it.reihenfolge }
                    doneGewerblich.sortBy { it.reihenfolge }
                    
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
        
        // Prüfe Abholungsdatum
        if (datumStart >= abholungStart) {
            val tageSeitAbholung = TimeUnit.MILLISECONDS.toDays(datumStart - abholungStart)
            if (tageSeitAbholung % intervallTage == 0L && datumStart == abholungStart + TimeUnit.DAYS.toMillis(tageSeitAbholung)) {
                return true
            }
        }
        
        // Prüfe Auslieferungsdatum
        if (datumStart >= auslieferungStart) {
            val tageSeitAuslieferung = TimeUnit.MILLISECONDS.toDays(datumStart - auslieferungStart)
            if (tageSeitAuslieferung % intervallTage == 0L && datumStart == auslieferungStart + TimeUnit.DAYS.toMillis(tageSeitAuslieferung)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Berechnet das nächste fällige Datum für einen Kunden in einer Liste
     */
    private fun getNaechstesListeDatum(liste: KundenListe, abDatum: Long = System.currentTimeMillis()): Long? {
        val abDatumStart = getStartOfDay(abDatum)
        var naechstesDatum: Long? = null
        
        // Durch alle Intervalle iterieren und das nächste Datum finden
        liste.intervalle.forEach { intervall ->
            if (!intervall.wiederholen) {
                // Einmaliges Intervall
                val abholungStart = getStartOfDay(intervall.abholungDatum)
                val auslieferungStart = getStartOfDay(intervall.auslieferungDatum)
                
                if (abholungStart >= abDatumStart && (naechstesDatum == null || abholungStart < naechstesDatum!!)) {
                    naechstesDatum = abholungStart
                }
                if (auslieferungStart >= abDatumStart && (naechstesDatum == null || auslieferungStart < naechstesDatum!!)) {
                    naechstesDatum = auslieferungStart
                }
            } else {
                // Wiederholendes Intervall
                val intervallTage = intervall.intervallTage.coerceIn(1, 365)
                val abholungStart = getStartOfDay(intervall.abholungDatum)
                val auslieferungStart = getStartOfDay(intervall.auslieferungDatum)
                
                // Nächstes Abholungsdatum
                if (abDatumStart >= abholungStart) {
                    val tageSeitAbholung = TimeUnit.MILLISECONDS.toDays(abDatumStart - abholungStart)
                    val naechsteAbholung = abholungStart + TimeUnit.DAYS.toMillis((tageSeitAbholung / intervallTage + 1) * intervallTage)
                    if (naechstesDatum == null || naechsteAbholung < naechstesDatum!!) {
                        naechstesDatum = naechsteAbholung
                    }
                } else {
                    if (naechstesDatum == null || abholungStart < naechstesDatum!!) {
                        naechstesDatum = abholungStart
                    }
                }
                
                // Nächstes Auslieferungsdatum
                if (abDatumStart >= auslieferungStart) {
                    val tageSeitAuslieferung = TimeUnit.MILLISECONDS.toDays(abDatumStart - auslieferungStart)
                    val naechsteAuslieferung = auslieferungStart + TimeUnit.DAYS.toMillis((tageSeitAuslieferung / intervallTage + 1) * intervallTage)
                    if (naechstesDatum == null || naechsteAuslieferung < naechstesDatum!!) {
                        naechstesDatum = naechsteAuslieferung
                    }
                } else {
                    if (naechstesDatum == null || auslieferungStart < naechstesDatum!!) {
                        naechstesDatum = auslieferungStart
                    }
                }
            }
        }
        
        return naechstesDatum
    }
}
