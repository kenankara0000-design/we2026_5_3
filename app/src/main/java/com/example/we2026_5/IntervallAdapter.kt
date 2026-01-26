package com.example.we2026_5

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.databinding.ItemIntervallBinding
import java.text.SimpleDateFormat
import java.util.*

class IntervallAdapter(
    private var intervalle: MutableList<CustomerIntervall>,
    private val onIntervallChanged: (List<CustomerIntervall>) -> Unit,
    private val onDatumSelected: (Int, Boolean) -> Unit // position, isAbholung
) : RecyclerView.Adapter<IntervallAdapter.ViewHolder>() {

    fun updateIntervalle(newIntervalle: List<CustomerIntervall>) {
        intervalle.clear()
        intervalle.addAll(newIntervalle)
        notifyDataSetChanged()
        onIntervallChanged(intervalle.toList())
    }

    fun addIntervall(intervall: CustomerIntervall) {
        intervalle.add(intervall)
        notifyItemInserted(intervalle.size - 1)
        onIntervallChanged(intervalle.toList())
    }

    fun removeIntervall(position: Int) {
        if (position in 0 until intervalle.size) {
            intervalle.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, intervalle.size)
            onIntervallChanged(intervalle.toList())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIntervallBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(intervalle[position], position)
    }

    override fun getItemCount(): Int = intervalle.size

    inner class ViewHolder(
        private val binding: ItemIntervallBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        fun bind(intervall: CustomerIntervall, position: Int) {
            // Abholungsdatum
            if (intervall.abholungDatum > 0) {
                binding.tvAbholungDatum.text = dateFormat.format(Date(intervall.abholungDatum))
            } else {
                binding.tvAbholungDatum.text = "Nicht gesetzt"
            }

            // Auslieferungsdatum
            if (intervall.auslieferungDatum > 0) {
                binding.tvAuslieferungDatum.text = dateFormat.format(Date(intervall.auslieferungDatum))
            } else {
                binding.tvAuslieferungDatum.text = "Nicht gesetzt"
            }

            // Wiederholen-Switch
            binding.switchWiederholen.isChecked = intervall.wiederholen
            binding.etIntervallTage.isEnabled = intervall.wiederholen
            binding.etIntervallTage.setText(intervall.intervallTage.toString())
            
            // Intervall-Anzahl
            binding.layoutIntervallAnzahl.visibility = if (intervall.wiederholen) android.view.View.VISIBLE else android.view.View.GONE
            binding.etIntervallAnzahl.isEnabled = intervall.wiederholen
            binding.etIntervallAnzahl.setText(if (intervall.intervallAnzahl > 0) intervall.intervallAnzahl.toString() else "")

            // Wiederholen-Switch Listener
            binding.switchWiederholen.setOnCheckedChangeListener { _, isChecked ->
                intervalle[position] = intervall.copy(wiederholen = isChecked)
                binding.etIntervallTage.isEnabled = isChecked
                binding.layoutIntervallAnzahl.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
                binding.etIntervallAnzahl.isEnabled = isChecked
                onIntervallChanged(intervalle.toList())
            }

            // Intervall-Tage Listener
            binding.etIntervallTage.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val tage = binding.etIntervallTage.text.toString().toIntOrNull() ?: 7
                    intervalle[position] = intervall.copy(intervallTage = tage.coerceIn(1, 365))
                    onIntervallChanged(intervalle.toList())
                }
            }
            
            // Intervall-Anzahl Listener
            binding.etIntervallAnzahl.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val anzahl = binding.etIntervallAnzahl.text.toString().toIntOrNull() ?: 0
                    intervalle[position] = intervall.copy(intervallAnzahl = anzahl.coerceAtLeast(0))
                    onIntervallChanged(intervalle.toList())
                }
            }

            // Abholungsdatum auswählen
            binding.tvAbholungDatum.setOnClickListener {
                onDatumSelected(position, true)
            }

            // Auslieferungsdatum auswählen
            binding.tvAuslieferungDatum.setOnClickListener {
                onDatumSelected(position, false)
            }

            // Intervall löschen
            binding.btnIntervallLoeschen.setOnClickListener {
                removeIntervall(position)
            }
        }
    }
}
