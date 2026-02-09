package com.example.we2026_5.ui.wasch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.data.repository.ArticleRepository
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenPreiseRepository
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
        if (query.isBlank()) {
            _uiState.value = s.copy(customerSearchQuery = query, customers = emptyList())
            return
        }
        viewModelScope.launch {
            if (customersSearchCache == null) {
                customersSearchCache = customerRepository.getAllCustomers().sortedBy { it.displayName }
            }
            val filtered = customersSearchCache!!.filter {
                it.displayName.contains(query, ignoreCase = true)
            }
            _uiState.value = s.copy(customerSearchQuery = query, customers = filtered)
        }
    }

    fun kundeGewaehlt(customer: Customer) {
        preiseJob?.cancel()
        _uiState.value = KundenpreiseUiState.KundenpreiseList(customer)
        preiseJob = viewModelScope.launch {
            kundenPreiseRepository.getKundenPreiseForCustomerFlow(customer.id).collect {
                _kundenPreiseList.value = it
            }
        }
    }

    fun backToKundeSuchen() {
        preiseJob?.cancel()
        _kundenPreiseList.value = emptyList()
        customersSearchCache = null
        _uiState.value = KundenpreiseUiState.KundeSuchen(customerSearchQuery = "", customers = emptyList())
    }
}
