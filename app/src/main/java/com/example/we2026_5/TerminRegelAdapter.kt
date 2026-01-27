package com.example.we2026_5

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.databinding.ItemTerminRegelBinding

class TerminRegelAdapter(
    private var regeln: List<TerminRegel>,
    private val onRegelClick: (TerminRegel) -> Unit,
    private val onRegelDelete: (TerminRegel) -> Unit
) : RecyclerView.Adapter<TerminRegelAdapter.ViewHolder>() {

    fun updateRegeln(newRegeln: List<TerminRegel>) {
        regeln = newRegeln
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTerminRegelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(regeln[position])
    }

    override fun getItemCount(): Int = regeln.size

    inner class ViewHolder(
        private val binding: ItemTerminRegelBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(regel: TerminRegel) {
            binding.tvRegelName.text = regel.name
            
            if (regel.beschreibung.isNotEmpty()) {
                binding.tvRegelBeschreibung.text = regel.beschreibung
                binding.tvRegelBeschreibung.visibility = View.VISIBLE
            } else {
                binding.tvRegelBeschreibung.visibility = View.GONE
            }
            
            // Abholung anzeigen
            val abholungText = if (regel.abholungDatum > 0) {
                com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(regel.abholungDatum)
            } else {
                "Heute"
            }
            binding.tvAbholung.text = abholungText
            
            // Auslieferung anzeigen
            val auslieferungText = if (regel.auslieferungDatum > 0) {
                com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(regel.auslieferungDatum)
            } else {
                "Heute"
            }
            binding.tvAuslieferung.text = auslieferungText
            
            // Intervall-Info hinzufügen
            val intervallInfo = if (regel.wiederholen) {
                "Alle ${regel.intervallTage} Tage"
            } else {
                "Einmalig"
            }
            
            binding.tvVerwendungsanzahl.text = "${regel.verwendungsanzahl}x verwendet"
            
            binding.root.setOnClickListener {
                onRegelClick(regel)
            }
            
            binding.root.setOnLongClickListener {
                onRegelDelete(regel)
                true
            }
        }
    }
}
