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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ein Beleg in der „Alle Belege“-Liste: Kunde + Monatsbeleg. */
data class BelegEintrag(val customer: Customer, val beleg: BelegMonat)

sealed class BelegeUiState {
    /** Liste aller erfassten Belege (nach Kundenname sortiert); Startansicht. Liste kommt aus alleBelegEintraege-Flow. */
    data class AlleBelege(val nameFilter: String = "") : BelegeUiState()
    /** Kunde suchen: nur Suchfeld, Treffer nur bei Eingabe (keine vollständige Kundenliste). */
    data class KundeSuchen(val customerSearchQuery: String = "", val customers: List<Customer> = emptyList()) : BelegeUiState()
    data class BelegListe(val customer: Customer) : BelegeUiState()
    data class BelegDetail(val customer: Customer, val monthKey: String, val monthLabel: String, val erfassungen: List<WaschErfassung>) : BelegeUiState()
}

class BelegeViewModel(
    private val customerRepository: CustomerRepository,
    private val erfassungRepository: ErfassungRepository,
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BelegeUiState>(BelegeUiState.AlleBelege())
    val uiState: StateFlow<BelegeUiState> = _uiState.asStateFlow()

    /** Kundencache für Namen in Alle Belege und für Kunde-suchen-Treffer. */
    private val _customersCache = MutableStateFlow<Map<String, Customer>>(emptyMap())

    private val _allErfassungen = MutableStateFlow<List<WaschErfassung>>(emptyList())
    /** Alle Belege: aus allen Erfassungen gruppiert (Kunde + Monat), nach Kundenname sortiert. */
    val alleBelegEintraege: StateFlow<List<BelegEintrag>> = combine(_allErfassungen, _customersCache) { erfassungen, cache ->
        if (cache.isEmpty()) return@combine emptyList()
        erfassungen
            .groupBy { it.customerId }
            .flatMap { (customerId, list) ->
                val customer = cache[customerId] ?: return@flatMap emptyList()
                BelegMonatGrouping.groupByMonth(list).map { beleg -> BelegEintrag(customer, beleg) }
            }
            .sortedWith(compareBy<BelegEintrag> { it.customer.displayName }.thenByDescending { it.beleg.monthKey })
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _erfassungenList = MutableStateFlow<List<WaschErfassung>>(emptyList())
    val belegMonate: StateFlow<List<BelegMonat>> = _erfassungenList
        .map { BelegMonatGrouping.groupByMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var erfassungenJob: Job? = null

    val articles = articleRepository.getAllArticlesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val customers = customerRepository.getAllCustomers()
            _customersCache.value = customers.associateBy { it.id }
        }
        viewModelScope.launch {
            erfassungRepository.getAllErfassungenFlow().collect { list ->
                _allErfassungen.value = list
            }
        }
    }

    fun setAlleBelegeNameFilter(filter: String) {
        val s = _uiState.value
        if (s is BelegeUiState.AlleBelege) _uiState.value = s.copy(nameFilter = filter)
    }

    fun setCustomerSearchQuery(query: String) {
        val s = _uiState.value
        if (s is BelegeUiState.KundeSuchen) _uiState.value = s.copy(customerSearchQuery = query)
    }

    /** Wechsel zur Kunde-suchen-Ansicht (Treffer nur bei Eingabe). */
    fun showKundeSuchen() {
        _uiState.value = BelegeUiState.KundeSuchen(
            customerSearchQuery = "",
            customers = _customersCache.value.values.sortedBy { it.displayName }
        )
    }

    /** Zurück von BelegListe zur Alle-Belege-Liste. */
    fun backToAlleBelege() {
        erfassungenJob?.cancel()
        _erfassungenList.value = emptyList()
        val s = _uiState.value
        val filter = (s as? BelegeUiState.AlleBelege)?.nameFilter ?: ""
        _uiState.value = BelegeUiState.AlleBelege(nameFilter = filter)
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

    /** Beleg aus der Alle-Belege-Liste öffnen. */
    fun openBelegDetailFromAlle(eintrag: BelegEintrag) {
        _uiState.value = BelegeUiState.BelegDetail(
            customer = eintrag.customer,
            monthKey = eintrag.beleg.monthKey,
            monthLabel = eintrag.beleg.monthLabel,
            erfassungen = eintrag.beleg.erfassungen
        )
    }

    fun backFromBelegDetail() {
        val s = _uiState.value
        if (s is BelegeUiState.BelegDetail) {
            _uiState.value = BelegeUiState.BelegListe(s.customer)
        }
    }

    /** Zurück zur Alle-Belege-Liste (von KundeSuchen aus). */
    fun backToKundeSuchen() {
        erfassungenJob?.cancel()
        _erfassungenList.value = emptyList()
        val filter = (_uiState.value as? BelegeUiState.AlleBelege)?.nameFilter ?: ""
        _uiState.value = BelegeUiState.AlleBelege(nameFilter = filter)
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

    /** Alle Erfassungen des geöffneten Belegs (Monat) löschen; danach zurück zur Beleg-Liste. */
    fun deleteBeleg(erfassungen: List<WaschErfassung>, onDeleted: () -> Unit) {
        viewModelScope.launch {
            var allOk = true
            for (e in erfassungen) {
                if (!erfassungRepository.deleteErfassung(e.id)) allOk = false
            }
            if (allOk && erfassungen.isNotEmpty()) {
                val s = _uiState.value
                if (s is BelegeUiState.BelegDetail) {
                    erfassungenJob?.cancel()
                    _erfassungenList.value = emptyList()
                    val remaining = erfassungRepository.getErfassungenByCustomer(s.customer.id)
                    _erfassungenList.value = remaining
                    _uiState.value = if (remaining.isEmpty()) {
                        BelegeUiState.AlleBelege()
                    } else {
                        BelegeUiState.BelegListe(s.customer)
                    }
                    if (remaining.isNotEmpty()) {
                        erfassungenJob = viewModelScope.launch {
                            erfassungRepository.getErfassungenByCustomerFlow(s.customer.id).collect {
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
