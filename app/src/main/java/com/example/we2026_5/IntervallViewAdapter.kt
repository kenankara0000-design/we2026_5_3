package com.example.we2026_5

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.databinding.ItemIntervallBinding

/**
 * Read-Only Adapter für die Anzeige von Intervallen im View-Mode
 * Zeigt nur Regel-Namen an, nicht die Intervall-Details
 */
class IntervallViewAdapter(
    private var intervalle: List<CustomerIntervall>,
    private val onRegelClick: (String) -> Unit // Callback für Regel-Klick (Regel-ID)
) : RecyclerView.Adapter<IntervallViewAdapter.ViewHolder>() {

    fun updateIntervalle(newIntervalle: List<CustomerIntervall>) {
        intervalle = newIntervalle
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIntervallBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onRegelClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(intervalle[position])
    }

    override fun getItemCount(): Int = intervalle.size

    inner class ViewHolder(
        private val binding: ItemIntervallBinding,
        private val onRegelClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(intervall: CustomerIntervall) {
            // Alle Intervall-Details ausblenden
            binding.tvAuslieferungDatum.visibility = View.GONE
            binding.switchWiederholen.visibility = View.GONE
            binding.etIntervallTage.visibility = View.GONE
            binding.layoutIntervallAnzahl.visibility = View.GONE
            binding.btnIntervallLoeschen.visibility = View.GONE
            
            // Labels finden und anpassen/ausblenden
            val rootLayout = binding.root as? android.view.ViewGroup
            rootLayout?.let { layout ->
                for (i in 0 until layout.childCount) {
                    val child = layout.getChildAt(i)
                    if (child is android.view.ViewGroup) {
                        for (j in 0 until child.childCount) {
                            val subChild = child.getChildAt(j)
                            if (subChild is TextView) {
                                val text = subChild.text.toString()
                                when {
                                    text.contains("Abholungsdatum") -> {
                                        // Ändere Label zu "Regel:"
                                        subChild.text = subChild.context.getString(com.example.we2026_5.R.string.intervall_regel_label)
                                        subChild.visibility = View.VISIBLE
                                    }
                                    text.contains("Auslieferungsdatum") -> {
                                        subChild.visibility = View.GONE
                                    }
                                    text.contains("Wiederholen") -> {
                                        subChild.visibility = View.GONE
                                    }
                                    text.contains("Intervall") && !text.contains("Regel") -> {
                                        // "Intervall" Label oben beibehalten
                                        // subChild.visibility = View.VISIBLE
                                    }
                                    text.contains("Anzahl:") -> {
                                        subChild.visibility = View.GONE
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Regel-Anzeige: nur Platzhalter (Regel-Vorlagen entfernt)
            binding.tvAbholungDatum.visibility = View.VISIBLE
            binding.tvAbholungDatum.text = if (intervall.terminRegelId.isNotEmpty()) {
                binding.root.context.getString(com.example.we2026_5.R.string.intervall_rule_not_found)
            } else {
                binding.root.context.getString(com.example.we2026_5.R.string.intervall_manual_created)
            }
            binding.tvAbholungDatum.isClickable = false
            binding.tvAbholungDatum.setTextColor(binding.root.context.getColor(com.example.we2026_5.R.color.text_secondary))
        }
    }
}
