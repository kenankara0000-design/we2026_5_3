package com.example.we2026_5.ui.tourplanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.ListItem
import com.example.we2026_5.SectionType
import com.example.we2026_5.data.repository.CustomerRepository
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class TourPlannerViewModel(
    private val repository: CustomerRepository
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
                val viewDateStart = getStartOfDay(selectedTimestamp)
                val heuteStart = getStartOfDay(System.currentTimeMillis())
                
                // Wochentag berechnen (0=Montag, 6=Sonntag)
                val calViewDate = Calendar.getInstance().apply {
                    timeInMillis = viewDateStart
                }
                val viewDateWochentag = (calViewDate.get(Calendar.DAY_OF_WEEK) + 5) % 7
                
                // Kunden filtern
                val filteredCustomers = allCustomers.filter { customer ->
                    if (customer.wochentag != viewDateWochentag) return@filter false
                    
                    val faelligAm = customerFaelligAm(customer)
                    val faelligAmImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                           faelligAm in customer.urlaubVon..customer.urlaubBis
                    if (faelligAmImUrlaub) return@filter false
                    
                    // Kunden anzeigen, die an diesem Tag fällig sind (Termin liegt an diesem Tag)
                    faelligAm <= viewDateStart + TimeUnit.DAYS.toMillis(1)
                }
                
                // Kunden in Gruppen einteilen
                val overdueCustomers = mutableListOf<Customer>()
                val normalCustomers = mutableListOf<Customer>()
                val doneCustomers = mutableListOf<Customer>()
                
                filteredCustomers.forEach { customer ->
                    val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                    val faelligAm = customerFaelligAm(customer)
                    
                    // Überfällig: Termin liegt in der Vergangenheit UND Kunde ist nicht erledigt
                    // WICHTIG: Überfällige Kunden nur anzeigen, wenn das angezeigte Datum in der Vergangenheit oder heute ist
                    // Nicht in der Zukunft anzeigen! (viewDateStart <= heuteStart)
                    val isOverdue = !isDone && faelligAm < heuteStart && viewDateStart <= heuteStart
                    
                    when {
                        isDone -> doneCustomers.add(customer)
                        isOverdue -> overdueCustomers.add(customer)
                        else -> normalCustomers.add(customer)
                    }
                }
                
                // Sortierung nach Reihenfolge
                overdueCustomers.sortBy { it.reihenfolge }
                normalCustomers.sortBy { it.reihenfolge }
                doneCustomers.sortBy { it.reihenfolge }
                
                // Liste mit Section Headers erstellen
                val items = mutableListOf<ListItem>()
                
                if (overdueCustomers.isNotEmpty()) {
                    items.add(ListItem.SectionHeader("ÜBERFÄLLIG", overdueCustomers.size, SectionType.OVERDUE))
                    if (isSectionExpanded(SectionType.OVERDUE)) {
                        overdueCustomers.forEach { items.add(ListItem.CustomerItem(it)) }
                    }
                }
                
                normalCustomers.forEach { items.add(ListItem.CustomerItem(it)) }
                
                if (doneCustomers.isNotEmpty()) {
                    items.add(ListItem.SectionHeader("ERLEDIGT", doneCustomers.size, SectionType.DONE))
                    if (isSectionExpanded(SectionType.DONE)) {
                        doneCustomers.forEach { items.add(ListItem.CustomerItem(it)) }
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
        return if (c.verschobenAufDatum > 0) c.verschobenAufDatum
        else c.letzterTermin + TimeUnit.DAYS.toMillis(c.intervallTage.toLong())
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
