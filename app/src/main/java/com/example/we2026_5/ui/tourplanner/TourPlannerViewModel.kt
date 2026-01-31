package com.example.we2026_5.ui.tourplanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListItem
import com.example.we2026_5.SectionType
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.tourplanner.TourDataProcessor
import com.example.we2026_5.util.Result
import com.example.we2026_5.util.TerminBerechnungUtils
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
    
    // StateFlow für ausgewähltes Datum (Single Source of Truth für Tourenplaner-Datum)
    private val selectedTimestampFlow = MutableStateFlow<Long?>(null)
    /** Ausgewähltes Datum für UI (z. B. Anzeige, Prev/Next). */
    val selectedTimestamp: LiveData<Long?> = selectedTimestampFlow.asLiveData()

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
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // Sections standardmäßig eingeklappt (collapsed)
    private val expandedSections = mutableSetOf<SectionType>()
    
    fun loadTourData(selectedTimestamp: Long, isSectionExpanded: (SectionType) -> Boolean) {
        selectedTimestampFlow.value = selectedTimestamp
        expandedSectionsFlow.value = expandedSections.toSet()
    }

    /** Setzt das anzuzeigende Datum (z. B. beim Start). */
    fun setSelectedTimestamp(ts: Long) {
        selectedTimestampFlow.value = ts
        expandedSectionsFlow.value = expandedSections.toSet()
    }

    /** Nächster Tag. */
    fun nextDay() {
        val current = selectedTimestampFlow.value ?: return
        selectedTimestampFlow.value = current + TimeUnit.DAYS.toMillis(1)
    }

    /** Vorheriger Tag. */
    fun prevDay() {
        val current = selectedTimestampFlow.value ?: return
        selectedTimestampFlow.value = current - TimeUnit.DAYS.toMillis(1)
    }

    /** Springt auf heute. */
    fun goToToday() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        selectedTimestampFlow.value = cal.timeInMillis
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
    
    /** Aktuelle Listen (für TourPlanner ohne runBlocking). */
    fun getListen(): List<KundenListe> = _listenStateFlow.value

    /** Aktuell gewähltes Datum (für UI-Sync). */
    fun getSelectedTimestamp(): Long? = selectedTimestampFlow.value

    /** Fehlermeldung setzen (z. B. wenn Erledigung/Verschieben fehlschlägt). Activity zeigt nur an. */
    fun setError(message: String?) {
        _error.value = message
    }

    /** Fehler-State zurücksetzen. */
    fun clearError() {
        _error.value = null
    }

    /**
     * Löscht einen Einzeltermin (fügt Datum zu geloeschteTermine hinzu).
     * Activity ruft auf und zeigt Ergebnis (Toast/Error); Reload erfolgt in der Activity.
     */
    suspend fun deleteTerminFromCustomer(customer: Customer, terminDatum: Long): Result<Boolean> {
        val terminDatumStart = TerminBerechnungUtils.getStartOfDay(terminDatum)
        val aktuelleGeloeschteTermine = customer.geloeschteTermine.toMutableList()
        if (!aktuelleGeloeschteTermine.contains(terminDatumStart)) {
            aktuelleGeloeschteTermine.add(terminDatumStart)
        }
        return repository.updateCustomerResult(customer.id, mapOf("geloeschteTermine" to aktuelleGeloeschteTermine))
    }

    /**
     * Setzt Tour-Zyklus zurück (letzterTermin = jetzt, A/L = false, verschobenAufDatum = 0).
     * Activity ruft auf und zeigt Ergebnis.
     */
    suspend fun resetTourCycle(customerId: String): Result<Boolean> {
        val resetData = mapOf(
            "letzterTermin" to System.currentTimeMillis(),
            "abholungErfolgt" to false,
            "auslieferungErfolgt" to false,
            "verschobenAufDatum" to 0
        )
        return repository.updateCustomerResult(customerId, resetData)
    }
}
