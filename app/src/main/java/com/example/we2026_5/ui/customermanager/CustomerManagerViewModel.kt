package com.example.we2026_5.ui.customermanager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
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
        .map { customers -> customers.sortedBy { it.name.uppercase() } }
    
    // StateFlow für Such-Query
    private val searchQueryFlow = MutableStateFlow("")
    
    // StateFlow für ausgewählten Tab (0=Gewerblich, 1=Privat, 2=Liste)
    private val selectedTabFlow = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = selectedTabFlow.asStateFlow()

    // StateFlow für KundenTyp-Filter (0=Alle, 1=Regelmäßig, 2=Unregelmäßig)
    private val kundenTypFilterFlow = MutableStateFlow(0)
    val kundenTypFilter: StateFlow<Int> = kundenTypFilterFlow.asStateFlow()

    // Kombiniere customers, searchQuery und selectedTab für gefilterte Liste
    val filteredCustomers: LiveData<List<Customer>> = combine(
        customersFlow,
        searchQueryFlow,
        selectedTabFlow,
        kundenTypFilterFlow
    ) { customers, query, selectedTab, typFilter ->
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
        
        // Dann nach Such-Query filtern
        if (query.isEmpty()) {
            typFiltered
        } else {
            typFiltered.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.adresse.contains(query, ignoreCase = true)
            }
        }.sortedBy { it.name.uppercase() }
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
    
    fun deleteCustomer(customerId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _error.value = null
            when (val result = repository.deleteCustomerResult(customerId)) {
                is Result.Success -> onSuccess()
                is Result.Error -> {
                    _error.value = result.message
                    onError(result.message)
                }
            }
        }
    }

    /** Fehler-State zurücksetzen (z. B. nach Anzeige oder Retry). */
    fun clearError() {
        _error.value = null
    }
}
