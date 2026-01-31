package com.example.we2026_5.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.util.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel für CustomerDetailActivity.
 * Hält Kunden-State und Geschäftslogik (Laden, Speichern, Löschen).
 * Activity beobachtet nur State und leitet Klicks weiter.
 */
class CustomerDetailViewModel(
    private val repository: CustomerRepository
) : ViewModel() {

    private val _customerId = MutableStateFlow<String?>(null)

    /** true, sobald getCustomerFlow mindestens einmal emittiert hat (dann ist null = wirklich nicht vorhanden). */
    private val _loadComplete = MutableStateFlow(false)
    val loadComplete: StateFlow<Boolean> = _loadComplete.asStateFlow()

    /** Echtzeit-Kundendaten aus Repository. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentCustomer: StateFlow<Customer?> = _customerId
        .flatMapLatest { id ->
            if (id == null) {
                _loadComplete.value = false
                flowOf(null)
            } else {
                repository.getCustomerFlow(id).map { customer ->
                    _loadComplete.value = true
                    customer
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** true nach erfolgreichem Löschen → Activity beendet sich mit Result. */
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    /** Kurzzeitig true während Speichern/Löschen. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** true während Foto-Upload. */
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    fun setUploading(uploading: Boolean) {
        _isUploading.value = uploading
    }

    /** Fehlermeldung für Toast/Snackbar. */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Bearbeitungsmodus (für Compose-Screen). */
    private val _isInEditMode = MutableStateFlow(false)
    val isInEditMode: StateFlow<Boolean> = _isInEditMode.asStateFlow()

    /** Intervalle im Bearbeitungsmodus (für Compose). */
    private val _editIntervalle = MutableStateFlow<List<CustomerIntervall>>(emptyList())
    val editIntervalle: StateFlow<List<CustomerIntervall>> = _editIntervalle.asStateFlow()

    fun setCustomerId(id: String) {
        _customerId.value = id
    }

    /**
     * Speichert Kunden-Updates. Fehler werden über [errorMessage] gemeldet (zentrale Fehlerbehandlung).
     * Nach Abschluss wird onComplete(success) auf dem Main-Thread aufgerufen.
     */
    fun saveCustomer(updates: Map<String, Any>, onComplete: ((Boolean) -> Unit)? = null) {
        val id = _customerId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = repository.updateCustomerResult(id, updates)) {
                is Result.Success -> onComplete?.invoke(result.data)
                is Result.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun deleteCustomer() {
        val id = _customerId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = repository.deleteCustomerResult(id)) {
                is Result.Success -> _deleted.value = true
                is Result.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun setEditMode(editing: Boolean, customer: Customer? = null) {
        _isInEditMode.value = editing
        if (editing && customer != null) {
            _editIntervalle.value = customer.intervalle
        }
    }

    fun updateEditIntervalle(intervalle: List<CustomerIntervall>) {
        _editIntervalle.value = intervalle
    }
}
