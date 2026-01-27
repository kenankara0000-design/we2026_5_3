package com.example.we2026_5

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.databinding.ItemWeekDayBinding
import java.util.Calendar

class WeekViewAdapter(
    private var weekData: Map<Int, List<ListItem>>,
    private val context: android.content.Context,
    private val onCustomerClick: (Customer) -> Unit,
    private val customerAdapterFactory: (List<ListItem>) -> CustomerAdapter
) : RecyclerView.Adapter<WeekViewAdapter.DayViewHolder>() {

    private val wochentage = arrayOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
    private var weekStartTimestamp: Long = 0

    inner class DayViewHolder(val binding: ItemWeekDayBinding) : RecyclerView.ViewHolder(binding.root) {
        val dayAdapter: CustomerAdapter = customerAdapterFactory(emptyList())
        
        init {
            binding.rvDayCustomers.layoutManager = LinearLayoutManager(context)
            binding.rvDayCustomers.adapter = dayAdapter
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemWeekDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val dayOffset = position // 0=Montag, 6=Sonntag
        val dayName = wochentage[dayOffset]
        val dayItems = weekData[dayOffset] ?: emptyList()
        
        // Datum berechnen basierend auf weekStartTimestamp
        val cal = Calendar.getInstance().apply {
            timeInMillis = weekStartTimestamp
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }
        val dateStr = com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(cal.timeInMillis)
        
        holder.binding.tvDayHeader.text = "$dayName, $dateStr"
        
        // Kunden-Adapter aktualisieren
        holder.dayAdapter.updateData(dayItems.toMutableList(), cal.timeInMillis)
        
        // Sichtbarkeit: Nur anzeigen wenn Kunden vorhanden
        holder.itemView.visibility = if (dayItems.isNotEmpty()) View.VISIBLE else View.GONE
    }
    
    fun updateWeekData(newWeekData: Map<Int, List<ListItem>>, weekStart: Long) {
        weekData = newWeekData
        weekStartTimestamp = weekStart
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = 7 // 7 Tage der Woche
}
