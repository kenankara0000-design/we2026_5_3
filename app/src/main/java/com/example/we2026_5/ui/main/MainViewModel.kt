package com.example.we2026_5.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.CustomerStatus
import com.example.we2026_5.TerminSlotVorschlag
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.data.repository.TourPlanRepository
import com.example.we2026_5.tourplanner.TourDataProcessor
import com.example.we2026_5.util.CustomerTermFilter
import com.example.we2026_5.util.TerminRegelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel für MainActivity.
 * Tour-Count (fällig) über Flow; Repository nur über ViewModel.
 * Flows werden nur bei Bedarf gesammelt (Prio 1.2 PLAN_PERFORMANCE_OFFLINE), siehe startCollecting/stopCollecting.
 */
class MainViewModel(
    private val repository: CustomerRepository,
    private val listeRepository: KundenListeRepository,
    private val tourPlanRepository: TourPlanRepository,
    private val dataProcessor: TourDataProcessor
) : ViewModel() {

    private val _tourFälligCount = MutableStateFlow(0)
    /** Anzahl fälliger/überfälliger Termine für heute (für Tour-Button-Text). */
    val tourFälligCount: StateFlow<Int> = _tourFälligCount.asStateFlow()

    private val _slotVorschlaege = MutableStateFlow<List<TerminSlotVorschlag>>(emptyList())
    val slotVorschlaege: StateFlow<List<TerminSlotVorschlag>> = _slotVorschlaege.asStateFlow()

    private var collectJob: Job? = null

    /**
     * Startet das Sammeln der Flows (Tour-Count, Slot-Vorschläge). Nur aufrufen, wenn Hauptbildschirm sichtbar ist (z. B. onStart).
     */
    fun startCollecting() {
        if (collectJob?.isActive == true) return
        collectJob = viewModelScope.launch {
            launch {
                combine(
                    repository.getCustomersForTourFlow(),
                    listeRepository.getAllListenFlow()
                ) { customers, listen ->
                    Pair(customers, listen)
                }.collect { (customers, listen) ->
                    val now = System.currentTimeMillis()
                    val activeCustomers = CustomerTermFilter.filterActiveForTerms(customers, now)
                    val count = withContext(Dispatchers.IO) {
                        dataProcessor.getFälligCount(activeCustomers, listen, now)
                    }
                    _tourFälligCount.value = count
                }
            }
            launch {
                combine(
                    repository.getAllCustomersFlow(),
                    tourPlanRepository.getTourSlotsFlow()
                ) { customers, tourSlots ->
                    Pair(customers, tourSlots)
                }.collect { (customers, tourSlots) ->
                    val now = System.currentTimeMillis()
                    val activeCustomers = CustomerTermFilter.filterActiveForTerms(customers, now)
                    val slots = withContext(Dispatchers.Default) {
                        activeCustomers
                            .filter { it.status == CustomerStatus.ADHOC || it.adHocTemplate != null }
                            .flatMap { customer ->
                                TerminRegelManager.schlageSlotsVor(
                                    kunde = customer,
                                    tourSlots = tourSlots,
                                    startDatum = System.currentTimeMillis(),
                                    tageVoraus = 14
                                )
                            }
                            .sortedBy { it.datum }
                            .take(10)
                    }
                    _slotVorschlaege.value = slots
                }
            }
        }
    }

    /**
     * Beendet das Sammeln der Flows. Aufrufen, wenn Hauptbildschirm nicht sichtbar ist (z. B. onStop).
     */
    fun stopCollecting() {
        collectJob?.cancel()
        collectJob = null
    }
}
