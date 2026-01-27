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
        holder.binding.tvSectionTitle.text = header.title
        holder.binding.tvSectionCount.text = "${header.erledigtCount}/${header.count}"
        
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
            // Finde alle Kunden, die zu diesem Section gehören
            val sectionKunden = mutableListOf<Customer>()
            val headerPosition = items.indexOf(header)
            // Suche nach Kunden-Items nach diesem Header
            for (i in (headerPosition + 1) until items.size) {
                val item = items[i]
                if (item is ListItem.CustomerItem) {
                    // Prüfe ob Kunde zu diesem Section gehört
                    val customer = item.customer
                    val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt
                    val isOverdue = when (header.sectionType) {
                        SectionType.OVERDUE -> {
                            val heuteStart = getStartOfDay(System.currentTimeMillis())
                            val viewDateStart = displayedDateMillis?.let { getStartOfDay(it) } ?: heuteStart
                            val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                                customer = customer,
                                startDatum = heuteStart - TimeUnit.DAYS.toMillis(365),
                                tageVoraus = 730
                            )
                            // WICHTIG: Ein Termin ist nur überfällig, wenn er in der Vergangenheit liegt
                            // UND nicht genau am angezeigten Tag liegt (dann ist er normal fällig)
                            termine.any { termin ->
                                val terminStart = getStartOfDay(termin.datum)
                                val istUeberfaellig = terminStart < heuteStart // Termin liegt in der Vergangenheit
                                // WICHTIG: Wenn der Termin genau am angezeigten Tag liegt, ist er NICHT überfällig
                                if (terminStart == viewDateStart) return@any false
                                istUeberfaellig && com.example.we2026_5.util.TerminFilterUtils.sollUeberfaelligAnzeigen(
                                    terminDatum = termin.datum,
                                    anzeigeDatum = viewDateStart,
                                    aktuellesDatum = heuteStart
                                )
                            }
                        }
                        SectionType.DONE -> isDone
                        else -> false
                    }
                    
                    val belongsToSection = when (header.sectionType) {
                        SectionType.OVERDUE -> isOverdue && !isDone
                        SectionType.DONE -> isDone
                        else -> false
                    }
                    
                    if (belongsToSection) {
                        sectionKunden.add(customer)
                    } else {
                        // Stoppe wenn wir zu einem anderen Section kommen
                        if (item is ListItem.SectionHeader || item is ListItem.ListeHeader) {
                            break
                        }
                    }
                } else if (item is ListItem.SectionHeader || item is ListItem.ListeHeader) {
                    // Stoppe wenn wir zu einem anderen Header kommen
                    break
                }
            }
            
            // Kunden-Views in den Container einfügen
            holder.binding.containerKunden.removeAllViews()
            sectionKunden.forEachIndexed { index, customer ->
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
                        if (index == 0) 0 else 4, // Kleiner Abstand oben (außer beim ersten)
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
    
    /**
     * Bindet einen Liste-Header-ViewHolder
     */
    fun bindListeHeaderViewHolder(
        holder: ListeHeaderViewHolder,
        header: ListItem.ListeHeader
    ) {
        holder.binding.tvSectionTitle.text = header.listeName
        holder.binding.tvSectionCount.text = "${header.erledigtCount}/${header.kundenCount}"
        
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
            // Finde alle Kunden, die zu dieser Liste gehören
            val listeKunden = mutableListOf<Customer>()
            val headerPosition = items.indexOf(header)
            // Suche nach Kunden-Items nach diesem Header
            for (i in (headerPosition + 1) until items.size) {
                val item = items[i]
                if (item is ListItem.CustomerItem && item.customer.listeId == header.listeId) {
                    listeKunden.add(item.customer)
                } else if (item is ListItem.ListeHeader || item is ListItem.SectionHeader) {
                    // Stoppe wenn wir zu einem anderen Header kommen
                    break
                }
            }
            
            // Trenne Kunden in erledigt und nicht erledigt basierend auf der Reihenfolge in items
            // Die Reihenfolge sollte bereits von TourDataProcessor kommen: zuerst nicht erledigte, dann erledigte
            val nichtErledigteKunden = mutableListOf<Customer>()
            val erledigteKundenInListe = mutableListOf<Customer>()
            var hatErledigteGesehen = false
            
            // Respektiere die Reihenfolge aus TourDataProcessor: zuerst nicht erledigte, dann erledigte
            listeKunden.forEach { customer ->
                val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt
                
                // Einfache Prüfung: Wenn Kunde erledigt ist, gehört er zum Erledigt-Bereich
                // Die komplexe Datumslogik wird bereits in TourDataProcessor gemacht
                if (isDone) {
                    erledigteKundenInListe.add(customer)
                    hatErledigteGesehen = true
                } else {
                    // Wenn wir bereits erledigte Kunden gesehen haben, sollte dieser Kunde nicht mehr kommen
                    // (Reihenfolge sollte bereits stimmen - nicht erledigte zuerst)
                    if (!hatErledigteGesehen) {
                        nichtErledigteKunden.add(customer)
                    }
                }
            }
            
            // Kunden-Views in den Container einfügen
            holder.binding.containerKunden.removeAllViews()
            
            // Zuerst nicht erledigte Kunden (Reihenfolge respektieren, aber nach Namen sortieren für bessere Übersicht)
            nichtErledigteKunden.sortedBy { it.name }.forEachIndexed { index, customer ->
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
                        if (index == 0) 0 else 4, // Kleiner Abstand oben (außer beim ersten)
                        layoutParams.rightMargin,
                        4 // Kleiner Abstand unten
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
                    setText("ERLEDIGT")
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(context, com.example.we2026_5.R.color.section_done_text))
                    setPadding(16, 12, 16, 8)
                    gravity = Gravity.CENTER_VERTICAL
                }
                holder.binding.containerKunden.addView(erledigtLabel)
            }
            
            // Dann erledigte Kunden (Reihenfolge respektieren, aber nach Namen sortieren für bessere Übersicht)
            erledigteKundenInListe.sortedBy { it.name }.forEachIndexed { index, customer ->
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
