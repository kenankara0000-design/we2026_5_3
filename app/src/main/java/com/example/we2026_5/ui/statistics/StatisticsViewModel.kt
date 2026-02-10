package com.example.we2026_5.ui.statistics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.util.AppErrorMapper
import com.example.we2026_5.util.CustomerTermFilter
import com.example.we2026_5.util.TerminBerechnungUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Schlafmodus: Statistik-Berechnung beim Öffnen deaktiviert (Funktion bleibt vorhanden). */
private const val STATISTICS_SLEEP_MODE = true

/** UI-State für Statistik-Bildschirm. */
data class StatisticsState(
    val isLoading: Boolean = true,
    val sleepMode: Boolean = false,
    val heuteCount: Int = 0,
    val wocheCount: Int = 0,
    val monatCount: Int = 0,
    val overdueCount: Int = 0,
    val doneTodayCount: Int = 0,
    val totalCustomers: Int = 0,
    val regelmaessigCount: Int = 0,
    val unregelmaessigCount: Int = 0,
    val aufAbrufCount: Int = 0,
    val quoteHeute: String = "—",
    val errorMessage: String? = null
)

/**
 * ViewModel für StatisticsActivity.
 * Fällig-/Erledigt-Logik mit TerminBerechnungUtils.getStartOfDay.
 */
class StatisticsViewModel(
    private val repository: CustomerRepository
) : ViewModel() {

    private val _state = MutableLiveData<StatisticsState>(StatisticsState(isLoading = true))
    val state: LiveData<StatisticsState> = _state

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            if (STATISTICS_SLEEP_MODE) {
                _state.value = StatisticsState(isLoading = false, sleepMode = true)
                return@launch
            }
            _state.value = _state.value?.copy(isLoading = true, errorMessage = null) ?: StatisticsState(isLoading = true)
            try {
                // Nur Tour-Kunden für Statistik (Prio 1.1 PLAN_PERFORMANCE_OFFLINE – keine Doppelladung)
                val result = withContext(Dispatchers.IO) {
                    val tourCustomers = repository.getCustomersForTourFlow().first()
                    computeStatistics(tourCustomers)
                }
                _state.value = result.copy(isLoading = false)
            } catch (e: Exception) {
                _state.value = (_state.value ?: StatisticsState()).copy(
                    isLoading = false,
                    errorMessage = AppErrorMapper.toLoadMessage(e)
                )
            }
        }
    }

    private fun computeStatistics(allCustomers: List<Customer>): StatisticsState {
        val heute = System.currentTimeMillis()
        val activeCustomers = CustomerTermFilter.filterActiveForTerms(allCustomers, heute)
        val heuteStart = TerminBerechnungUtils.getStartOfDay(heute)
        val cal = com.example.we2026_5.util.AppTimeZone.newCalendar()

        val heuteEnd = heuteStart + TimeUnit.DAYS.toMillis(1)
        val heuteCount = activeCustomers.count { customer ->
            val faelligAm = TerminBerechnungUtils.naechstesFaelligAmDatum(customer)
            faelligAm >= heuteStart && faelligAm < heuteEnd &&
                !(customer.abholungErfolgt && customer.auslieferungErfolgt)
        }

        cal.timeInMillis = heuteStart
        val wocheStart = cal.apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val wocheEnd = wocheStart + TimeUnit.DAYS.toMillis(7)
        val wocheCount = activeCustomers.count { customer ->
            val faelligAm = TerminBerechnungUtils.naechstesFaelligAmDatum(customer)
            faelligAm >= wocheStart && faelligAm < wocheEnd
        }

        cal.timeInMillis = heuteStart
        val monatStart = cal.apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val monatEnd = cal.timeInMillis
        val monatCount = activeCustomers.count { customer ->
            val faelligAm = TerminBerechnungUtils.naechstesFaelligAmDatum(customer)
            faelligAm >= monatStart && faelligAm < monatEnd
        }

        val overdueCount = activeCustomers.count { customer ->
            val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
            val faelligAm = TerminBerechnungUtils.naechstesFaelligAmDatum(customer)
            !isDone && faelligAm < heuteStart
        }

        val doneTodayCount = activeCustomers.count { customer ->
            val abholungHeute = customer.abholungErledigtAm > 0 &&
                TerminBerechnungUtils.isTimestampInBerlinDay(customer.abholungErledigtAm, heuteStart)
            val auslieferungHeute = customer.auslieferungErledigtAm > 0 &&
                TerminBerechnungUtils.isTimestampInBerlinDay(customer.auslieferungErledigtAm, heuteStart)
            (abholungHeute && auslieferungHeute) || (customer.keinerWäscheErfolgt &&
                customer.keinerWäscheErledigtAm > 0 && TerminBerechnungUtils.isTimestampInBerlinDay(customer.keinerWäscheErledigtAm, heuteStart))
        }

        val totalCustomers = activeCustomers.size
        val regelmaessigCount = activeCustomers.count { it.kundenTyp == com.example.we2026_5.KundenTyp.REGELMAESSIG }
        val unregelmaessigCount = activeCustomers.count { it.kundenTyp == com.example.we2026_5.KundenTyp.UNREGELMAESSIG }
        val aufAbrufCount = activeCustomers.count { it.kundenTyp == com.example.we2026_5.KundenTyp.AUF_ABRUF }
        val faelligHeuteGesamt = heuteCount + doneTodayCount
        val quoteHeute = if (faelligHeuteGesamt > 0)
            (100.0 * doneTodayCount / faelligHeuteGesamt).toInt().toString() + "%"
        else "—"

        return StatisticsState(
            isLoading = false,
            heuteCount = heuteCount,
            wocheCount = wocheCount,
            monatCount = monatCount,
            overdueCount = overdueCount,
            doneTodayCount = doneTodayCount,
            totalCustomers = totalCustomers,
            regelmaessigCount = regelmaessigCount,
            unregelmaessigCount = unregelmaessigCount,
            aufAbrufCount = aufAbrufCount,
            quoteHeute = quoteHeute
        )
    }
}
