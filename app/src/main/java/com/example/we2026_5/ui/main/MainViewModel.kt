package com.example.we2026_5.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel für MainActivity.
 * Tour-Count (fällig) über Flow; Repository nur über ViewModel.
 */
class MainViewModel(
    private val repository: CustomerRepository,
    private val listeRepository: KundenListeRepository,
    private val tourPlanRepository: TourPlanRepository
) : ViewModel() {

    private val dataProcessor = TourDataProcessor()

    private val _tourFälligCount = MutableLiveData<Int>(0)
    /** Anzahl fälliger/überfälliger Termine für heute (für Tour-Button-Text). */
    val tourFälligCount: LiveData<Int> = _tourFälligCount

    private val _slotVorschlaege = MutableLiveData<List<TerminSlotVorschlag>>(emptyList())
    val slotVorschlaege: LiveData<List<TerminSlotVorschlag>> = _slotVorschlaege

    init {
        // Tour-Count: getCustomersForTourFlow (nur Tour-Kunden, PLAN_TOURPLANNER_PERFORMANCE_3TAGE)
        viewModelScope.launch {
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

        // Slot-Vorschläge: ADHOC-Kunden aus allen Kunden, tageVoraus 14 (PLAN)
        viewModelScope.launch {
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
