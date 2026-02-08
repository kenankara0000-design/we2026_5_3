package com.example.we2026_5.ui.wasch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.data.repository.ArticleRepository
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.ErfassungRepository
import com.example.we2026_5.wasch.WaschErfassung
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class BelegeUiState {
    data class KundeSuchen(val customerSearchQuery: String = "", val customers: List<Customer> = emptyList()) : BelegeUiState()
    data class BelegListe(val customer: Customer) : BelegeUiState()
    data class BelegDetail(val customer: Customer, val monthKey: String, val monthLabel: String, val erfassungen: List<WaschErfassung>) : BelegeUiState()
}

class BelegeViewModel(
    private val customerRepository: CustomerRepository,
    private val erfassungRepository: ErfassungRepository,
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BelegeUiState>(BelegeUiState.KundeSuchen())
    val uiState: StateFlow<BelegeUiState> = _uiState.asStateFlow()

    private val _erfassungenList = MutableStateFlow<List<WaschErfassung>>(emptyList())
    val belegMonate: StateFlow<List<BelegMonat>> = _erfassungenList
        .map { BelegMonatGrouping.groupByMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var erfassungenJob: Job? = null

    val articles = articleRepository.getAllArticlesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val customers = customerRepository.getAllCustomers().sortedBy { it.displayName }
            _uiState.value = BelegeUiState.KundeSuchen(customers = customers)
        }
    }

    fun setCustomerSearchQuery(query: String) {
        val s = _uiState.value
        if (s is BelegeUiState.KundeSuchen) _uiState.value = s.copy(customerSearchQuery = query)
    }

    fun kundeGewaehlt(customer: Customer) {
        erfassungenJob?.cancel()
        _uiState.value = BelegeUiState.BelegListe(customer)
        erfassungenJob = viewModelScope.launch {
            erfassungRepository.getErfassungenByCustomerFlow(customer.id).collect {
                _erfassungenList.value = it
            }
        }
    }

    fun openBelegDetail(beleg: BelegMonat) {
        val s = _uiState.value
        if (s is BelegeUiState.BelegListe) {
            _uiState.value = BelegeUiState.BelegDetail(
                customer = s.customer,
                monthKey = beleg.monthKey,
                monthLabel = beleg.monthLabel,
                erfassungen = beleg.erfassungen
            )
        }
    }

    fun backFromBelegDetail() {
        val s = _uiState.value
        if (s is BelegeUiState.BelegDetail) {
            _uiState.value = BelegeUiState.BelegListe(s.customer)
        }
    }

    fun backToKundeSuchen() {
        erfassungenJob?.cancel()
        _erfassungenList.value = emptyList()
        viewModelScope.launch {
            val customers = customerRepository.getAllCustomers().sortedBy { it.displayName }
            _uiState.value = BelegeUiState.KundeSuchen(customerSearchQuery = "", customers = customers)
        }
    }

    fun deleteErfassung(erfassung: WaschErfassung, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val ok = erfassungRepository.deleteErfassung(erfassung.id)
            if (ok) {
                val s = _uiState.value
                if (s is BelegeUiState.BelegDetail) {
                    val customer = customerRepository.getCustomerById(erfassung.customerId)
                    if (customer != null) {
                        _uiState.value = BelegeUiState.BelegListe(customer)
                        erfassungenJob?.cancel()
                        erfassungenJob = viewModelScope.launch {
                            erfassungRepository.getErfassungenByCustomerFlow(customer.id).collect {
                                _erfassungenList.value = it
                            }
                        }
                    }
                }
                onDeleted()
            }
        }
    }
}
