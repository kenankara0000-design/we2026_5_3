package com.example.we2026_5

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.adapter.CustomerDialogHelper
import com.example.we2026_5.adapter.CustomerViewHolderBinder
import com.example.we2026_5.adapter.CustomerItemHelper
import com.example.we2026_5.adapter.CustomerAdapterCallbacks
import com.example.we2026_5.adapter.CustomerViewHolder
import com.example.we2026_5.adapter.SectionHeaderViewHolder
import com.example.we2026_5.adapter.ListeHeaderViewHolder
import com.example.we2026_5.databinding.ItemCustomerBinding
import com.example.we2026_5.databinding.ItemSectionHeaderBinding
import java.util.Calendar
import java.util.concurrent.TimeUnit

sealed class ListItem {
    data class CustomerItem(val customer: Customer) : ListItem()
    data class SectionHeader(val title: String, val count: Int, val erledigtCount: Int, val sectionType: SectionType) : ListItem()
    data class ListeHeader(val listeName: String, val kundenCount: Int, val erledigtCount: Int, val listeId: String) : ListItem()
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
    // Sections standardmäßig eingeklappt (collapsed)
    private var expandedSections = mutableSetOf<SectionType>()
    var onSectionToggle: ((SectionType) -> Unit)? = null
    
    // Multi-Select für Bulk-Operationen
    private var isMultiSelectMode = false
    private val selectedCustomers = mutableSetOf<String>() // Customer IDs
    
    // Button-Zustand: Welcher Button wurde für welchen Kunden gedrückt
    private val pressedButtons = mutableMapOf<String, String>() // customerId -> "A", "L", "V", "U"
    
    // Dialog-Helper für Verschieben, Urlaub, Rückgängig
    private val dialogHelper: CustomerDialogHelper by lazy {
        CustomerDialogHelper(
            context = context,
            onVerschieben = { customer, newDate, alleVerschieben ->
                onVerschieben?.invoke(customer, newDate, alleVerschieben)
            },
            onUrlaub = { customer, von, bis ->
                onUrlaub?.invoke(customer, von, bis)
            },
            onRueckgaengig = { customer ->
                onRueckgaengig?.invoke(customer)
            },
            onButtonStateReset = { customerId ->
                pressedButtons.remove(customerId)
                val position = items.indexOfFirst { it is ListItem.CustomerItem && it.customer.id == customerId }
                if (position != -1) notifyItemChanged(position)
            }
        )
    }
    
    // Callbacks für Firebase-Operationen (statt direkter Firebase-Aufrufe)
    var onAbholung: ((Customer) -> Unit)? = null
    var onAuslieferung: ((Customer) -> Unit)? = null
    var onResetTourCycle: ((String) -> Unit)? = null
    var onVerschieben: ((Customer, Long, Boolean) -> Unit)? = null // customer, newDate, alleVerschieben
    var onUrlaub: ((Customer, Long, Long) -> Unit)? = null // customer, von, bis
    var onRueckgaengig: ((Customer) -> Unit)? = null
    var onBulkMarkDone: ((List<Customer>) -> Unit)? = null // Für Bulk-Operationen
    var onTerminClick: ((Customer, Long) -> Unit)? = null // customer, terminDatum - für Termin-Detail-Dialog
    // Callbacks für Datum-Berechnung (für A/L Button-Aktivierung)
    var getAbholungDatum: ((Customer) -> Long)? = null // Gibt Abholungsdatum für heute zurück
    var getAuslieferungDatum: ((Customer) -> Long)? = null // Gibt Auslieferungsdatum für heute zurück
    
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
    
    // Helper-Klassen
    private val itemHelper: CustomerItemHelper by lazy {
        CustomerItemHelper(
            context = context,
            displayedDateMillis = displayedDateMillis,
            items = items,
            expandedSections = expandedSections,
            expandedListen = expandedListen,
            bindCustomerViewHolder = { holder, customer -> bindCustomerViewHolder(holder, customer) },
            getStartOfDay = { ts -> getStartOfDay(ts) }
        )
    }
    
