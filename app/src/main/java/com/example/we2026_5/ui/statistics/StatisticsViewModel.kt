package com.example.we2026_5.ui.statistics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.util.AppErrorMapper
import com.example.we2026_5.util.TerminBerechnungUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** UI-State für Statistik-Bildschirm. */
data class StatisticsState(
    val isLoading: Boolean = true,
    val heuteCount: Int = 0,
    val wocheCount: Int = 0,
    val monatCount: Int = 0,
    val overdueCount: Int = 0,
    val doneTodayCount: Int = 0,
    val totalCustomers: Int = 0,
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
            _state.value = _state.value?.copy(isLoading = true, errorMessage = null) ?: StatisticsState(isLoading = true)
            try {
                val result = withContext(Dispatchers.IO) {
                    computeStatistics(repository.getAllCustomers())
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
        val heuteStart = TerminBerechnungUtils.getStartOfDay(heute)
        val cal = Calendar.getInstance()

        val heuteEnd = heuteStart + TimeUnit.DAYS.toMillis(1)
        val heuteCount = allCustomers.count { customer ->
            val faelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum
                else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
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
        val wocheCount = allCustomers.count { customer ->
            val faelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum
                else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
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
        val monatCount = allCustomers.count { customer ->
            val faelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum
                else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
            faelligAm >= monatStart && faelligAm < monatEnd
        }

        val overdueCount = allCustomers.count { customer ->
            val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
            val faelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum
                else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
            !isDone && faelligAm < heuteStart
        }

        val doneTodayCount = allCustomers.count { customer ->
            val abholungHeute = customer.abholungErledigtAm > 0 &&
                TerminBerechnungUtils.getStartOfDay(customer.abholungErledigtAm) == heuteStart
            val auslieferungHeute = customer.auslieferungErledigtAm > 0 &&
                TerminBerechnungUtils.getStartOfDay(customer.auslieferungErledigtAm) == heuteStart
            (abholungHeute && auslieferungHeute) || (customer.keinerWäscheErfolgt &&
                customer.keinerWäscheErledigtAm > 0 && TerminBerechnungUtils.getStartOfDay(customer.keinerWäscheErledigtAm) == heuteStart)
        }

        val totalCustomers = allCustomers.size
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
            quoteHeute = quoteHeute
        )
    }
}
