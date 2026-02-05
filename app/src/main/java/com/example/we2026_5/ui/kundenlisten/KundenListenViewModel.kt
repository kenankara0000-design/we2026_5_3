package com.example.we2026_5.ui.kundenlisten

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ui.common.getWochentagFullResIds
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.R
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class KundenListenViewModel(
    private val context: Context,
    private val listeRepository: KundenListeRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow<KundenListenState>(KundenListenState.Loading)
    val state: StateFlow<KundenListenState> = _state.asStateFlow()

    fun loadListen() {
        viewModelScope.launch {
            _state.value = KundenListenState.Loading
            try {
                val listen = withContext(Dispatchers.IO) { listeRepository.getAllListen() }.toMutableList()
                val allCustomers = withContext(Dispatchers.IO) { customerRepository.getAllCustomers() }
                ensureWochentagListen(listen)
                val sortedListen = sortListen(listen)
                val kundenProListe = sortedListen.associate { liste ->
                    val count = if (liste.wochentag in 0..6) {
                        allCustomers.count { k ->
                            k.defaultAbholungWochentag == liste.wochentag ||
                            k.defaultAuslieferungWochentag == liste.wochentag
                        }
                    } else {
                        allCustomers.count { it.listeId == liste.id }
                    }
                    liste.id to count
                }
                if (listen.isEmpty()) {
                    _state.value = KundenListenState.Empty
                } else {
                    _state.value = KundenListenState.Success(sortedListen, kundenProListe)
                }
            } catch (e: Exception) {
                _state.value = KundenListenState.Error(R.string.error_load_lists, e.message)
            }
        }
    }

    private suspend fun ensureWochentagListen(listen: MutableList<KundenListe>) {
        val hasWeekday = listen.any { it.wochentag in 0..6 }
        if (hasWeekday) return
        val weekdays = getWochentagFullResIds().map { context.getString(it) }
        val created = weekdays.mapIndexed { index, name ->
            KundenListe(
                id = "weekday-$index",
                name = name,
                listeArt = "Liste",
                wochentag = index,
                intervalle = emptyList(),
                erstelltAm = System.currentTimeMillis()
            )
        }
        withContext(Dispatchers.IO) {
            created.forEach { listeRepository.saveListe(it) }
        }
        listen.clear()
        listen.addAll(withContext(Dispatchers.IO) { listeRepository.getAllListen() })
    }

    private fun sortListen(listen: List<KundenListe>): List<KundenListe> {
        val todayIdx = ((java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7)
        val (weekdayListen, other) = listen.partition { it.wochentag in 0..6 }
        val sortedWeekdays = weekdayListen.sortedBy { ((it.wochentag - todayIdx + 7) % 7) }
        return sortedWeekdays + other.sortedBy { it.name }
    }
}
