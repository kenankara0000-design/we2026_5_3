package com.example.we2026_5.ui.customermanager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.data.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
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
    
    // Kombiniere customers und searchQuery für gefilterte Liste
    val filteredCustomers: LiveData<List<Customer>> = combine(
        customersFlow,
        searchQueryFlow
    ) { customers, query ->
        if (query.isEmpty()) {
            customers
        } else {
            customers.filter {
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
    
    // Diese Funktion wird nicht mehr benötigt, da Flow automatisch aktualisiert
    // Behalten für Kompatibilität, aber macht nichts mehr
    fun loadCustomers() {
        // Flow aktualisiert automatisch, keine Aktion nötig
    }
    
    fun filterCustomers(query: String) {
        searchQueryFlow.value = query
    }
    
    fun deleteCustomer(customerId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.deleteCustomer(customerId)
                if (success) {
                    // Flow aktualisiert automatisch, keine manuelle Aktualisierung nötig
                    onSuccess()
                } else {
                    onError("Fehler beim Löschen des Kunden")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Fehler beim Löschen")
            }
        }
    }
}
