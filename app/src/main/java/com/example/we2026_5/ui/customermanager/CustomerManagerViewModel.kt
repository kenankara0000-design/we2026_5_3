package com.example.we2026_5.ui.customermanager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerStatus
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

class CustomerManagerViewModel(
    private val repository: CustomerRepository
) : ViewModel() {
    
    // Echtzeit-Listener: Flow wird automatisch in LiveData umgewandelt
    private val customersFlow = repository.getAllCustomersFlow()
        .map { customers -> customers.sortedBy { it.displayName.uppercase() } }
    
    // StateFlow für Such-Query
    private val searchQueryFlow = MutableStateFlow("")
    
    // StateFlow für ausgewählten Tab (0=Gewerblich, 1=Privat, 2=Liste)
    private val selectedTabFlow = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = selectedTabFlow.asStateFlow()

    // StateFlow für KundenTyp-Filter (0=Alle, 1=Regelmäßig, 2=Unregelmäßig)
    private val kundenTypFilterFlow = MutableStateFlow(0)
    val kundenTypFilter: StateFlow<Int> = kundenTypFilterFlow.asStateFlow()

    // StateFlow für Ohne-Tour-Filter (0=Alle, 1=Nur Ohne Tour, 2=Ohne Tour ausblenden)
    private val ohneTourFilterFlow = MutableStateFlow(0)
    val ohneTourFilter: StateFlow<Int> = ohneTourFilterFlow.asStateFlow()

    // StateFlow für Pausierte-Filter (0=Ausblenden, 1=Anzeigen)
    private val pausierteFilterFlow = MutableStateFlow(0)
    val pausierteFilter: StateFlow<Int> = pausierteFilterFlow.asStateFlow()

    // StateFlow für Keine-Termine-Filter (0=Alle, 1=Nur Kunden ohne Termine; ohne-Tour-Kunden werden ausgeschlossen)
    private val keinetermineFilterFlow = MutableStateFlow(0)
    val keinetermineFilter: StateFlow<Int> = keinetermineFilterFlow.asStateFlow()

    // Kombiniere customers, searchQuery und selectedTab für gefilterte Liste
    val filteredCustomers: LiveData<List<Customer>> = combine(
        customersFlow,
        searchQueryFlow,
        selectedTabFlow,
        kundenTypFilterFlow,
        ohneTourFilterFlow,
        pausierteFilterFlow,
        keinetermineFilterFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val customers = values[0] as List<Customer>
        val query = values[1] as String
        val selectedTab = values[2] as Int
        val typFilter = values[3] as Int
        val ohneTourFilter = values[4] as Int
        val pausierteFilter = values[5] as Int
        val keinetermineFilter = values[6] as Int
        // Zuerst nach Tab filtern
        val tabFiltered = when (selectedTab) {
            0 -> customers.filter { it.kundenArt == "Gewerblich" }
            1 -> customers.filter { it.kundenArt == "Privat" }
            2 -> customers.filter { it.kundenArt == "Tour" }
            else -> customers
        }

        val typFiltered = when (typFilter) {
            1 -> tabFiltered.filter { it.kundenTyp == KundenTyp.REGELMAESSIG }
            2 -> tabFiltered.filter { it.kundenTyp == KundenTyp.UNREGELMAESSIG }
            3 -> tabFiltered.filter { it.kundenTyp == KundenTyp.AUF_ABRUF }
            else -> tabFiltered
        }

        val ohneTourFiltered = when (ohneTourFilter) {
            1 -> typFiltered.filter { it.ohneTour }
            2 -> typFiltered.filter { !it.ohneTour }
            else -> typFiltered
        }

        val pausierteFiltered = when (pausierteFilter) {
            0 -> ohneTourFiltered.filter { it.status != CustomerStatus.PAUSIERT }
            1 -> ohneTourFiltered
            else -> ohneTourFiltered
        }

        val keinetermineFiltered = when (keinetermineFilter) {
            1 -> pausierteFiltered.filter { !it.ohneTour && hatKeineTermine(it) }
            else -> pausierteFiltered
        }

        // Dann nach Such-Query filtern
        val queryFiltered = if (query.isEmpty()) {
            keinetermineFiltered
        } else {
            keinetermineFiltered.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true) ||
                it.alias.contains(query, ignoreCase = true) ||
                it.adresse.contains(query, ignoreCase = true)
            }
        }
        queryFiltered.sortedBy { it.displayName.uppercase() }
    }.asLiveData()
    
    // Für Kompatibilität: customers ohne Filter
    val customers: LiveData<List<Customer>> = customersFlow.asLiveData()
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Bulk-Auswahl (für Compose-Screen)
    private val _isBulkMode = MutableStateFlow(false)
    val isBulkMode: StateFlow<Boolean> = _isBulkMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    fun setBulkMode(enabled: Boolean) {
        _isBulkMode.value = enabled
        if (!enabled) _selectedIds.value = emptySet()
    }

    fun toggleSelection(customerId: String) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (customerId in this) remove(customerId) else add(customerId)
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }
    
    // Diese Funktion wird nicht mehr benötigt, da Flow automatisch aktualisiert
    // Behalten für Kompatibilität, aber macht nichts mehr
    fun loadCustomers() {
        // Flow aktualisiert automatisch, keine Aktion nötig
    }
    
    fun filterCustomers(query: String) {
        searchQueryFlow.value = query
    }
    
    fun setSelectedTab(tabIndex: Int) {
        selectedTabFlow.value = tabIndex
    }

    fun setKundenTypFilter(filterIndex: Int) {
        kundenTypFilterFlow.value = filterIndex
    }

    fun setOhneTourFilter(filterIndex: Int) {
        ohneTourFilterFlow.value = filterIndex
    }

    fun setPausierteFilter(filterIndex: Int) {
        pausierteFilterFlow.value = filterIndex
    }

    fun setKeinetermineFilter(filterIndex: Int) {
        keinetermineFilterFlow.value = filterIndex
    }

    /** Kunde hat keine Termine: keine Intervalle, keine Wochentage, keine Kunden-/Ausnahme-Termine. */
    private fun hatKeineTermine(c: Customer): Boolean =
        c.intervalle.isEmpty() &&
            c.effectiveAbholungWochentage.isEmpty() &&
            c.kundenTermine.isEmpty() &&
            c.ausnahmeTermine.isEmpty()
    
    fun deleteCustomer(customerId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _error.value = null
            when (val result = repository.deleteCustomerResult(customerId)) {
                is Result.Success -> onSuccess()
                is Result.Error -> {
                    _error.value = result.message
                    onError(result.message)
                }
                is Result.Loading -> { /* Ignorieren */ }
            }
        }
    }

    /** Fehler-State zurücksetzen (z. B. nach Anzeige oder Retry). */
    fun clearError() {
        _error.value = null
    }
}
