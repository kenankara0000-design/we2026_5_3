package com.example.we2026_5

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.databinding.ItemIntervallBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Read-Only Adapter für die Anzeige von ListeIntervallen im View-Mode
 */
class ListeIntervallViewAdapter(
    private var intervalle: List<ListeIntervall>
) : RecyclerView.Adapter<ListeIntervallViewAdapter.ViewHolder>() {

    fun updateIntervalle(newIntervalle: List<ListeIntervall>) {
        intervalle = newIntervalle
        notifyDataSetChanged()
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
        holder.bind(intervalle[position])
    }

    override fun getItemCount(): Int = intervalle.size

    inner class ViewHolder(
        private val binding: ItemIntervallBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        fun bind(intervall: ListeIntervall) {
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

            // Wiederholen-Status anzeigen
            binding.switchWiederholen.isChecked = intervall.wiederholen
            binding.switchWiederholen.isEnabled = false // Read-Only
            
            // Intervall-Tage anzeigen
            binding.etIntervallTage.setText(intervall.intervallTage.toString())
            binding.etIntervallTage.isEnabled = false // Read-Only
            
            // Intervall-Anzahl anzeigen
            if (intervall.wiederholen) {
                binding.layoutIntervallAnzahl.visibility = View.VISIBLE
                binding.etIntervallAnzahl.setText(if (intervall.intervallAnzahl > 0) intervall.intervallAnzahl.toString() else "Unbegrenzt")
                binding.etIntervallAnzahl.isEnabled = false // Read-Only
            } else {
                binding.layoutIntervallAnzahl.visibility = View.GONE
            }
            
            // Löschen-Button ausblenden (Read-Only)
            binding.btnIntervallLoeschen.visibility = View.GONE
            
            // Datum-TextViews nicht klickbar machen (Read-Only)
            binding.tvAbholungDatum.isClickable = false
            binding.tvAuslieferungDatum.isClickable = false
        }
    }
}
