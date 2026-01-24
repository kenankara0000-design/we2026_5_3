package com.example.we2026_5

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.databinding.ItemCustomerBinding
import java.util.Calendar
import java.util.concurrent.TimeUnit

class CustomerAdapter(
    private var customers: List<Customer>,
    private val onClick: (Customer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.ViewHolder>() {

    private var displayedDateMillis: Long? = null

    inner class ViewHolder(val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val customer = customers[position]
        holder.binding.tvItemName.text = customer.name
        holder.binding.tvItemAdresse.text = customer.adresse

        resetStyles(holder)

        // Wenn ein Datum gesetzt ist (vom Touren-Planer), zeige den Status an.
        if (displayedDateMillis != null) {
            applyStatusStyles(holder, customer)
        } else {
            // Sonst (in der Kundenliste), blende Status-Elemente aus.
            holder.binding.ivStatusAbholung.visibility = View.GONE
            holder.binding.ivStatusAuslieferung.visibility = View.GONE
            holder.binding.tvStatusLabel.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(customer) }
    }

    private fun resetStyles(holder: ViewHolder) {
        holder.binding.itemContainer.alpha = 1.0f
        holder.binding.tvItemName.setTextColor(Color.BLACK)
        holder.binding.tvItemName.setTypeface(null, Typeface.NORMAL)
        holder.binding.itemContainer.setBackgroundColor(Color.WHITE)
    }

    private fun applyStatusStyles(holder: ViewHolder, customer: Customer) {
        holder.binding.ivStatusAbholung.visibility = View.VISIBLE
        holder.binding.ivStatusAuslieferung.visibility = View.VISIBLE
        holder.binding.tvStatusLabel.visibility = View.VISIBLE
        holder.binding.tvStatusLabel.setTextColor(Color.WHITE)

        holder.binding.ivStatusAbholung.alpha = if (customer.abholungErfolgt) 1.0f else 0.3f
        holder.binding.ivStatusAuslieferung.alpha = if (customer.auslieferungErfolgt) 1.0f else 0.3f

        val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
        val faelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())

        val heuteStart = getStartOfDay(System.currentTimeMillis())
        val displayedDateStart = displayedDateMillis ?: 0
        val isActuallyOverdue = faelligAm < displayedDateStart
        val isViewDateInFuture = displayedDateStart > heuteStart
        val showAsOverdue = isActuallyOverdue && !isViewDateInFuture && !isDone

        when {
            isDone -> {
                holder.binding.tvStatusLabel.text = "ERLEDIGT"
                holder.binding.tvStatusLabel.setBackgroundColor(Color.GRAY)
                holder.binding.itemContainer.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.light_gray))
                holder.binding.itemContainer.alpha = 0.6f
            }
            showAsOverdue -> {
                holder.binding.tvStatusLabel.text = "ÜBERFÄLLIG"
                holder.binding.tvStatusLabel.setBackgroundColor(Color.TRANSPARENT)
                holder.binding.tvStatusLabel.setTextColor(Color.RED)
                holder.binding.tvItemName.setTextColor(Color.RED)
                holder.binding.tvItemName.setTypeface(null, Typeface.BOLD)
            }
            customer.verschobenAufDatum > 0 -> {
                holder.binding.tvStatusLabel.text = "VERSCHOBEN"
                holder.binding.tvStatusLabel.setBackgroundColor(Color.BLUE)
            }
            else -> holder.binding.tvStatusLabel.visibility = View.GONE
        }
    }

    override fun getItemCount() = customers.size

    fun updateData(newList: List<Customer>, date: Long? = null) {
        customers = newList
        displayedDateMillis = date
        notifyDataSetChanged()
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
