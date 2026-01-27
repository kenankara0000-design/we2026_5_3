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
import com.example.we2026_5.ui.tourplanner.TourPlannerWeekDataProcessor
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
    private val weekDataProcessor = TourPlannerWeekDataProcessor(dataProcessor)
    
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
    
    fun loadWeekData(weekStartTimestamp: Long, isSectionExpanded: (SectionType) -> Boolean) {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                // Verwende die aktuellen Werte aus den Flows (Echtzeit-Updates)
                val allCustomers = customersFlow.value ?: emptyList()
                val allListen = listenFlow.value ?: emptyList()
                
                // Verwende WeekDataProcessor für die Verarbeitung
                val weekData = weekDataProcessor.processWeekData(
                    allCustomers = allCustomers,
                    allListen = allListen,
                    weekStartTimestamp = weekStartTimestamp,
                    isSectionExpanded = isSectionExpanded
                )
                
                _weekItems.value = weekData
            } catch (e: Exception) {
                _error.value = e.message ?: "Fehler beim Laden der Wochenansicht"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Alle Datenverarbeitungsfunktionen entfernt - jetzt in TourDataProcessor und TourPlannerWeekDataProcessor
}
