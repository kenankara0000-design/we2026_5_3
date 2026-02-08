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
import com.example.we2026_5.data.repository.TourOrderRepository
import com.example.we2026_5.tourplanner.TourDataProcessor
import com.example.we2026_5.tourplanner.TourProcessResult
import com.example.we2026_5.ui.tourplanner.ErledigtSheetContent
import com.example.we2026_5.util.CustomerTermFilter
import com.example.we2026_5.util.AgentDebugLog
import com.example.we2026_5.util.Result
import com.example.we2026_5.util.TerminBerechnungUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class TourPlannerViewModel(
    private val repository: CustomerRepository,
    private val listeRepository: KundenListeRepository,
    private val tourOrderRepository: TourOrderRepository
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
            repository.getCustomersForTourFlow().collect { customers ->
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

    // StateFlow für erweiterte Sections – ERLEDIGT standardmäßig eingeklappt, bleibt so bis Nutzer aufmacht
    private val expandedSectionsFlow = MutableStateFlow<Set<SectionType>>(emptySet())

    /** Trigger: bei Änderung der Tour-Reihenfolge neu kombinieren. */
    private val tourOrderUpdateTrigger = MutableStateFlow(0)
    
    // #region agent log
    private val combineEmissionCount = java.util.concurrent.atomic.AtomicInteger(0)
    // #endregion
    // Kombiniere alle Flows: Ergebnis ohne Erledigt-Section in der Liste; Erledigt-Daten für Button/Sheet
    // Debounce (250 ms) auf Kunden/Listen reduziert Pipeline-Läufe bei schnellen Firebase-Updates (Punkt 6.2)
    private val processResultFlow = combine(
        customersFlow.debounce(250L),
        listenFlow.debounce(250L),
        selectedTimestampFlow,
        expandedSectionsFlow,
        tourOrderUpdateTrigger
    ) { customers, listen, timestamp, expandedSections, _ ->
        if (timestamp == null) {
            TourProcessResult(emptyList(), 0, emptyList(), emptyList())
        } else {
            // #region agent log
            val t0 = System.currentTimeMillis()
            AgentDebugLog.log("TourPlannerViewModel.kt", "combine_process_start", mapOf("n" to customers.size, "listen" to listen.size, "ts" to timestamp), "H1")
            // #endregion
            val activeCustomers = CustomerTermFilter.filterActiveForTerms(customers, System.currentTimeMillis())
            val result = dataProcessor.processTourData(activeCustomers, listen, timestamp, expandedSections)
            val order = tourOrderRepository.getOrderForDate(dateKey(timestamp))
            val reorderedItems = applyTourOrder(result.items, order)
            // #region agent log
            val emitNr = combineEmissionCount.incrementAndGet()
            AgentDebugLog.log("TourPlannerViewModel.kt", "combine_process_end", mapOf("duration_ms" to (System.currentTimeMillis() - t0), "items" to result.items.size, "customers" to activeCustomers.size, "emitNr" to emitNr), "H1")
            AgentDebugLog.log("TourPlannerViewModel.kt", "combine_emit", mapOf("emitNr" to emitNr), "H5")
            // #endregion
            result.copy(items = reorderedItems)
        }
    }.distinctUntilChanged().flowOn(Dispatchers.Default)

    val tourItems: LiveData<List<ListItem>> = processResultFlow.map { it.items }.asLiveData()
    val erledigtCount: LiveData<Int> = processResultFlow.map { it.erledigtCount }.asLiveData()
    val erledigtSheetContent: LiveData<ErledigtSheetContent?> = processResultFlow.map { r ->
        ErledigtSheetContent(r.erledigtDoneOhneListen, r.erledigtTourListen)
    }.asLiveData()
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadTourData(selectedTimestamp: Long, isSectionExpanded: (SectionType) -> Boolean) {
        // #region agent log
        AgentDebugLog.log("TourPlannerViewModel.kt", "loadTourData", mapOf("ts" to selectedTimestamp), "H5")
        // #endregion
        selectedTimestampFlow.value = selectedTimestamp
        expandedSectionsFlow.value = expandedSectionsFlow.value
    }

    /** Setzt das anzuzeigende Datum (z. B. beim Start). */
    fun setSelectedTimestamp(ts: Long) {
        selectedTimestampFlow.value = ts
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
        val current = expandedSectionsFlow.value.toMutableSet()
        if (current.contains(sectionType)) {
            current.remove(sectionType)
        } else {
            current.add(sectionType)
        }
        expandedSectionsFlow.value = current
    }

    fun isSectionExpanded(sectionType: SectionType): Boolean =
        expandedSectionsFlow.value.contains(sectionType)
    
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

    /** Stellt einen einzelnen Termin wieder her (entfernt Datum aus geloeschteTermine). */
    suspend fun restoreTerminForCustomer(customer: Customer, terminDatum: Long): Result<Boolean> {
        val terminDatumStart = TerminBerechnungUtils.getStartOfDay(terminDatum)
        val aktuelleGeloeschteTermine = customer.geloeschteTermine.toMutableList().apply {
            remove(terminDatumStart)
        }
        return repository.updateCustomerResult(customer.id, mapOf("geloeschteTermine" to aktuelleGeloeschteTermine))
    }

    // --- Tour-Reihenfolge (Drag & Drop / Route) ---

    private fun dateKey(ts: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ts }
        return String.format("%04d%02d%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    /** Wendet gespeicherte Reihenfolge auf die Liste an (nur CustomerItems umsortiert). */
    private fun applyTourOrder(items: List<ListItem>, order: List<String>): List<ListItem> {
        if (order.isEmpty()) return items
        val customerItems = items.filterIsInstance<ListItem.CustomerItem>()
        if (customerItems.isEmpty()) return items
        val idToItem = customerItems.associateBy { it.customer.id }
        val orderSet = order.toSet()
        val reordered = order.mapNotNull { idToItem[it] } +
            customerItems.filter { it.customer.id !in orderSet }
        var j = 0
        return items.map { item ->
            if (item is ListItem.CustomerItem) reordered[j++] else item
        }
    }

    /**
     * Verschiebt einen Kunden in der Tour-Reihenfolge (Indizes bezogen auf die flache Liste der CustomerItems).
     * Speichert die neue Reihenfolge und löst UI-Update aus.
     */
    fun moveTourOrder(dateMillis: Long, fromIndex: Int, toIndex: Int, currentCustomerIds: List<String>) {
        if (fromIndex == toIndex || fromIndex !in currentCustomerIds.indices || toIndex !in currentCustomerIds.indices) return
        val ids = currentCustomerIds.toMutableList()
        val id = ids.removeAt(fromIndex)
        ids.add(toIndex.coerceIn(0, ids.size), id)
        tourOrderRepository.setOrderForDate(dateKey(dateMillis), ids)
        tourOrderUpdateTrigger.value = tourOrderUpdateTrigger.value + 1
    }

    /** Setzt die komplette Tour-Reihenfolge (z. B. nach Drag & Drop) und löst UI-Update aus. */
    fun setTourOrder(dateMillis: Long, customerIds: List<String>) {
        if (customerIds.isEmpty()) return
        tourOrderRepository.setOrderForDate(dateKey(dateMillis), customerIds)
        tourOrderUpdateTrigger.value = tourOrderUpdateTrigger.value + 1
    }
}
