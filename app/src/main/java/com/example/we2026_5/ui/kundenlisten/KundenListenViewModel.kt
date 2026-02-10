package com.example.we2026_5.ui.kundenlisten

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ui.common.getWochentagFullResIds
import com.example.we2026_5.R
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.util.CustomerTermFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class KundenListenState {
    object Loading : KundenListenState()
    object Empty : KundenListenState()
    data class Error(val messageResId: Int, val messageArg: String? = null) : KundenListenState()
    data class Success(
        val listen: List<KundenListe>,
        val kundenProListe: Map<String, Int>
    ) : KundenListenState()
}

/**
 * Listen (inkl. Wochentagslisten) werden per Flow aus der DB bezogen; nur bei Änderung liefert
 * Firebase neue Daten. Kein erneutes getAllListen() bei onResume/Refresh – State wird aus Cache abgeleitet.
 */
class KundenListenViewModel(
    private val context: Context,
    private val listeRepository: KundenListeRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow<KundenListenState>(KundenListenState.Loading)
    val state: StateFlow<KundenListenState> = _state.asStateFlow()

    private val _listenCache = MutableStateFlow<List<KundenListe>>(emptyList())
    private val _customersCache = MutableStateFlow<List<Customer>>(emptyList())

    init {
        viewModelScope.launch {
            listeRepository.getAllListenFlow()
                .catch { e -> _state.value = KundenListenState.Error(R.string.error_load_lists, e.message) }
                .flowOn(Dispatchers.IO)
                .collect { _listenCache.value = it }
        }
        viewModelScope.launch {
            customerRepository.getAllCustomersFlow()
                .catch { e -> _state.value = KundenListenState.Error(R.string.error_load_lists, e.message) }
                .flowOn(Dispatchers.IO)
                .collect { _customersCache.value = it }
        }
        viewModelScope.launch {
            combine(_listenCache, _customersCache) { listen, customers ->
                Pair(listen, customers)
            }.collect { (listen, customers) ->
                updateStateFromCache(listen, customers)
            }
        }
    }

    /** Aktualisiert die Anzeige aus dem gecachten Stand (kein DB-Lesen). Bei Pull-to-Refresh / onResume. */
    fun loadListen() {
        _state.value = KundenListenState.Loading
        updateStateFromCache(_listenCache.value, _customersCache.value)
    }

    private fun updateStateFromCache(listen: List<KundenListe>, allCustomers: List<Customer>) {
        viewModelScope.launch {
            try {
                val listToUse = if (listen.any { it.wochentag in 0..6 }) listen else {
                    ensureWochentagListenAndEmit()
                    return@launch
                }
                val now = System.currentTimeMillis()
                val activeCustomers = CustomerTermFilter.filterActiveForTerms(allCustomers, now)
                val sortedListen = sortListen(listToUse)
                val kundenProListe = sortedListen.associate { liste ->
                    val count = if (liste.wochentag in 0..6) {
                        activeCustomers.count { k ->
                            liste.wochentag in k.effectiveAbholungWochentage ||
                            liste.wochentag in k.effectiveAuslieferungWochentage
                        }
                    } else {
                        activeCustomers.count { it.listeId == liste.id }
                    }
                    liste.id to count
                }
                _state.value = if (sortedListen.isEmpty()) KundenListenState.Empty
                else KundenListenState.Success(sortedListen, kundenProListe)
            } catch (e: Exception) {
                _state.value = KundenListenState.Error(R.string.error_load_lists, e.message)
            }
        }
    }

    /** Legt Wochentagslisten in Firebase an, wenn noch keine existieren. Kein getAllListen() – Flow liefert die neuen Daten. */
    private suspend fun ensureWochentagListenAndEmit() {
        val weekdays = getWochentagFullResIds().map { context.getString(it) }
        val created = weekdays.mapIndexed { index, name ->
            KundenListe(
                id = "weekday-$index",
                name = name,
                listeArt = "Tour",
                wochentag = index,
                intervalle = emptyList(),
                erstelltAm = System.currentTimeMillis()
            )
        }
        withContext(Dispatchers.IO) {
            created.forEach { listeRepository.saveListe(it) }
        }
    }

    private fun sortListen(listen: List<KundenListe>): List<KundenListe> {
        val todayIdx = ((com.example.we2026_5.util.AppTimeZone.newCalendar().get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7)
        val (weekdayListen, other) = listen.partition { it.wochentag in 0..6 }
        val sortedWeekdays = weekdayListen.sortedBy { ((it.wochentag - todayIdx + 7) % 7) }
        return sortedWeekdays + other.sortedBy { it.name }
    }
}
