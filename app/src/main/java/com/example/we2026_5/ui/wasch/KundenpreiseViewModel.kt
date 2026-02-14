package com.example.we2026_5.ui.wasch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.data.repository.ArticleRepository
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenPreiseRepository
import com.example.we2026_5.util.CustomerSearchHelper
import com.example.we2026_5.wasch.KundenPreis
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class KundenpreiseUiState {
    data class KundeSuchen(val customerSearchQuery: String = "", val customers: List<Customer> = emptyList()) : KundenpreiseUiState()
    data class KundenpreiseList(val customer: Customer) : KundenpreiseUiState()
}

class KundenpreiseViewModel(
    private val customerRepository: CustomerRepository,
    private val kundenPreiseRepository: KundenPreiseRepository,
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<KundenpreiseUiState>(KundenpreiseUiState.KundeSuchen())
    val uiState: StateFlow<KundenpreiseUiState> = _uiState.asStateFlow()

    private val _kundenPreiseList = MutableStateFlow<List<KundenPreis>>(emptyList())
    val kundenPreiseList: StateFlow<List<KundenPreis>> = _kundenPreiseList.asStateFlow()

    private val _isLoadingPreise = MutableStateFlow(false)
    val isLoadingPreise: StateFlow<Boolean> = _isLoadingPreise.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var preiseJob: Job? = null
    /** Cache für Kunde-Suche: nur bei Eingabe Treffer anzeigen, nicht alle Kunden beim Start. */
    private var customersSearchCache: List<Customer>? = null

    val articles = articleRepository.getAllArticlesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Keine Kundenliste beim Start – nur Treffer bei Suche anzeigen
    }

    fun setCustomerSearchQuery(query: String) {
        val s = _uiState.value
        if (s !is KundenpreiseUiState.KundeSuchen) return
        // Sofort Query aktualisieren, damit der Cursor nicht zurückspringt (Recomposition)
        if (query.isBlank()) {
            _uiState.value = s.copy(customerSearchQuery = query, customers = emptyList())
            return
        }
        _uiState.value = s.copy(customerSearchQuery = query)
        viewModelScope.launch {
            if (customersSearchCache == null) {
                customersSearchCache = customerRepository.getAllCustomers().sortedBy { it.displayName }
            }
            val filtered = CustomerSearchHelper.filterByDisplayName(customersSearchCache!!, query)
            val now = _uiState.value
            if (now is KundenpreiseUiState.KundeSuchen && now.customerSearchQuery == query) {
                _uiState.value = now.copy(customers = filtered)
            }
        }
    }

    fun kundeGewaehlt(customer: Customer) {
        preiseJob?.cancel()
        _isLoadingPreise.value = true
        _uiState.value = KundenpreiseUiState.KundenpreiseList(customer)
        preiseJob = viewModelScope.launch {
            kundenPreiseRepository.getKundenPreiseForCustomerFlow(customer.id).collect {
                _isLoadingPreise.value = false
                _kundenPreiseList.value = it
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun backToKundeSuchen() {
        preiseJob?.cancel()
        _kundenPreiseList.value = emptyList()
        customersSearchCache = null
        _uiState.value = KundenpreiseUiState.KundeSuchen(customerSearchQuery = "", customers = emptyList())
    }
}