    private val callbacks: CustomerAdapterCallbacks by lazy {
        CustomerAdapterCallbacks(
            context = context,
            onAbholung = onAbholung,
            onAuslieferung = onAuslieferung,
            onSectionToggle = onSectionToggle
        )
    }

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
                
                // Prüfe ob Kunde tatsächlich zu einem Section gehört (wird in CardView angezeigt)
                val isInSection = if (prevItem is ListItem.SectionHeader && displayedDateMillis != null) {
                    // Prüfe ob Kunde tatsächlich zu diesem Section gehört
                    val customer = item.customer
                    val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt
                    when (prevItem.sectionType) {
                        SectionType.OVERDUE -> {
                            val heuteStart = getStartOfDay(System.currentTimeMillis())
                            val viewDateStart = displayedDateMillis?.let { getStartOfDay(it) } ?: heuteStart
                            val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                                customer = customer,
                                startDatum = heuteStart - java.util.concurrent.TimeUnit.DAYS.toMillis(365),
                                tageVoraus = 730
                            )
                            val isOverdue = termine.any { termin ->
                                val terminStart = getStartOfDay(termin.datum)
                                terminStart < heuteStart && terminStart != viewDateStart && // Nicht überfällig wenn genau am angezeigten Tag
                                com.example.we2026_5.util.TerminFilterUtils.sollUeberfaelligAnzeigen(
                                    terminDatum = termin.datum,
                                    anzeigeDatum = viewDateStart,
                                    aktuellesDatum = heuteStart
                                )
                            }
                            isOverdue && !isDone
                        }
                        SectionType.DONE -> isDone
                        else -> false
                    }
                } else {
                    false
                }
                
                // Im CustomerManager: Alle Kunden immer anzeigen (alphabetisch sortiert)
                val isInCustomerManager = displayedDateMillis == null
                
                if (isInListe && !isInCustomerManager) {
                    // Im TourPlanner: Kunde gehört zu einer Liste - wird im Container angezeigt, also hier verstecken
                    // Kunden aus Listen werden nur im Container der CardView angezeigt, nicht als separate Items
                    holder.itemView.visibility = View.GONE
                } else if (isInSection && !isInCustomerManager) {
                    // Kunde gehört tatsächlich zu einem Section - wird in CardView angezeigt, also hier verstecken
                    holder.itemView.visibility = View.GONE
                } else {
                    // Im CustomerManager: Alle Kunden immer anzeigen
                    // Im TourPlanner: Gewerblich-Kunde oder nicht in Liste/Section
                    // WICHTIG: Kunden mit listeId sollten NUR in Listen-Containern angezeigt werden
                    val customer = item.customer
                    if (customer.listeId.isNotEmpty() && !isInCustomerManager) {
                        // Kunde gehört zu einer Liste - sollte nicht als separates Item angezeigt werden
                        holder.itemView.visibility = View.GONE
                    } else if (shouldShowCustomer(customer)) {
                        bindCustomerViewHolder(holder as CustomerViewHolder, customer)
                        
                        // WICHTIG: Wenn Kunde im Überfällig-Bereich ist, nur A-Button anzeigen
                        // Wenn Kunde im normalen Bereich ist und auch im Überfällig-Bereich ist, nur L-Button anzeigen
                        if (displayedDateMillis != null && prevItem is ListItem.SectionHeader) {
                            val heuteStart = getStartOfDay(System.currentTimeMillis())
                            val viewDateStart = displayedDateMillis?.let { getStartOfDay(it) } ?: heuteStart
                            
                            // Prüfe welche Termine am Tag fällig sind
                            val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                                customer = customer,
                                startDatum = viewDateStart - java.util.concurrent.TimeUnit.DAYS.toMillis(365),
                                tageVoraus = 730
                            )
                            val termineAmTag = termine.filter { getStartOfDay(it.datum) == viewDateStart }
                            val hatAbholungAmTag = termineAmTag.any { it.typ == com.example.we2026_5.TerminTyp.ABHOLUNG }
                            val hatAuslieferungAmTag = termineAmTag.any { it.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG }
                            
                            // Prüfe ob Abholung überfällig ist
                            val abholungUeberfaellig = if (hatAbholungAmTag) {
                                val abholungTermin = termineAmTag.firstOrNull { it.typ == com.example.we2026_5.TerminTyp.ABHOLUNG }
                                if (abholungTermin != null && !customer.abholungErfolgt) {
                                    val terminStart = getStartOfDay(abholungTermin.datum)
                                    terminStart < heuteStart && com.example.we2026_5.util.TerminFilterUtils.sollUeberfaelligAnzeigen(
                                        terminDatum = abholungTermin.datum,
                                        anzeigeDatum = viewDateStart,
                                        aktuellesDatum = heuteStart
                                    )
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                            
                            // Prüfe ob Auslieferung überfällig ist
                            val auslieferungUeberfaellig = if (hatAuslieferungAmTag) {
                                val auslieferungTermin = termineAmTag.firstOrNull { it.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG }
                                if (auslieferungTermin != null && !customer.auslieferungErfolgt) {
                                    val terminStart = getStartOfDay(auslieferungTermin.datum)
                                    terminStart < heuteStart && com.example.we2026_5.util.TerminFilterUtils.sollUeberfaelligAnzeigen(
                                        terminDatum = auslieferungTermin.datum,
                                        anzeigeDatum = viewDateStart,
                                        aktuellesDatum = heuteStart
                                    )
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                            
                            // Wenn im Überfällig-Bereich: Nur A-Button anzeigen (wenn Abholung überfällig)
                            if (prevItem.sectionType == SectionType.OVERDUE) {
                                if (!abholungUeberfaellig) {
                                    holder.binding.btnAbholung.visibility = View.GONE
                                }
                                // L-Button ausblenden wenn Auslieferung nicht überfällig
                                if (!auslieferungUeberfaellig) {
                                    holder.binding.btnAuslieferung.visibility = View.GONE
                                }
                            } else if (prevItem.sectionType != SectionType.DONE) {
                                // Im normalen Bereich: Nur L-Button anzeigen (wenn Auslieferung normal)
                                // Wenn Abholung überfällig ist, sollte L-Button angezeigt werden
                                if (abholungUeberfaellig && !auslieferungUeberfaellig) {
                                    // Abholung überfällig, Auslieferung normal -> nur L-Button
                                    holder.binding.btnAbholung.visibility = View.GONE
                                } else if (!hatAbholungAmTag) {
                                    // Keine Abholung am Tag -> A-Button ausblenden
                                    holder.binding.btnAbholung.visibility = View.GONE
                                }
                                if (!hatAuslieferungAmTag) {
                                    // Keine Auslieferung am Tag -> L-Button ausblenden
                                    holder.binding.btnAuslieferung.visibility = View.GONE
                                }
                            }
                        }
                        
                        holder.itemView.visibility = View.VISIBLE
                    } else {
                        holder.itemView.visibility = View.GONE
                    }
                }
            }
            is ListItem.ListeHeader -> {
                // Im CustomerManager: Liste-Header ausblenden (alle Kunden untereinander)
                if (displayedDateMillis == null) {
                    holder.itemView.visibility = View.GONE
                } else {
                    itemHelper.bindListeHeaderViewHolder(holder as ListeHeaderViewHolder, item)
                    // Click-Listener setzen
                    holder.itemView.setOnClickListener {
                        toggleListe(item.listeId)
                    }
                    holder.binding.tvSectionTitle.setOnClickListener {
                        toggleListe(item.listeId)
                    }
                    holder.binding.tvExpandCollapse.setOnClickListener {
                        toggleListe(item.listeId)
                    }
                }
            }
            is ListItem.SectionHeader -> {
                // Im CustomerManager: Section-Header ausblenden (alle Kunden untereinander)
                if (displayedDateMillis == null) {
                    holder.itemView.visibility = View.GONE
                } else {
                    itemHelper.bindSectionHeaderViewHolder(holder as SectionHeaderViewHolder, item)
                    // Click-Listener setzen
                    holder.itemView.setOnClickListener {
                        toggleSection(item.sectionType)
                    }
                    holder.binding.tvSectionTitle.setOnClickListener {
                        toggleSection(item.sectionType)
                    }
                    holder.binding.tvExpandCollapse.setOnClickListener {
                        toggleSection(item.sectionType)
                    }
                }
            }
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
        // Navigation-Button setzen
        holder.binding.btnNavigation.setOnClickListener {
            callbacks.startNavigation(customer.adresse)
        }
        
        // Verwende den ViewHolder-Binder für den Rest
        // Aktualisiere den Binder mit aktuellen Werten (da lazy init nur einmal ausgeführt wird)
        val binder = CustomerViewHolderBinder(
            context = context,
            displayedDateMillis = displayedDateMillis,
            pressedButtons = pressedButtons,
            selectedCustomers = selectedCustomers,
            isMultiSelectMode = isMultiSelectMode,
            getAbholungDatum = getAbholungDatum,
            getAuslieferungDatum = getAuslieferungDatum,
            onTerminClick = onTerminClick,
            onClick = onClick,
            dialogHelper = dialogHelper,
            onAbholung = { customer -> callbacks.handleAbholung(customer) },
            onAuslieferung = { customer -> callbacks.handleAuslieferung(customer) },
            enableMultiSelectMode = { enableMultiSelectMode() },
            toggleCustomerSelection = { customerId, holder -> toggleCustomerSelection(customerId, holder as com.example.we2026_5.adapter.CustomerViewHolder) }
        )
        binder.bind(holder, customer)
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
    
    private fun toggleCustomerSelection(customerId: String, holder: com.example.we2026_5.adapter.CustomerViewHolder) {
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


    override fun getItemCount() = items.size

    fun updateData(newList: List<ListItem>, date: Long? = null) {
        // Listen bleiben standardmäßig eingeklappt (collapsed)
        items = newList.toMutableList()
        displayedDateMillis = date
        notifyDataSetChanged()
    }
    
    /**
     * Entfernt einen Kunden direkt aus dem Adapter (optimistische UI-Aktualisierung)
     * Entfernt alle Vorkommen des Kunden (falls er mehrfach in der Liste ist)
     */
    fun removeCustomer(customerId: String) {
        var removedCount = 0
        var firstPosition = -1
        
        // Alle Vorkommen des Kunden finden und entfernen (rückwärts, um Indizes nicht zu verschieben)
        for (i in items.size - 1 downTo 0) {
            if (items[i] is ListItem.CustomerItem && (items[i] as ListItem.CustomerItem).customer.id == customerId) {
                if (firstPosition == -1) {
                    firstPosition = i
                }
                items.removeAt(i)
                removedCount++
            }
        }
        
        if (removedCount > 0 && firstPosition != -1) {
            // Benachrichtige über die Änderungen
            notifyItemRangeRemoved(firstPosition, removedCount)
            if (firstPosition < items.size) {
                notifyItemRangeChanged(firstPosition, items.size - firstPosition)
            }
        }
    }
    
    /**
     * Setzt alle gedrückten Buttons zurück (nach erfolgreicher Aktion)
     */
    fun clearPressedButtons() {
        pressedButtons.clear()
        notifyDataSetChanged()
    }
    
    fun isSectionExpanded(sectionType: SectionType): Boolean {
        return expandedSections.contains(sectionType)
    }
    
    fun shouldShowCustomer(customer: Customer): Boolean {
        // Im TourPlanner: Alle Kunden anzeigen (wurde bereits vom ViewModel gefiltert)
        if (displayedDateMillis != null) {
            return true
        }
        
        // Im CustomerManager: Alle Kunden immer anzeigen (alphabetisch sortiert, keine Sections)
        return true
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

    private fun resetTourCycle(customerId: String) {
        onResetTourCycle?.invoke(customerId)
    }
}
