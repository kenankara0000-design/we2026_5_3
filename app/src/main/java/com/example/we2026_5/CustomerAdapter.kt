package com.example.we2026_5

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.databinding.ItemCustomerBinding
import com.example.we2026_5.databinding.ItemSectionHeaderBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

sealed class ListItem {
    data class CustomerItem(val customer: Customer) : ListItem()
    data class SectionHeader(val title: String, val count: Int, val sectionType: SectionType) : ListItem()
    data class ListeHeader(val listeName: String, val kundenCount: Int, val listeId: String) : ListItem()
}

enum class SectionType {
    OVERDUE, DONE, LISTE
}

class CustomerAdapter(
    private var items: MutableList<ListItem>,
    private val context: Context,
    private val onClick: (Customer) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var displayedDateMillis: Long? = null
    private var expandedSections = mutableSetOf<SectionType>() // Standardmäßig eingeklappt
    var onSectionToggle: ((SectionType) -> Unit)? = null
    
    // Multi-Select für Bulk-Operationen
    private var isMultiSelectMode = false
    private val selectedCustomers = mutableSetOf<String>() // Customer IDs
    
    // Callbacks für Firebase-Operationen (statt direkter Firebase-Aufrufe)
    var onAbholung: ((Customer) -> Unit)? = null
    var onAuslieferung: ((Customer) -> Unit)? = null
    var onResetTourCycle: ((String) -> Unit)? = null
    var onVerschieben: ((Customer, Long, Boolean) -> Unit)? = null // customer, newDate, alleVerschieben
    var onUrlaub: ((Customer, Long, Long) -> Unit)? = null // customer, von, bis
    var onRueckgaengig: ((Customer) -> Unit)? = null
    var onBulkMarkDone: ((List<Customer>) -> Unit)? = null // Für Bulk-Operationen
    
    // Drag & Drop Support
    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        // Nur Kunden-Items verschieben, keine Section Headers
        val fromItem = items[fromPosition]
        val toItem = items[toPosition]
        
        // Prüfen ob beide Items Kunden sind (keine Section Headers)
        if (fromItem !is ListItem.CustomerItem || toItem !is ListItem.CustomerItem) {
            return false
        }
        
        // Items tauschen
        items.removeAt(fromPosition)
        items.add(toPosition, fromItem)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    companion object {
        private const val VIEW_TYPE_CUSTOMER = 0
        private const val VIEW_TYPE_SECTION_HEADER = 1
        private const val VIEW_TYPE_LISTE_HEADER = 2
    }
    
    private var expandedListen = mutableSetOf<String>() // Liste IDs die expanded sind

    inner class CustomerViewHolder(val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root)
    inner class SectionHeaderViewHolder(val binding: ItemSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    inner class ListeHeaderViewHolder(val binding: ItemSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.CustomerItem -> VIEW_TYPE_CUSTOMER
            is ListItem.SectionHeader -> VIEW_TYPE_SECTION_HEADER
            is ListItem.ListeHeader -> VIEW_TYPE_LISTE_HEADER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CUSTOMER -> {
                val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                CustomerViewHolder(binding)
            }
            VIEW_TYPE_SECTION_HEADER -> {
                val binding = ItemSectionHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SectionHeaderViewHolder(binding)
            }
            VIEW_TYPE_LISTE_HEADER -> {
                val binding = ItemSectionHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ListeHeaderViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.CustomerItem -> {
                // Prüfe ob Kunde zu einer Liste gehört
                val prevItem = if (position > 0) items[position - 1] else null
                val isInListe = prevItem is ListItem.ListeHeader && item.customer.listeId == prevItem.listeId
                
                if (isInListe) {
                    // Kunde gehört zu einer Liste: nur anzeigen wenn Liste expanded ist
                    val listeHeader = prevItem as ListItem.ListeHeader
                    if (expandedListen.contains(listeHeader.listeId)) {
                        bindCustomerViewHolder(holder as CustomerViewHolder, item.customer)
                        holder.itemView.visibility = View.VISIBLE
                    } else {
                        holder.itemView.visibility = View.GONE
                    }
                } else {
                    // Gewerblich-Kunde oder nicht in Liste: normale Logik
                    if (shouldShowCustomer(item.customer)) {
                        bindCustomerViewHolder(holder as CustomerViewHolder, item.customer)
                        holder.itemView.visibility = View.VISIBLE
                    } else {
                        holder.itemView.visibility = View.GONE
                    }
                }
            }
            is ListItem.ListeHeader -> {
                bindListeHeaderViewHolder(holder as ListeHeaderViewHolder, item)
            }
            is ListItem.SectionHeader -> bindSectionHeaderViewHolder(holder as SectionHeaderViewHolder, item)
        }
    }

    private fun bindSectionHeaderViewHolder(holder: SectionHeaderViewHolder, header: ListItem.SectionHeader) {
        holder.binding.tvSectionTitle.text = header.title
        holder.binding.tvSectionCount.text = "(${header.count})"
        
        val isExpanded = expandedSections.contains(header.sectionType)
        holder.binding.ivExpandCollapse.rotation = if (isExpanded) 180f else 0f
        
        // Hintergrund und Textfarbe nach Section-Typ setzen (moderneres Design mit CardView)
        when (header.sectionType) {
            SectionType.OVERDUE -> {
                holder.binding.cardSectionHeader.setCardBackgroundColor(ContextCompat.getColor(context, R.color.section_overdue_bg))
                holder.binding.tvSectionTitle.setTextColor(ContextCompat.getColor(context, R.color.section_overdue_text))
                holder.binding.ivExpandCollapse.setColorFilter(ContextCompat.getColor(context, R.color.section_overdue_text))
                holder.binding.tvSectionCount.setTextColor(ContextCompat.getColor(context, R.color.section_overdue_text))
            }
            SectionType.DONE -> {
                holder.binding.cardSectionHeader.setCardBackgroundColor(ContextCompat.getColor(context, R.color.section_done_bg))
                holder.binding.tvSectionTitle.setTextColor(ContextCompat.getColor(context, R.color.section_done_text))
                holder.binding.ivExpandCollapse.setColorFilter(ContextCompat.getColor(context, R.color.section_done_text))
                holder.binding.tvSectionCount.setTextColor(ContextCompat.getColor(context, R.color.section_done_text))
            }
            SectionType.LISTE -> {
                // Wird nicht für SectionHeader verwendet, nur für ListeHeader
            }
        }
        
        // Click-Listener auf das gesamte Item setzen
        holder.itemView.setOnClickListener {
            toggleSection(header.sectionType)
        }
        
        // Auch auf den Titel klicken können
        holder.binding.tvSectionTitle.setOnClickListener {
            toggleSection(header.sectionType)
        }
        
        // Auch auf den Pfeil klicken können
        holder.binding.ivExpandCollapse.setOnClickListener {
            toggleSection(header.sectionType)
        }
    }
    
    private fun bindListeHeaderViewHolder(holder: ListeHeaderViewHolder, header: ListItem.ListeHeader) {
        holder.binding.tvSectionTitle.text = header.listeName
        holder.binding.tvSectionCount.text = "(${header.kundenCount})"
        
        val isExpanded = expandedListen.contains(header.listeId)
        holder.binding.ivExpandCollapse.rotation = if (isExpanded) 180f else 0f
        
        // Design für Liste-Header (andere Farbe als normale Sections)
        holder.binding.cardSectionHeader.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_blue))
        holder.binding.tvSectionTitle.setTextColor(ContextCompat.getColor(context, R.color.white))
        holder.binding.ivExpandCollapse.setColorFilter(ContextCompat.getColor(context, R.color.white))
        holder.binding.tvSectionCount.setTextColor(ContextCompat.getColor(context, R.color.white))
        
        holder.itemView.setOnClickListener {
            toggleListe(header.listeId)
        }
        
        holder.binding.tvSectionTitle.setOnClickListener {
            toggleListe(header.listeId)
        }
    }
    
    private fun toggleListe(listeId: String) {
        if (expandedListen.contains(listeId)) {
            expandedListen.remove(listeId)
        } else {
            expandedListen.add(listeId)
        }
        notifyDataSetChanged()
    }
    
    private fun toggleSection(sectionType: SectionType) {
        if (expandedSections.contains(sectionType)) {
            expandedSections.remove(sectionType)
        } else {
            expandedSections.add(sectionType)
        }
        notifyDataSetChanged()
        
        // TourPlannerActivity benachrichtigen, damit Daten neu geladen werden
        onSectionToggle?.invoke(sectionType)
    }

    private fun bindCustomerViewHolder(holder: CustomerViewHolder, customer: Customer) {
        holder.binding.tvItemName.text = customer.name
        holder.binding.tvItemAdresse.text = customer.adresse

        // Nächstes Tour-Datum berechnen und anzeigen (immer, auch in TourPlanner)
        val naechsteTour = if (customer.verschobenAufDatum > 0) {
            customer.verschobenAufDatum
        } else {
            customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
        }
        val cal = Calendar.getInstance()
        cal.timeInMillis = naechsteTour
        val dateStr = "${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}.${cal.get(Calendar.YEAR)}"
        holder.binding.tvNextTour.text = "Nächste Tour: $dateStr"
        holder.binding.tvNextTour.visibility = View.VISIBLE

        // Navigation-Button anzeigen wenn Adresse vorhanden (immer)
        holder.binding.btnNavigation.visibility = if (customer.adresse.isNotBlank()) View.VISIBLE else View.GONE
        holder.binding.btnNavigation.setOnClickListener {
            startNavigation(customer.adresse)
        }

        resetStyles(holder)

        if (displayedDateMillis != null) {
            applyStatusStyles(holder, customer)
            
            // Buttons nur bei fälligen Kunden anzeigen (nicht erledigt und fällig)
            val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
            val faelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum 
                           else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
            val displayedDateStart = displayedDateMillis ?: 0
            val isFaellig = faelligAm <= displayedDateStart + TimeUnit.DAYS.toMillis(1)
            val showButtons = !isDone && isFaellig
            
            holder.binding.btnAbholung.visibility = if (showButtons) View.VISIBLE else View.GONE
            holder.binding.btnAuslieferung.visibility = if (showButtons) View.VISIBLE else View.GONE
            holder.binding.btnVerschieben.visibility = if (showButtons) View.VISIBLE else View.GONE
            holder.binding.btnUrlaub.visibility = if (showButtons) View.VISIBLE else View.GONE
            // Rückgängig-Button nur bei erledigten Kunden anzeigen
            holder.binding.btnRueckgaengig.visibility = if (isDone) View.VISIBLE else View.GONE
        } else {
            // In CustomerManager: Nächstes Tour-Datum immer anzeigen
            val naechsteTour = if (customer.verschobenAufDatum > 0) {
                customer.verschobenAufDatum
            } else {
                customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
            }
            val cal = Calendar.getInstance()
            cal.timeInMillis = naechsteTour
            val dateStr = "${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}.${cal.get(Calendar.YEAR)}"
            holder.binding.tvNextTour.text = "Nächste Tour: $dateStr"
            holder.binding.tvNextTour.visibility = View.VISIBLE
            
            // Navigation-Button anzeigen wenn Adresse vorhanden
            holder.binding.btnNavigation.visibility = if (customer.adresse.isNotBlank()) View.VISIBLE else View.GONE
            holder.binding.btnNavigation.setOnClickListener {
                startNavigation(customer.adresse)
            }
            
            holder.binding.tvStatusLabel.visibility = View.GONE
            // Buttons in CustomerManager ausblenden
            holder.binding.btnAbholung.visibility = View.GONE
            holder.binding.btnAuslieferung.visibility = View.GONE
            holder.binding.btnVerschieben.visibility = View.GONE
            holder.binding.btnUrlaub.visibility = View.GONE
        }

        // Button-Handler
        holder.binding.btnAbholung.setOnClickListener { handleAbholung(customer) }
        holder.binding.btnAuslieferung.setOnClickListener { handleAuslieferung(customer) }
        holder.binding.btnVerschieben.setOnClickListener { showVerschiebenDialog(customer) }
        holder.binding.btnUrlaub.setOnClickListener { showUrlaubDialog(customer) }
        holder.binding.btnRueckgaengig.setOnClickListener { handleRueckgaengig(customer) }

        // Multi-Select oder normaler Click
        holder.itemView.setOnClickListener {
            if (isMultiSelectMode) {
                toggleCustomerSelection(customer.id, holder)
            } else {
                onClick(customer)
            }
        }
        
        // Long-Press für Multi-Select aktivieren
        holder.itemView.setOnLongClickListener {
            if (!isMultiSelectMode) {
                enableMultiSelectMode()
                toggleCustomerSelection(customer.id, holder)
            }
            true
        }
        
        // Multi-Select Visualisierung
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
    
    fun enableMultiSelectMode() {
        isMultiSelectMode = true
        selectedCustomers.clear()
        notifyDataSetChanged()
    }
    
    fun disableMultiSelectMode() {
        isMultiSelectMode = false
        selectedCustomers.clear()
        notifyDataSetChanged()
    }
    
    fun getSelectedCustomers(): List<Customer> {
        return items.filterIsInstance<ListItem.CustomerItem>()
            .map { it.customer }
            .filter { selectedCustomers.contains(it.id) }
    }
    
    fun hasSelectedCustomers(): Boolean = selectedCustomers.isNotEmpty()
    
    fun isMultiSelectModeEnabled(): Boolean = isMultiSelectMode
    
    private fun toggleCustomerSelection(customerId: String, holder: CustomerViewHolder) {
        if (selectedCustomers.contains(customerId)) {
            selectedCustomers.remove(customerId)
        } else {
            selectedCustomers.add(customerId)
        }
        notifyItemChanged(holder.adapterPosition)
    }

    private fun startNavigation(adresse: String) {
        val gmmIntentUri = Uri.parse("google.navigation:q=$adresse")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Google Maps ist nicht installiert.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetStyles(holder: CustomerViewHolder) {
        holder.binding.itemContainer.alpha = 1.0f
        holder.binding.tvItemName.setTextColor(Color.BLACK)
        holder.binding.tvItemName.setTypeface(null, Typeface.NORMAL)
        holder.binding.itemContainer.setBackgroundColor(Color.WHITE)
    }

    private fun applyStatusStyles(holder: CustomerViewHolder, customer: Customer) {
        holder.binding.tvStatusLabel.visibility = View.VISIBLE
        holder.binding.tvStatusLabel.setTextColor(Color.WHITE)

        val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
        val faelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())

        val heuteStart = getStartOfDay(System.currentTimeMillis())
        // Überfällig: Termin liegt in der Vergangenheit UND Kunde ist nicht erledigt
        val isActuallyOverdue = !isDone && faelligAm < heuteStart
        val showAsOverdue = isActuallyOverdue

        when {
            isDone -> {
                holder.binding.tvStatusLabel.text = "ERLEDIGT"
                holder.binding.tvStatusLabel.setBackgroundResource(R.drawable.status_badge_done)
                holder.binding.tvStatusLabel.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
                holder.binding.itemContainer.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.light_gray))
                holder.binding.itemContainer.alpha = 0.6f
            }
            showAsOverdue -> {
                holder.binding.tvStatusLabel.text = "ÜBERFÄLLIG"
                holder.binding.tvStatusLabel.setBackgroundResource(R.drawable.status_badge_overdue)
                holder.binding.tvStatusLabel.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
                holder.binding.tvItemName.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.status_overdue))
                holder.binding.tvItemName.setTypeface(null, Typeface.BOLD)
            }
            customer.verschobenAufDatum > 0 -> {
                holder.binding.tvStatusLabel.text = "VERSCHOBEN"
                holder.binding.tvStatusLabel.setBackgroundResource(R.drawable.status_badge_verschoben)
                holder.binding.tvStatusLabel.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
            }
            else -> holder.binding.tvStatusLabel.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newList: List<ListItem>, date: Long? = null) {
        // Alle Listen standardmäßig expanded machen
        newList.forEach { item ->
            if (item is ListItem.ListeHeader) {
                expandedListen.add(item.listeId)
            }
        }
        items = newList.toMutableList()
        displayedDateMillis = date
        notifyDataSetChanged()
    }
    
    fun isSectionExpanded(sectionType: SectionType): Boolean {
        return expandedSections.contains(sectionType)
    }
    
    fun shouldShowCustomer(customer: Customer): Boolean {
        val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
        val heuteStart = getStartOfDay(System.currentTimeMillis())
        val displayedDateStart = displayedDateMillis ?: 0
        val faelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum 
                       else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
        // Überfällig: Termin liegt in der Vergangenheit UND Kunde ist nicht erledigt
        val isOverdue = !isDone && faelligAm < heuteStart
        
        return when {
            isDone -> isSectionExpanded(SectionType.DONE)
            isOverdue -> isSectionExpanded(SectionType.OVERDUE)
            else -> true // Normale Kunden immer anzeigen
        }
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

    private fun handleAbholung(customer: Customer) {
        if (customer.abholungErfolgt) return
        onAbholung?.invoke(customer)
    }

    private fun handleAuslieferung(customer: Customer) {
        if (customer.auslieferungErfolgt) return
        onAuslieferung?.invoke(customer)
    }

    private fun resetTourCycle(customerId: String) {
        onResetTourCycle?.invoke(customerId)
    }

    private fun showVerschiebenDialog(customer: Customer) {
        val cal = Calendar.getInstance()
        DatePickerDialog(context, { _, year, month, dayOfMonth ->
            val picked = Calendar.getInstance().apply { set(year, month, dayOfMonth, 0, 0, 0) }
            val newDate = picked.timeInMillis
            
            // Dialog: Nur diesen Termin oder alle restlichen Termine verschieben?
            AlertDialog.Builder(context)
                .setTitle("Termin verschieben")
                .setMessage("Wie möchten Sie vorgehen?")
                .setPositiveButton("Nur diesen Termin") { _, _ ->
                    // Nur diesen Termin verschieben
                    onVerschieben?.invoke(customer, newDate, false)
                }
                .setNeutralButton("Alle zukünftigen Termine") { _, _ ->
                    // Alle zukünftigen Termine verschieben
                    onVerschieben?.invoke(customer, newDate, true)
                }
                .setNegativeButton("Abbrechen", null)
                .show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun handleRueckgaengig(customer: Customer) {
        if (!customer.abholungErfolgt && !customer.auslieferungErfolgt) return
        
        AlertDialog.Builder(context)
            .setTitle("Rückgängig machen")
            .setMessage("Möchten Sie die Erledigung wirklich rückgängig machen?")
            .setPositiveButton("Ja") { _, _ ->
                onRueckgaengig?.invoke(customer)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun showUrlaubDialog(customer: Customer) {
        val cal = Calendar.getInstance()
        var urlaubVon: Long = 0

        val dateSetListenerVon = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val pickedVon = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0) }
            urlaubVon = pickedVon.timeInMillis

            val dateSetListenerBis = DatePickerDialog.OnDateSetListener { _, y, m, d ->
                val pickedBis = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }
                if (pickedBis.timeInMillis >= urlaubVon) {
                    onUrlaub?.invoke(customer, urlaubVon, pickedBis.timeInMillis)
                } else {
                    Toast.makeText(context, "Enddatum muss nach Startdatum sein!", Toast.LENGTH_SHORT).show()
                }
            }

            DatePickerDialog(context, dateSetListenerBis, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                setTitle("Urlaub bis")
                show()
            }
        }

        DatePickerDialog(context, dateSetListenerVon, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
            setTitle("Urlaub von")
            show()
        }
    }
}
