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
import com.example.we2026_5.adapter.CustomerAdapterCallbacksConfig
import com.example.we2026_5.adapter.CustomerViewHolder
import com.example.we2026_5.adapter.SectionHeaderViewHolder
import com.example.we2026_5.adapter.ListeHeaderViewHolder
import com.example.we2026_5.databinding.ItemCustomerBinding
import com.example.we2026_5.databinding.ItemSectionHeaderBinding
import com.example.we2026_5.util.TerminBerechnungUtils
import java.util.Calendar
import java.util.concurrent.TimeUnit

class CustomerAdapter(
    private var items: MutableList<ListItem>,
    private val context: Context,
    private val onClick: (Customer) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var displayedDateMillis: Long? = null
    // Sections standardmäßig eingeklappt (collapsed)
    private var expandedSections = mutableSetOf<SectionType>()
    
    // Multi-Select für Bulk-Operationen
    private var isMultiSelectMode = false
    private val selectedCustomers = mutableSetOf<String>() // Customer IDs
    
    // Button-Zustand: Welcher Button wurde für welchen Kunden gedrückt
    private val pressedButtons = mutableMapOf<String, String>() // customerId -> "A", "L", "V", "U"
    
    /** Gebündelte Callbacks (Erledigung, Datum-Berechnung, Aktionen). */
    var callbacks: CustomerAdapterCallbacksConfig = CustomerAdapterCallbacksConfig()
    
    // Dialog-Helper für Verschieben, Urlaub, Rückgängig
    private val dialogHelper: CustomerDialogHelper by lazy {
        CustomerDialogHelper(
            context = context,
            onVerschieben = { customer, newDate, alleVerschieben, typ ->
                this@CustomerAdapter.callbacks.onVerschieben?.invoke(customer, newDate, alleVerschieben, typ)
            },
            onUrlaub = { customer, von, bis ->
                this@CustomerAdapter.callbacks.onUrlaub?.invoke(customer, von, bis)
            },
            onRueckgaengig = { customer ->
                this@CustomerAdapter.callbacks.onRueckgaengig?.invoke(customer)
            },
            onButtonStateReset = { customerId ->
                pressedButtons.remove(customerId)
                val position = items.indexOfFirst { it is ListItem.CustomerItem && it.customer.id == customerId }
                if (position != -1) notifyItemChanged(position)
            }
        )
    }

    /** Binder einmal pro Adapter-Lifecycle (wird bei updateData invalidiert). */
    private var binder: CustomerViewHolderBinder? = null

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
            getStartOfDay = { ts -> TerminBerechnungUtils.getStartOfDay(ts) }
        )
    }
    
    private val callbackHelper: CustomerAdapterCallbacks by lazy {
        CustomerAdapterCallbacks(context = context) { callbacks }
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
                // Kunden werden bereits gefiltert vom ViewModel/TourDataProcessor
                // Liste-Kunden werden nicht mehr als separate Items hinzugefügt (nur in ListeHeader)
                // Section-Kunden werden nicht mehr als separate Items hinzugefügt (nur in SectionHeader)
                // Daher: Alle CustomerItems hier sind gültig und sollten angezeigt werden
                bindCustomerViewHolder(holder as CustomerViewHolder, item.customer, item.isOverdue)
                holder.itemView.visibility = View.VISIBLE
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
                    holder.itemView.visibility = View.VISIBLE
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
        callbacks.onSectionToggle?.invoke(sectionType)
    }

    private fun bindCustomerViewHolder(holder: CustomerViewHolder, customer: Customer, isOverdue: Boolean = false) {
        holder.binding.btnNavigation.setOnClickListener {
            callbackHelper.startNavigation(customer.adresse)
        }
        if (binder == null) {
            binder = CustomerViewHolderBinder(
                context = context,
                displayedDateMillis = displayedDateMillis,
                pressedButtons = pressedButtons,
                selectedCustomers = selectedCustomers,
                isMultiSelectMode = isMultiSelectMode,
                callbacksConfig = callbacks,
                onClick = onClick,
                dialogHelper = dialogHelper,
                onAbholung = { c -> callbackHelper.handleAbholung(c) },
                onAuslieferung = { c -> callbackHelper.handleAuslieferung(c) },
                onKw = { c -> callbackHelper.handleKw(c) },
                enableMultiSelectMode = { enableMultiSelectMode() },
                toggleCustomerSelection = { customerId, h -> toggleCustomerSelection(customerId, h as com.example.we2026_5.adapter.CustomerViewHolder) }
            )
        }
        binder!!.bind(holder, customer, isOverdue)
    }
    
    fun enableMultiSelectMode() {
        isMultiSelectMode = true
        selectedCustomers.clear()
        binder = null
        notifyDataSetChanged()
    }
    
    fun disableMultiSelectMode() {
        isMultiSelectMode = false
        selectedCustomers.clear()
        binder = null
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
            Toast.makeText(context, context.getString(R.string.error_maps_not_installed), Toast.LENGTH_SHORT).show()
        }
    }


    override fun getItemCount() = items.size

    fun updateData(newList: List<ListItem>, date: Long? = null) {
        items = newList.toMutableList()
        displayedDateMillis = date
        binder = null // Binder bei neuem Datum/Liste neu erstellen
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
    
    private fun resetTourCycle(customerId: String) {
        callbacks.onResetTourCycle?.invoke(customerId)
    }
}
