package com.example.we2026_5.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.Customer
import com.example.we2026_5.ListItem
import com.example.we2026_5.SectionType
import com.example.we2026_5.databinding.ItemCustomerBinding
import com.example.we2026_5.databinding.ItemSectionHeaderBinding
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * ViewHolder-Klassen für CustomerAdapter
 */
class CustomerViewHolder(val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root)
class SectionHeaderViewHolder(val binding: ItemSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root)
class ListeHeaderViewHolder(val binding: ItemSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root)

/**
 * Helper-Klasse für Item-Erstellung und Header-Binding in CustomerAdapter
 */
class CustomerItemHelper(
    private val context: Context,
    private val displayedDateMillis: Long?,
    private val items: MutableList<ListItem>,
    private val expandedSections: MutableSet<SectionType>,
    private val expandedListen: MutableSet<String>,
    private val bindCustomerViewHolder: (CustomerViewHolder, Customer) -> Unit,
    private val getStartOfDay: (Long) -> Long
) {
    
    /**
     * Bindet einen Section-Header-ViewHolder
     */
    fun bindSectionHeaderViewHolder(
        holder: SectionHeaderViewHolder,
        header: ListItem.SectionHeader
    ) {
        // Header sollte immer einen Titel haben (wird nicht hinzugefügt wenn leer)
        holder.binding.cardSectionHeader.visibility = View.VISIBLE
        holder.binding.tvSectionTitle.text = header.title
        // Überfällig: „(n)“; ERLEDIGT: „erledigt/count“
        holder.binding.tvSectionCount.text = when (header.sectionType) {
            SectionType.OVERDUE -> "(${header.count})"
            SectionType.DONE -> "${header.erledigtCount}/${header.count}"
            else -> "${header.erledigtCount}/${header.count}"
        }
        
        val isExpanded = expandedSections.contains(header.sectionType)
        // Plus/Minus Symbol: + (eingeklappt) / - (ausgeklappt)
        holder.binding.tvExpandCollapse.text = if (isExpanded) "-" else "+"
        
        // Hintergrund und Textfarbe nach Section-Typ setzen (moderneres Design mit CardView)
        when (header.sectionType) {
            SectionType.OVERDUE -> {
                holder.binding.cardSectionHeader.setCardBackgroundColor(ContextCompat.getColor(context, com.example.we2026_5.R.color.section_overdue_bg))
                holder.binding.tvSectionTitle.setTextColor(ContextCompat.getColor(context, com.example.we2026_5.R.color.section_overdue_text))
                holder.binding.tvExpandCollapse.setTextColor(ContextCompat.getColor(context, com.example.we2026_5.R.color.section_overdue_text))
                holder.binding.tvSectionCount.setTextColor(ContextCompat.getColor(context, com.example.we2026_5.R.color.section_overdue_text))
            }
            SectionType.DONE -> {
                holder.binding.cardSectionHeader.setCardBackgroundColor(ContextCompat.getColor(context, com.example.we2026_5.R.color.section_done_bg))
                holder.binding.tvSectionTitle.setTextColor(ContextCompat.getColor(context, com.example.we2026_5.R.color.section_done_text))
                holder.binding.tvExpandCollapse.setTextColor(ContextCompat.getColor(context, com.example.we2026_5.R.color.section_done_text))
                holder.binding.tvSectionCount.setTextColor(ContextCompat.getColor(context, com.example.we2026_5.R.color.section_done_text))
            }
            SectionType.LISTE -> {
                // Wird nicht für SectionHeader verwendet, nur für ListeHeader
            }
        }
        
        // Kunden in den Container einfügen (nur für Tagesansicht, nicht Wochenansicht)
        if (displayedDateMillis != null && isExpanded) {
            // Kunden direkt aus dem Header holen
            val sectionKunden = header.kunden
            
            // Kunden-Views in den Container einfügen
            holder.binding.containerKunden.removeAllViews()
            // Kunden sind bereits sortiert (aus TourDataProcessor)
            sectionKunden.forEachIndexed { index, customer ->
                val customerBinding = ItemCustomerBinding.inflate(LayoutInflater.from(context))
                val customerHolder = CustomerViewHolder(customerBinding)
                bindCustomerViewHolder(customerHolder, customer)
                
                // Abstand zwischen Kunden anpassen
                val cardView = customerBinding.root as? androidx.cardview.widget.CardView
                if (cardView != null) {
                    val layoutParams = cardView.layoutParams as? ViewGroup.MarginLayoutParams
                        ?: ViewGroup.MarginLayoutParams(
                            ViewGroup.MarginLayoutParams.MATCH_PARENT,
                            ViewGroup.MarginLayoutParams.WRAP_CONTENT
                        )
                    layoutParams.setMargins(
                        layoutParams.leftMargin,
                        if (index == 0) 0 else 8,
                        layoutParams.rightMargin,
                        8
                    )
                    cardView.layoutParams = layoutParams
                }
                
                holder.binding.containerKunden.addView(customerBinding.root)
            }
            holder.binding.containerKunden.visibility = View.VISIBLE
        } else {
            holder.binding.containerKunden.visibility = View.GONE
            holder.binding.containerKunden.removeAllViews()
        }
    }
    
    /**
     * Bindet einen Liste-Header-ViewHolder
     */
    fun bindListeHeaderViewHolder(
        holder: ListeHeaderViewHolder,
        header: ListItem.ListeHeader
    ) {
        holder.binding.tvSectionTitle.text = header.listeName
        holder.binding.tvSectionCount.text = context.getString(com.example.we2026_5.R.string.label_section_count_format, header.erledigtCount, header.kundenCount)
        
        val isExpanded = expandedListen.contains(header.listeId)
        // Plus/Minus Symbol: + (eingeklappt) / - (ausgeklappt)
        holder.binding.tvExpandCollapse.text = if (isExpanded) "-" else "+"
        
        // Design für Liste-Header (andere Farbe als normale Sections)
        holder.binding.cardSectionHeader.setCardBackgroundColor(ContextCompat.getColor(context, com.example.we2026_5.R.color.primary_blue))
        holder.binding.tvSectionTitle.setTextColor(ContextCompat.getColor(context, com.example.we2026_5.R.color.white))
        holder.binding.tvExpandCollapse.setTextColor(ContextCompat.getColor(context, com.example.we2026_5.R.color.white))
        holder.binding.tvSectionCount.setTextColor(ContextCompat.getColor(context, com.example.we2026_5.R.color.white))
        
        // Kunden in den Container einfügen (nur für Tagesansicht, nicht Wochenansicht)
        if (displayedDateMillis != null && isExpanded) {
            // Kunden direkt aus dem Header holen - viel einfacher!
            val nichtErledigteKunden = header.nichtErledigteKunden
            val erledigteKundenInListe = header.erledigteKunden
            
            // Kunden-Views in den Container einfügen
            holder.binding.containerKunden.removeAllViews()
            
            // Zuerst nicht erledigte Kunden (bereits sortiert aus TourDataProcessor)
            nichtErledigteKunden.forEachIndexed { index, customer ->
                val customerBinding = ItemCustomerBinding.inflate(LayoutInflater.from(context))
                val customerHolder = CustomerViewHolder(customerBinding)
                bindCustomerViewHolder(customerHolder, customer)
                
                // Abstand zwischen Kunden anpassen (dichter zusammen, aber etwas Abstand)
                val cardView = customerBinding.root as? androidx.cardview.widget.CardView
                if (cardView != null) {
                    val layoutParams = cardView.layoutParams as? ViewGroup.MarginLayoutParams
                        ?: ViewGroup.MarginLayoutParams(
                            ViewGroup.MarginLayoutParams.MATCH_PARENT,
                            ViewGroup.MarginLayoutParams.WRAP_CONTENT
                        )
                    layoutParams.setMargins(
                        layoutParams.leftMargin,
                        if (index == 0) 0 else 8, // Einheitlicher Abstand oben (außer beim ersten)
                        layoutParams.rightMargin,
                        8 // Einheitlicher Abstand unten
                    )
                    cardView.layoutParams = layoutParams
                }
                
                holder.binding.containerKunden.addView(customerBinding.root)
            }
            
            // Dann Erledigt-Label (wenn es erledigte Kunden gibt)
            // WICHTIG: Label immer anzeigen wenn es erledigte Kunden gibt, auch wenn keine nicht erledigten vorhanden sind
            if (erledigteKundenInListe.isNotEmpty()) {
                // Trennstrich oder Label für Erledigt-Bereich innerhalb der Liste
                val erledigtLabel = TextView(context).apply {
                    setText(context.getString(com.example.we2026_5.R.string.status_erledigt))
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(context, com.example.we2026_5.R.color.section_done_text))
                    setPadding(16, 12, 16, 8)
                    gravity = Gravity.CENTER_VERTICAL
                }
                holder.binding.containerKunden.addView(erledigtLabel)
            }
            
            // Dann erledigte Kunden (bereits sortiert aus TourDataProcessor)
            erledigteKundenInListe.forEachIndexed { index, customer ->
                val customerBinding = ItemCustomerBinding.inflate(LayoutInflater.from(context))
                val customerHolder = CustomerViewHolder(customerBinding)
                bindCustomerViewHolder(customerHolder, customer)
                
                // Abstand zwischen Kunden anpassen (dichter zusammen, aber etwas Abstand)
                val cardView = customerBinding.root as? androidx.cardview.widget.CardView
                if (cardView != null) {
                    val layoutParams = cardView.layoutParams as? ViewGroup.MarginLayoutParams
                        ?: ViewGroup.MarginLayoutParams(
                            ViewGroup.MarginLayoutParams.MATCH_PARENT,
                            ViewGroup.MarginLayoutParams.WRAP_CONTENT
                        )
                    layoutParams.setMargins(
                        layoutParams.leftMargin,
                        if (index == 0 && nichtErledigteKunden.isEmpty()) 0 else 4, // Kleiner Abstand oben
                        layoutParams.rightMargin,
                        4 // Kleiner Abstand unten
                    )
                    cardView.layoutParams = layoutParams
                }
                
                holder.binding.containerKunden.addView(customerBinding.root)
            }
            holder.binding.containerKunden.visibility = View.VISIBLE
        } else {
            holder.binding.containerKunden.visibility = View.GONE
            holder.binding.containerKunden.removeAllViews()
        }
    }
}
