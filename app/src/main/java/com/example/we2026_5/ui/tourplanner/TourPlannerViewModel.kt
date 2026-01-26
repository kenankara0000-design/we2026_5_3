package com.example.we2026_5.ui.tourplanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
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
                
                // Kunden nach Art trennen
                val privatKunden = allCustomers.filter { it.kundenArt == "Privat" }
                val gewerblichKunden = allCustomers.filter { it.kundenArt == "Gewerblich" }
                
                // Privat-Kunden nach Listen gruppieren
                val kundenNachListen = privatKunden.groupBy { it.listeId }
                
                // Gewerblich-Kunden filtern (alte Logik)
                val filteredGewerblich = gewerblichKunden.filter { customer ->
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
                
                // Privat-Kunden filtern: Prüfe ob Liste an diesem Tag fällig ist
                val listenMitKunden = mutableMapOf<String, List<Customer>>()
                kundenNachListen.forEach { (listeId, kunden) ->
                    if (listeId.isEmpty()) return@forEach
                    val liste = allListen.find { it.id == listeId } ?: return@forEach
                    
                    // Prüfe ob Abholung oder Auslieferung an diesem Wochentag fällig ist
                    val istAbholungTag = liste.abholungWochentag == viewDateWochentag
                    val istAuslieferungTag = liste.auslieferungWochentag == viewDateWochentag
                    
                    if (istAbholungTag || istAuslieferungTag) {
                        // Filtere Kunden die an diesem Tag fällig sind
                        val fälligeKunden = kunden.filter { customer ->
                            val faelligAm = customerFaelligAm(customer)
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
                
                // Liste mit Items erstellen
                val items = mutableListOf<ListItem>()
                
                // Privat-Kunden nach Listen gruppiert anzeigen
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
    
    private fun customerFaelligAm(c: Customer): Long {
        return c.getFaelligAm()
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
}
