package com.example.we2026_5

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.databinding.ItemCustomerBinding // Achte auf den Namen!
import java.util.concurrent.TimeUnit

class CustomerAdapter(
    private var customers: List<Customer>,
    private val onClick: (Customer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val customer = customers[position]
        holder.binding.tvItemName.text = customer.name
        holder.binding.tvItemAdresse.text = customer.adresse

        // Icons für Abholung/Auslieferung (Punkt 1)
        holder.binding.ivStatusAbholung.alpha = if (customer.abholungErfolgt) 1.0f else 0.2f
        holder.binding.ivStatusAuslieferung.alpha = if (customer.auslieferungErfolgt) 1.0f else 0.2f

        // Logik für Überfälligkeit & Verschiebung (Punkt 5 & 9)
        val heute = System.currentTimeMillis()
        val faelligAm = if (customer.verschobenAufDatum > 0) {
            customer.verschobenAufDatum
        } else {
            customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
        }

        if (heute > faelligAm && (!customer.abholungErfolgt || !customer.auslieferungErfolgt)) {
            holder.binding.tvStatusLabel.visibility = View.VISIBLE
            holder.binding.tvStatusLabel.text = "ÜBERFÄLLIG"
            holder.binding.tvStatusLabel.setBackgroundColor(Color.RED)
            holder.binding.itemContainer.setBackgroundColor(Color.parseColor("#FFF5F5"))
        } else if (customer.verschobenAufDatum > 0) {
            holder.binding.tvStatusLabel.visibility = View.VISIBLE
            holder.binding.tvStatusLabel.text = "VERSCHOBEN"
            holder.binding.tvStatusLabel.setBackgroundColor(Color.BLUE)
            holder.binding.itemContainer.setBackgroundColor(Color.WHITE)
        } else {
            holder.binding.tvStatusLabel.visibility = View.GONE
            holder.binding.itemContainer.setBackgroundColor(Color.WHITE)
        }

        holder.itemView.setOnClickListener { onClick(customer) }
    }

    override fun getItemCount() = customers.size

    // Diese Funktion behebt deine Fehler in den Activities!
    fun updateData(newList: List<Customer>) {
        customers = newList
        notifyDataSetChanged()
    }
}