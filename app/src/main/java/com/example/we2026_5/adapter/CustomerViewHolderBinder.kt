package com.example.we2026_5.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import androidx.core.content.ContextCompat
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerAdapter
import com.example.we2026_5.R
import com.example.we2026_5.databinding.ItemCustomerBinding
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.util.TerminInfo
import com.example.we2026_5.ui.CustomerTypeButtonHelper
import java.util.concurrent.TimeUnit

/**
 * Helper-Klasse für das Binding von Customer ViewHolders.
 * Extrahiert die komplexe ViewHolder-Binding-Logik aus CustomerAdapter.
 */
class CustomerViewHolderBinder(
    private val context: Context,
    private val displayedDateMillis: Long?,
    private val pressedButtons: MutableMap<String, String>,
    private val selectedCustomers: MutableSet<String>,
    private val isMultiSelectMode: Boolean,
    private val getAbholungDatum: ((Customer) -> Long)?,
    private val getAuslieferungDatum: ((Customer) -> Long)?,
    private val getNaechstesTourDatum: ((Customer) -> Long)?,
    private val getTermineFuerKunde: ((Customer, Long, Int) -> List<com.example.we2026_5.util.TerminInfo>)?,
    private val onTerminClick: ((Customer, Long) -> Unit)?,
    private val onClick: (Customer) -> Unit,
    private val dialogHelper: CustomerDialogHelper,
    private val onAbholung: (Customer) -> Unit,
    private val onAuslieferung: (Customer) -> Unit,
    private val onKw: ((Customer) -> Unit)?,
    private val enableMultiSelectMode: () -> Unit,
    private val toggleCustomerSelection: (String, CustomerViewHolder) -> Unit
) {

    private val getAlleTermineLambda: (Customer, Long, Int) -> List<TerminInfo> = { customer, startDatum, tageVoraus ->
        getTermineFuerKunde?.invoke(customer, startDatum, tageVoraus)
            ?: TerminBerechnungUtils.berechneAlleTermineFuerKunde(customer, null, startDatum, tageVoraus)
    }

    private val buttonVisibilityHelper = CustomerButtonVisibilityHelper(
        context = context,
        displayedDateMillis = displayedDateMillis,
        pressedButtons = pressedButtons,
        getAbholungDatum = getAbholungDatum,
        getAuslieferungDatum = getAuslieferungDatum,
        getAlleTermine = getAlleTermineLambda
    )

    /** Termine für Kunde (nutzt Listen-Termin-Regel bei Listen-Kunden). */
    private fun getAlleTermine(customer: Customer, startDatum: Long, tageVoraus: Int): List<com.example.we2026_5.util.TerminInfo> =
        getTermineFuerKunde?.invoke(customer, startDatum, tageVoraus)
            ?: TerminBerechnungUtils.berechneAlleTermineFuerKunde(customer, null, startDatum, tageVoraus)
    
    fun bind(holder: CustomerViewHolder, customer: Customer) {
        setupBasicInfo(holder, customer)
        setupCustomerTypeButton(holder, customer)
        setupNavigation(holder, customer)
        resetStyles(holder)
        
        setupClickListeners(holder, customer)
        applyMultiSelectStyles(holder, customer)
        
        // Status-Styles ZULETZT anwenden (nach MultiSelect, damit sie nicht überschrieben werden)
        if (displayedDateMillis != null) {
            applyStatusStyles(holder, customer)
        } else {
            holder.binding.tvStatusLabel.visibility = View.GONE
        }
        if (displayedDateMillis != null) {
            val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
            val viewDateStart = TerminBerechnungUtils.getStartOfDay(displayedDateMillis!!)
            CompletionHintsHelper.apply(holder.binding, customer, viewDateStart, heuteStart)
        } else {
            holder.binding.tvErledigungsHinweise.visibility = View.GONE
            holder.binding.tvUeberfaelligIndikator.visibility = View.GONE
        }
        buttonVisibilityHelper.apply(holder.binding, customer)
    }
    
    private fun setupBasicInfo(holder: CustomerViewHolder, customer: Customer) {
        holder.binding.tvItemName.text = customer.name
        holder.binding.tvItemAdresse.text = customer.adresse
        
        // Telefon anzeigen (wenn vorhanden)
        if (customer.telefon.isNotBlank()) {
            holder.binding.tvItemTelefon.text = "📞 ${customer.telefon}"
            holder.binding.tvItemTelefon.visibility = View.VISIBLE
        } else {
            holder.binding.tvItemTelefon.visibility = View.GONE
        }
        
        // Notizen anzeigen (wenn vorhanden)
        if (customer.notizen.isNotBlank()) {
            holder.binding.tvItemNotizen.text = "📝 ${customer.notizen}"
            holder.binding.tvItemNotizen.visibility = View.VISIBLE
        } else {
            holder.binding.tvItemNotizen.visibility = View.GONE
        }
        
        // Nächstes Tour-Datum berechnen und anzeigen (Listen-Kunden: Termin-Regel der Liste)
        val naechsteTour = getNaechstesTourDatum?.invoke(customer) ?: customer.getFaelligAm()
        if (naechsteTour > 0) {
            val dateStr = DateFormatter.formatDate(naechsteTour)
            holder.binding.tvNextTour.text = "Nächste Tour: $dateStr"
            holder.binding.tvNextTour.visibility = View.VISIBLE
        } else {
            holder.binding.tvNextTour.text = "Nächste Tour: Kein Termin"
            holder.binding.tvNextTour.visibility = View.VISIBLE
        }
    }
    
    private fun setupCustomerTypeButton(holder: CustomerViewHolder, customer: Customer) {
        CustomerTypeButtonHelper.setupButton(holder.binding.btnKundenTyp, customer, context)
    }
    
    private fun setupNavigation(holder: CustomerViewHolder, customer: Customer) {
        holder.binding.btnNavigation.visibility = if (customer.adresse.isNotBlank()) View.VISIBLE else View.GONE
        // Navigation-Listener wird in CustomerAdapter gesetzt (benötigt startNavigation Funktion)
    }
    
    private fun setupClickListeners(holder: CustomerViewHolder, customer: Customer) {
        // Button-Handler - nur setzen wenn im TourPlanner (displayedDateMillis != null)
        if (displayedDateMillis != null) {
            val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
            val viewDateStart = TerminBerechnungUtils.getStartOfDay(displayedDateMillis!!)
            val istHeute = viewDateStart == heuteStart
            
            holder.binding.btnAbholung.setOnClickListener {
                if (!istHeute) {
                    android.widget.Toast.makeText(context, "Termine können nur am Tag Heute erledigt werden.", android.widget.Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                pressedButtons[customer.id] = "A"
                onAbholung(customer)
            }
            holder.binding.btnAuslieferung.setOnClickListener {
                if (!istHeute) {
                    android.widget.Toast.makeText(context, "Termine können nur am Tag Heute erledigt werden.", android.widget.Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                pressedButtons[customer.id] = "L"
                onAuslieferung(customer)
            }
            holder.binding.btnKw.setOnClickListener {
                if (!istHeute) {
                    android.widget.Toast.makeText(context, "Termine können nur am Tag Heute erledigt werden.", android.widget.Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                pressedButtons[customer.id] = "KW"
                onKw?.invoke(customer)
            }
            holder.binding.btnVerschieben.setOnClickListener {
                pressedButtons[customer.id] = "V"
                dialogHelper.showVerschiebenDialog(customer)
            }
            holder.binding.btnUrlaub.setOnClickListener {
                pressedButtons[customer.id] = "U"
                dialogHelper.showUrlaubDialog(customer)
            }
            holder.binding.btnRueckgaengig.setOnClickListener {
                dialogHelper.showRueckgaengigDialog(customer)
            }
        } else {
            // Im CustomerManager: Click-Listener entfernen, damit sie keine Clicks abfangen
            holder.binding.btnAbholung.setOnClickListener(null)
            holder.binding.btnAuslieferung.setOnClickListener(null)
            holder.binding.btnKw.setOnClickListener(null)
            holder.binding.btnVerschieben.setOnClickListener(null)
            holder.binding.btnUrlaub.setOnClickListener(null)
            holder.binding.btnRueckgaengig.setOnClickListener(null)
        }
        
        // Termin-Klick: Wenn im TourPlanner, öffne Termin-Detail-Dialog
        if (displayedDateMillis != null) {
            holder.itemView.setOnClickListener {
                if (isMultiSelectMode) {
                    toggleCustomerSelection(customer.id, holder)
                } else {
                    val terminDatum = if (customer.verschobenAufDatum > 0) {
                        customer.verschobenAufDatum
                    } else {
                        customer.getFaelligAm()
                    }
                    onTerminClick?.invoke(customer, terminDatum)
                }
            }
        } else {
            // Click-Listener auf itemView UND itemContainer setzen, um sicherzustellen, dass Clicks funktionieren
            val clickListener = View.OnClickListener {
                android.util.Log.d("CustomerViewHolderBinder", "Item clicked: ${customer.name}, MultiSelect: $isMultiSelectMode")
                if (isMultiSelectMode) {
                    toggleCustomerSelection(customer.id, holder)
                } else {
                    android.util.Log.d("CustomerViewHolderBinder", "Calling onClick for customer: ${customer.name}")
                    onClick(customer)
                }
            }
            
            // Click-Listener auf alle relevanten Views setzen
            holder.itemView.setOnClickListener(clickListener)
            holder.binding.itemContainer.setOnClickListener(clickListener)
            holder.binding.tvItemName.setOnClickListener(clickListener)
            holder.binding.tvItemAdresse.setOnClickListener(clickListener)
            holder.binding.tvItemTelefon.setOnClickListener(clickListener)
            holder.binding.tvItemNotizen.setOnClickListener(clickListener)
            holder.binding.tvNextTour.setOnClickListener(clickListener)
            
            // Wichtig: Views müssen clickable sein, damit der Listener funktioniert
            holder.itemView.isClickable = true
            holder.itemView.isFocusable = true
            holder.binding.itemContainer.isClickable = true
            holder.binding.itemContainer.isFocusable = true
            holder.binding.tvItemName.isClickable = true
            holder.binding.tvItemAdresse.isClickable = true
            holder.binding.tvItemTelefon.isClickable = true
            holder.binding.tvItemNotizen.isClickable = true
            holder.binding.tvNextTour.isClickable = true
            
            // WICHTIG: Button-Container im CustomerManager nicht klickbar machen, damit Clicks durchgehen
            if (displayedDateMillis == null) {
                holder.binding.buttonContainer.isClickable = false
                holder.binding.buttonContainer.isFocusable = false
            }
        }
        
        // Long-Press für Multi-Select aktivieren
        val longClickListener = View.OnLongClickListener {
            if (!isMultiSelectMode) {
                enableMultiSelectMode()
                toggleCustomerSelection(customer.id, holder)
            }
            true
        }
        holder.itemView.setOnLongClickListener(longClickListener)
        holder.binding.itemContainer.setOnLongClickListener(longClickListener)
    }
    
    private fun applyMultiSelectStyles(holder: CustomerViewHolder, customer: Customer) {
        if (isMultiSelectMode) {
            holder.binding.itemContainer.alpha = if (selectedCustomers.contains(customer.id)) 0.7f else 1.0f
            holder.binding.itemContainer.setBackgroundColor(
                if (selectedCustomers.contains(customer.id))
                    ContextCompat.getColor(context, R.color.primary_blue_light)
                else
                    Color.WHITE
            )
        } else {
            holder.binding.itemContainer.alpha = 1.0f
            resetStyles(holder)
        }
    }
    
    private fun resetStyles(holder: CustomerViewHolder) {
        holder.binding.itemContainer.alpha = 1.0f
        holder.binding.tvItemName.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        holder.binding.tvItemName.setTypeface(null, Typeface.NORMAL)
        holder.binding.itemContainer.setBackgroundColor(Color.TRANSPARENT)
        holder.binding.root.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_white))
    }
    
    private fun applyStatusStyles(holder: CustomerViewHolder, customer: Customer) {
        holder.binding.tvStatusLabel.visibility = View.VISIBLE
        holder.binding.tvStatusLabel.setTextColor(Color.WHITE)
        
        val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
        val viewDateStart = displayedDateMillis?.let {
            TerminBerechnungUtils.getStartOfDay(it)
        } ?: heuteStart
        
        if (viewDateStart > heuteStart) {
            holder.binding.tvStatusLabel.visibility = View.GONE
            return
        }
        
        val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt
        
        // Prüfe ob beide A und L heute relevant sind (für vollständige Erledigung)
        val hatAbholungRelevantAmTag = run {
            val alleTermine = getAlleTermineLambda(customer, viewDateStart - TimeUnit.DAYS.toMillis(365), 730)
            val hatAbholungAmTag = alleTermine.any { 
                TerminBerechnungUtils.getStartOfDay(it.datum) == viewDateStart && it.typ == com.example.we2026_5.TerminTyp.ABHOLUNG
            }
            val hatUeberfaelligeAbholung = alleTermine.any { termin ->
                val terminStart = TerminBerechnungUtils.getStartOfDay(termin.datum)
                val istAbholung = termin.typ == com.example.we2026_5.TerminTyp.ABHOLUNG
                val istNichtErledigt = !customer.abholungErfolgt
                val istAmTagXFaellig = terminStart == viewDateStart
                val warVorHeuteFaellig = terminStart < heuteStart
                val istHeute = viewDateStart == heuteStart
                val istHeuteFaellig = terminStart == heuteStart
                val istUeberfaellig = (istAmTagXFaellig || (warVorHeuteFaellig && istHeute)) && !istHeuteFaellig
                istAbholung && istNichtErledigt && istUeberfaellig
            }
            hatAbholungAmTag || hatUeberfaelligeAbholung
        }
        
        val hatAuslieferungRelevantAmTag = run {
            val alleTermine = getAlleTermineLambda(customer, viewDateStart - TimeUnit.DAYS.toMillis(365), 730)
            val hatAuslieferungAmTag = alleTermine.any { 
                TerminBerechnungUtils.getStartOfDay(it.datum) == viewDateStart && it.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG
            }
            val hatUeberfaelligeAuslieferung = alleTermine.any { termin ->
                val terminStart = TerminBerechnungUtils.getStartOfDay(termin.datum)
                val istAuslieferung = termin.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG
                val istNichtErledigt = !customer.auslieferungErfolgt
                val istAmTagXFaellig = terminStart == viewDateStart
                val warVorHeuteFaellig = terminStart < heuteStart
                val istHeute = viewDateStart == heuteStart
                val istHeuteFaellig = terminStart == heuteStart
                val istUeberfaellig = (istAmTagXFaellig || (warVorHeuteFaellig && istHeute)) && !istHeuteFaellig
                istAuslieferung && istNichtErledigt && istUeberfaellig
            }
            hatAuslieferungAmTag || hatUeberfaelligeAuslieferung
        }
        
        val beideRelevantAmTag = hatAbholungRelevantAmTag && hatAuslieferungRelevantAmTag
        
        val kwErledigtAmTagStatus = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.keinerWäscheErledigtAm) == viewDateStart
        // "ERLEDIGT" Badge: Wenn beide A und L relevant, nur anzeigen wenn beide erledigt; KW am Tag zählt als erledigt
        val sollAlsErledigtAnzeigen = if (beideRelevantAmTag) {
            customer.abholungErfolgt && customer.auslieferungErfolgt
        } else {
            isDone || kwErledigtAmTagStatus
        }
        
        // Gleiche Logik wie in CustomerButtonVisibilityHelper verwenden
        val istUeberfaellig = run {
            val alleTermine = getAlleTermineLambda(customer, viewDateStart - TimeUnit.DAYS.toMillis(365), 730)
            alleTermine.any { termin ->
                val terminStart = TerminBerechnungUtils.getStartOfDay(termin.datum)
                val istAmTagXFaellig = terminStart == viewDateStart
                val warVorHeuteFaellig = terminStart < heuteStart
                val istHeute = viewDateStart == heuteStart
                val istHeuteFaellig = terminStart == heuteStart
                val istUeberfaellig = (istAmTagXFaellig || (warVorHeuteFaellig && istHeute)) && !istHeuteFaellig
                val istNichtErledigt = (termin.typ == com.example.we2026_5.TerminTyp.ABHOLUNG && !customer.abholungErfolgt) ||
                        (termin.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG && !customer.auslieferungErfolgt)
                istUeberfaellig && istNichtErledigt
            }
        }
        
        val showAsOverdue = istUeberfaellig && !sollAlsErledigtAnzeigen
        val cardView = holder.itemView as? androidx.cardview.widget.CardView
        
        when {
            sollAlsErledigtAnzeigen -> {
                holder.binding.tvStatusLabel.text = "ERLEDIGT"
                holder.binding.tvStatusLabel.setBackgroundResource(R.drawable.status_badge_done)
                holder.binding.tvStatusLabel.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
                holder.binding.itemContainer.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.customer_done_bg))
                holder.binding.itemContainer.alpha = 0.8f
                cardView?.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.customer_done_bg))
            }
            showAsOverdue -> {
                holder.binding.tvStatusLabel.visibility = View.GONE
                holder.binding.tvItemName.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.status_overdue))
                holder.binding.tvItemName.setTypeface(null, Typeface.BOLD)
                cardView?.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.customer_overdue_bg))
            }
            customer.verschobenAufDatum > 0 -> {
                holder.binding.tvStatusLabel.text = "VERSCHOBEN"
                holder.binding.tvStatusLabel.setBackgroundResource(R.drawable.status_badge_verschoben)
                holder.binding.tvStatusLabel.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
                cardView?.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.surface_white))
            }
            else -> {
                holder.binding.tvStatusLabel.visibility = View.GONE
                // Gleiche Karten-Optik für Gewerblich und Privat im Tourenplaner
                if ((customer.kundenArt == "Gewerblich" || customer.kundenArt == "Privat") && displayedDateMillis != null) {
                    cardView?.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.customer_gewerblich_bg))
                } else {
                    cardView?.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.surface_white))
                }
            }
        }
    }
}
