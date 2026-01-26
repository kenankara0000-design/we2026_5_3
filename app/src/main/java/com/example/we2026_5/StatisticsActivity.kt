package com.example.we2026_5

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.databinding.ActivityStatisticsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import org.koin.android.ext.android.inject

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private val repository: CustomerRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        
        loadStatistics()
    }

    private fun loadStatistics() {
        binding.progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val allCustomers = repository.getAllCustomers()
                val heute = System.currentTimeMillis()
                val cal = Calendar.getInstance()
                
                // Heute
                val heuteStart = getStartOfDay(heute)
                val heuteEnd = heuteStart + TimeUnit.DAYS.toMillis(1)
                val heuteCount = allCustomers.count { customer ->
                    val faelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum 
                                   else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
                    faelligAm >= heuteStart && faelligAm < heuteEnd && 
                    !(customer.abholungErfolgt && customer.auslieferungErfolgt)
                }
                
                // Diese Woche
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
                
                // Dieser Monat
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
                
                // Überfällig
                val overdueCount = allCustomers.count { customer ->
                    val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                    val faelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum 
                                   else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
                    !isDone && faelligAm < heuteStart
                }
                
                // Erledigt heute
                val doneTodayCount = allCustomers.count { customer ->
                    customer.abholungErfolgt && customer.auslieferungErfolgt &&
                    customer.letzterTermin >= heuteStart && customer.letzterTermin < heuteEnd
                }
                
                // Gesamt Kunden
                val totalCustomers = allCustomers.size
                
                // Anzeigen
                binding.tvStatHeute.text = heuteCount.toString()
                binding.tvStatWoche.text = wocheCount.toString()
                binding.tvStatMonat.text = monatCount.toString()
                binding.tvStatOverdue.text = overdueCount.toString()
                binding.tvStatDoneToday.text = doneTodayCount.toString()
                binding.tvStatTotal.text = totalCustomers.toString()
                
                binding.progressBar.visibility = View.GONE
                binding.contentLayout.visibility = View.VISIBLE
                
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.tvError.text = "Fehler: ${e.message}"
                binding.errorLayout.visibility = View.VISIBLE
            }
        }
    }
    
    private fun getStartOfDay(ts: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
