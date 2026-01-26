package com.example.we2026_5

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.databinding.ItemCustomerBinding
import com.example.we2026_5.databinding.ItemSectionHeaderBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

sealed class ListItem {
    data class CustomerItem(val customer: Customer) : ListItem()
    data class SectionHeader(val title: String, val count: Int, val sectionType: SectionType) : ListItem()
}

enum class SectionType {
    OVERDUE, DONE
}

class CustomerAdapter(
    private var items: List<ListItem>,
    private val context: Context,
    private val onClick: (Customer) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var displayedDateMillis: Long? = null
    private val db = FirebaseFirestore.getInstance()
    private var expandedSections = mutableSetOf<SectionType>() // Standardmäßig eingeklappt
    var onSectionToggle: ((SectionType) -> Unit)? = null

    companion object {
        private const val VIEW_TYPE_CUSTOMER = 0
        private const val VIEW_TYPE_SECTION_HEADER = 1
    }

    inner class CustomerViewHolder(val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root)
    inner class SectionHeaderViewHolder(val binding: ItemSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.CustomerItem -> VIEW_TYPE_CUSTOMER
            is ListItem.SectionHeader -> VIEW_TYPE_SECTION_HEADER
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
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.CustomerItem -> {
                // Kunde nur anzeigen wenn Section erweitert ist (für überfällige/erledigte)
                if (shouldShowCustomer(item.customer)) {
                    bindCustomerViewHolder(holder as CustomerViewHolder, item.customer)
                    holder.itemView.visibility = View.VISIBLE
                } else {
                    holder.itemView.visibility = View.GONE
                }
            }
            is ListItem.SectionHeader -> bindSectionHeaderViewHolder(holder as SectionHeaderViewHolder, item)
        }
    }

    private fun bindSectionHeaderViewHolder(holder: SectionHeaderViewHolder, header: ListItem.SectionHeader) {
        holder.binding.tvSectionTitle.text = header.title
        holder.binding.tvSectionCount.text = "(${header.count})"
        
        val isExpanded = expandedSections.contains(header.sectionType)
        holder.binding.ivExpandCollapse.rotation = if (isExpanded) 180f else 0f
        
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

        holder.itemView.setOnClickListener { onClick(customer) }
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
                holder.binding.tvStatusLabel.setBackgroundColor(Color.GRAY)
                holder.binding.itemContainer.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.light_gray))
                holder.binding.itemContainer.alpha = 0.6f
            }
            showAsOverdue -> {
                holder.binding.tvStatusLabel.text = "ÜBERFÄLLIG"
                holder.binding.tvStatusLabel.setBackgroundColor(Color.TRANSPARENT)
                holder.binding.tvStatusLabel.setTextColor(Color.RED)
                holder.binding.tvItemName.setTextColor(Color.RED)
                holder.binding.tvItemName.setTypeface(null, Typeface.BOLD)
            }
            customer.verschobenAufDatum > 0 -> {
                holder.binding.tvStatusLabel.text = "VERSCHOBEN"
                holder.binding.tvStatusLabel.setBackgroundColor(Color.BLUE)
            }
            else -> holder.binding.tvStatusLabel.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newList: List<ListItem>, date: Long? = null) {
        items = newList
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
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeWithRetryAndToast(
                operation = { 
                    db.collection("customers").document(customer.id)
                        .update("abholungErfolgt", true)
                },
                context = context,
                errorMessage = "Fehler beim Registrieren der Abholung. Bitte erneut versuchen.",
                maxRetries = 3
            )
            if (success != null) {
                Toast.makeText(context, "Abholung registriert", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleAuslieferung(customer: Customer) {
        if (customer.auslieferungErfolgt) return
        
        val wasAbholungErfolgt = customer.abholungErfolgt
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeWithRetryAndToast(
                operation = { 
                    db.collection("customers").document(customer.id)
                        .update("auslieferungErfolgt", true)
                },
                context = context,
                errorMessage = "Fehler beim Registrieren der Auslieferung. Bitte erneut versuchen.",
                maxRetries = 3
            )
            if (success != null) {
                Toast.makeText(context, "Auslieferung registriert", Toast.LENGTH_SHORT).show()
                if (wasAbholungErfolgt) {
                    resetTourCycle(customer.id)
                }
            }
        }
    }

    private fun resetTourCycle(customerId: String) {
        val resetData = mapOf(
            "letzterTermin" to System.currentTimeMillis(),
            "abholungErfolgt" to false,
            "auslieferungErfolgt" to false,
            "verschobenAufDatum" to 0
        )
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeWithRetryAndToast(
                operation = { 
                    db.collection("customers").document(customerId).update(resetData)
                },
                context = context,
                errorMessage = "Fehler beim Zurücksetzen. Bitte erneut versuchen.",
                maxRetries = 3
            )
            if (success != null) {
                Toast.makeText(context, "Tour abgeschlossen!", Toast.LENGTH_SHORT).show()
            }
        }
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
                    CoroutineScope(Dispatchers.Main).launch {
                        val success = FirebaseRetryHelper.executeWithRetryAndToast(
                            operation = { 
                                db.collection("customers").document(customer.id)
                                    .update("verschobenAufDatum", newDate)
                            },
                            context = context,
                            errorMessage = "Fehler beim Verschieben. Bitte erneut versuchen.",
                            maxRetries = 3
                        )
                        if (success != null) {
                            Toast.makeText(context, "Termin verschoben", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNeutralButton("Alle zukünftigen Termine") { _, _ ->
                    // Alle zukünftigen Termine verschieben
                    val aktuellerFaelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum
                                             else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
                    val diff = newDate - aktuellerFaelligAm
                    
                    // letzterTermin anpassen, damit alle zukünftigen Termine verschoben werden
                    val neuerLetzterTermin = customer.letzterTermin + diff
                    CoroutineScope(Dispatchers.Main).launch {
                        val success = FirebaseRetryHelper.executeWithRetryAndToast(
                            operation = { 
                                db.collection("customers").document(customer.id)
                                    .update(mapOf(
                                        "letzterTermin" to neuerLetzterTermin,
                                        "verschobenAufDatum" to 0
                                    ))
                            },
                            context = context,
                            errorMessage = "Fehler beim Verschieben. Bitte erneut versuchen.",
                            maxRetries = 3
                        )
                        if (success != null) {
                            Toast.makeText(context, "Alle zukünftigen Termine verschoben", Toast.LENGTH_SHORT).show()
                        }
                    }
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
                val updates = mutableMapOf<String, Any>()
                if (customer.abholungErfolgt) updates["abholungErfolgt"] = false
                if (customer.auslieferungErfolgt) updates["auslieferungErfolgt"] = false
                
                CoroutineScope(Dispatchers.Main).launch {
                    val success = FirebaseRetryHelper.executeWithRetryAndToast(
                        operation = { 
                            db.collection("customers").document(customer.id).update(updates)
                        },
                        context = context,
                        errorMessage = "Fehler beim Rückgängigmachen. Bitte erneut versuchen.",
                        maxRetries = 3
                    )
                    if (success != null) {
                        Toast.makeText(context, "Rückgängig gemacht", Toast.LENGTH_SHORT).show()
                    }
                }
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
                    CoroutineScope(Dispatchers.Main).launch {
                        val success = FirebaseRetryHelper.executeWithRetryAndToast(
                            operation = { 
                                db.collection("customers").document(customer.id)
                                    .update(mapOf("urlaubVon" to urlaubVon, "urlaubBis" to pickedBis.timeInMillis))
                            },
                            context = context,
                            errorMessage = "Fehler beim Eintragen des Urlaubs. Bitte erneut versuchen.",
                            maxRetries = 3
                        )
                        if (success != null) {
                            Toast.makeText(context, "Urlaub eingetragen", Toast.LENGTH_SHORT).show()
                        }
                    }
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
