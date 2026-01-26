package com.example.we2026_5.ui.customermanager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.data.repository.CustomerRepository
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

class CustomerManagerViewModel(
    private val repository: CustomerRepository
) : ViewModel() {
    
    private val _customers = MutableLiveData<List<Customer>>()
    val customers: LiveData<List<Customer>> = _customers
    
    private val _filteredCustomers = MutableLiveData<List<Customer>>()
    val filteredCustomers: LiveData<List<Customer>> = _filteredCustomers
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    init {
        loadCustomers()
    }
    
    fun loadCustomers() {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val allCustomers = repository.getAllCustomers()
                    .sortedBy { it.name.uppercase() } // Nach A-Z sortieren
                _customers.value = allCustomers
                _filteredCustomers.value = allCustomers
            } catch (e: Exception) {
                _error.value = e.message ?: "Fehler beim Laden der Kunden"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun filterCustomers(query: String) {
        val allCustomers = _customers.value ?: emptyList()
        val filtered = if (query.isEmpty()) {
            allCustomers
        } else {
            allCustomers.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.adresse.contains(query, ignoreCase = true)
            }
        }.sortedBy { it.name.uppercase() } // Nach A-Z sortieren
        _filteredCustomers.value = filtered
    }
    
    fun deleteCustomer(customerId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.deleteCustomer(customerId)
                if (success) {
                    // Kunde sofort aus der Liste entfernen (optimistische UI-Aktualisierung)
                    val currentCustomers = _customers.value?.toMutableList() ?: mutableListOf()
                    currentCustomers.removeAll { it.id == customerId }
                    _customers.value = currentCustomers.sortedBy { it.name.uppercase() }
                    
                    // Gefilterte Liste auch aktualisieren
                    val currentFiltered = _filteredCustomers.value?.toMutableList() ?: mutableListOf()
                    currentFiltered.removeAll { it.id == customerId }
                    _filteredCustomers.value = currentFiltered.sortedBy { it.name.uppercase() }
                    
                    // Dann die Liste neu laden um sicherzustellen, dass alles synchron ist
                    loadCustomers()
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
