package com.example.we2026_5.ui.mapview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.util.TerminBerechnungUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_WAYPOINTS = 25

/** UI-State für Kartenansicht. */
sealed class MapViewState {
    object Loading : MapViewState()
    object Empty : MapViewState()
    data class Error(val message: String) : MapViewState()
    data class Success(val addresses: List<String>, val filteredToToday: Boolean) : MapViewState()
}

class MapViewViewModel(
    private val customerRepository: CustomerRepository,
    private val listeRepository: KundenListeRepository
) : ViewModel() {

    private val _state = MutableLiveData<MapViewState>(MapViewState.Loading)
    val state: LiveData<MapViewState> = _state

    fun loadCustomersForMap() {
        viewModelScope.launch {
            _state.value = MapViewState.Loading
            try {
                val allCustomers = withContext(Dispatchers.IO) { customerRepository.getAllCustomers() }
                var customersWithAddress = allCustomers.filter { it.adresse.isNotBlank() }
                var filteredToToday = false
                if (customersWithAddress.size > MAX_WAYPOINTS) {
                    val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
                    customersWithAddress = withContext(Dispatchers.IO) {
                        customersWithAddress.filter { customer ->
                            val liste = if (customer.listeId.isNotBlank())
                                listeRepository.getListeById(customer.listeId) else null
                            TerminBerechnungUtils.hatTerminAmDatum(customer, liste, heuteStart, TerminTyp.ABHOLUNG) ||
                                TerminBerechnungUtils.hatTerminAmDatum(customer, liste, heuteStart, TerminTyp.AUSLIEFERUNG)
                        }
                    }
                    filteredToToday = true
                }
                if (customersWithAddress.isEmpty()) {
                    _state.value = MapViewState.Empty
                    return@launch
                }
                val addresses = customersWithAddress.map { it.adresse }
                _state.value = MapViewState.Success(addresses, filteredToToday)
            } catch (e: Exception) {
                _state.value = MapViewState.Error(e.message ?: "Unbekannter Fehler")
            }
        }
    }
}
