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
    // Sections standardmäßig expanded, damit sie sichtbar sind
    private var expandedSections = mutableSetOf<SectionType>(SectionType.OVERDUE, SectionType.DONE)
    var onSectionToggle: ((SectionType) -> Unit)? = null
    
    // Multi-Select für Bulk-Operationen
    private var isMultiSelectMode = false
    private val selectedCustomers = mutableSetOf<String>() // Customer IDs
    
    // Button-Zustand: Welcher Button wurde für welchen Kunden gedrückt
    private val pressedButtons = mutableMapOf<String, String>() // customerId -> "A", "L", "V", "U"
    
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
                
                // Im CustomerManager: Alle Kunden immer anzeigen (alphabetisch sortiert)
                val isInCustomerManager = displayedDateMillis == null
                
                if (isInListe && !isInCustomerManager) {
                    // Im TourPlanner: Kunde gehört zu einer Liste - nur anzeigen wenn Liste expanded ist
                    val listeHeader = prevItem as ListItem.ListeHeader
                    if (expandedListen.contains(listeHeader.listeId)) {
                        bindCustomerViewHolder(holder as CustomerViewHolder, item.customer)
                        holder.itemView.visibility = View.VISIBLE
                    } else {
                        holder.itemView.visibility = View.GONE
                    }
                } else {
                    // Im CustomerManager: Alle Kunden immer anzeigen
                    // Im TourPlanner: Gewerblich-Kunde oder nicht in Liste
                    if (shouldShowCustomer(item.customer)) {
                        bindCustomerViewHolder(holder as CustomerViewHolder, item.customer)
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
                    bindListeHeaderViewHolder(holder as ListeHeaderViewHolder, item)
                }
            }
            is ListItem.SectionHeader -> {
                // Im CustomerManager: Section-Header ausblenden (alle Kunden untereinander)
                if (displayedDateMillis == null) {
                    holder.itemView.visibility = View.GONE
                } else {
                    bindSectionHeaderViewHolder(holder as SectionHeaderViewHolder, item)
                }
            }
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
        
        // Kunden-Art anzeigen
        holder.binding.tvItemKundenArt.text = customer.kundenArt
        holder.binding.tvItemKundenArt.visibility = View.VISIBLE
        
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

        // Nächstes Tour-Datum berechnen und anzeigen
        // Verwende getFaelligAm() um sowohl neue (intervalle) als auch alte Struktur zu unterstützen
        val naechsteTour = customer.getFaelligAm()
        
        if (naechsteTour > 0) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = naechsteTour
            val dateStr = "${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}.${cal.get(Calendar.YEAR)}"
            holder.binding.tvNextTour.text = "Nächste Tour: $dateStr"
            holder.binding.tvNextTour.visibility = View.VISIBLE
        } else {
            // Kein Termin gefunden - zeige entsprechenden Text
            holder.binding.tvNextTour.text = "Nächste Tour: Kein Termin"
            holder.binding.tvNextTour.visibility = View.VISIBLE
        }

        // Navigation-Button anzeigen wenn Adresse vorhanden (immer)
        holder.binding.btnNavigation.visibility = if (customer.adresse.isNotBlank()) View.VISIBLE else View.GONE
        holder.binding.btnNavigation.setOnClickListener {
            startNavigation(customer.adresse)
        }

        resetStyles(holder)

        // Status-Styles anwenden wenn im TourPlanner
        if (displayedDateMillis != null) {
            applyStatusStyles(holder, customer)
        } else {
            holder.binding.tvStatusLabel.visibility = View.GONE
        }

        // Button-Handler (immer setzen)
        holder.binding.btnAbholung.setOnClickListener { 
            pressedButtons[customer.id] = "A"
            handleAbholung(customer) 
        }
        holder.binding.btnAuslieferung.setOnClickListener { 
            pressedButtons[customer.id] = "L"
            handleAuslieferung(customer) 
        }
        holder.binding.btnVerschieben.setOnClickListener { 
            pressedButtons[customer.id] = "V"
            showVerschiebenDialog(customer) 
        }
        holder.binding.btnUrlaub.setOnClickListener { 
            pressedButtons[customer.id] = "U"
            showUrlaubDialog(customer) 
        }
        holder.binding.btnRueckgaengig.setOnClickListener { handleRueckgaengig(customer) }

        // Termin-Klick: Wenn im TourPlanner, öffne Termin-Detail-Dialog
        // Normale Klicks auf die Card werden nur im CustomerManager verwendet
        if (displayedDateMillis != null) {
            // Im TourPlanner: Klick auf Card öffnet Termin-Detail-Dialog
            holder.itemView.setOnClickListener {
                if (isMultiSelectMode) {
                    toggleCustomerSelection(customer.id, holder)
                } else {
                    // Berechne das aktuelle Termin-Datum für diesen Kunden
                    val terminDatum = if (customer.verschobenAufDatum > 0) {
                        customer.verschobenAufDatum
                    } else {
                        // Berechne fälliges Datum basierend auf Customer-Daten
                        customer.getFaelligAm()
                    }
                    onTerminClick?.invoke(customer, terminDatum)
                }
            }
        } else {
            // Im CustomerManager: Normaler Click öffnet CustomerDetailActivity
            holder.itemView.setOnClickListener {
                if (isMultiSelectMode) {
                    toggleCustomerSelection(customer.id, holder)
                } else {
                    onClick(customer)
                }
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
        
        // WICHTIG: Buttons-Sichtbarkeit und -Farben am ENDE setzen, damit sie nicht überschrieben werden
        // Im TourPlanner: Buttons für ALLE nicht-erledigten Kunden anzeigen
        val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
        if (displayedDateMillis != null) {
            val heuteStart = getStartOfDay(System.currentTimeMillis())
            val viewDateStart = displayedDateMillis?.let { 
                com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(it) 
            } ?: com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
            
            // Im TourPlanner: Buttons anzeigen mit Farben basierend auf Status
            val activeColor = ContextCompat.getColor(context, R.color.button_active) // Orange
            val inactiveColor = ContextCompat.getColor(context, R.color.button_inactive) // Grau
            
            // Prüfe welche Buttons gedrückt wurden
            val pressedButton = pressedButtons[customer.id]
            
            // A (Abholung): Prüfe ob am angezeigten Tag ein Abholungstermin fällig ist ODER überfällig
            val abholungDatumHeute = getAbholungDatum?.invoke(customer) ?: 0L
            val hatAbholungHeute = abholungDatumHeute > 0
            
            // Prüfe ob es einen überfälligen Abholungstermin gibt, der am angezeigten Tag sichtbar sein soll
            val hatUeberfaelligeAbholung = if (!customer.abholungErfolgt) {
                val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                    customer = customer,
                    startDatum = heuteStart - java.util.concurrent.TimeUnit.DAYS.toMillis(365),
                    tageVoraus = 730
                )
                termine.any { termin ->
                    termin.typ == com.example.we2026_5.TerminTyp.ABHOLUNG &&
                    com.example.we2026_5.util.TerminBerechnungUtils.istUeberfaellig(termin.datum, heuteStart, customer.abholungErfolgt) &&
                    com.example.we2026_5.util.TerminBerechnungUtils.sollUeberfaelligAnzeigen(termin.datum, viewDateStart, heuteStart)
                }
            } else {
                false
            }
            
            // A-Button anzeigen wenn: Termin heute fällig ODER überfällig und am angezeigten Tag sichtbar
            val sollAButtonAnzeigen = (hatAbholungHeute || hatUeberfaelligeAbholung) && !isDone
            // Button ist aktiv wenn: Am tatsächlichen Termin-Tag ODER bei überfälligen Terminen (damit man erledigen kann)
            val aButtonAktiv = (hatAbholungHeute || hatUeberfaelligeAbholung) && !customer.abholungErfolgt
            // Am tatsächlichen Abholungstermin-Tag (nicht überfällig)
            val istAmTatsaechlichenAbholungTag = hatAbholungHeute && !hatUeberfaelligeAbholung
            
            holder.binding.btnAbholung.visibility = if (sollAButtonAnzeigen) View.VISIBLE else View.GONE
            // A-Button: Grüner Hintergrund wenn nicht geklickt, grauer Hintergrund wenn geklickt
            if (pressedButton == "A") {
                // Button wurde geklickt: Grauer Hintergrund, Text bleibt weiß
                holder.binding.btnAbholung.background = ContextCompat.getDrawable(context, R.drawable.button_gray)
                holder.binding.btnAbholung.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                // Button nicht geklickt: Grüner Hintergrund
                // Am tatsächlichen Termin-Tag: Voll sichtbar, bei überfälligen: auch aktiv aber ggf. etwas transparenter
                holder.binding.btnAbholung.background = ContextCompat.getDrawable(context, R.drawable.button_a_l)
                if (istAmTatsaechlichenAbholungTag) {
                    // Am tatsächlichen Termin-Tag: Voll sichtbar
                    holder.binding.btnAbholung.setTextColor(ContextCompat.getColor(context, R.color.white))
                    holder.binding.btnAbholung.alpha = 1.0f
                } else if (hatUeberfaelligeAbholung) {
                    // Bei überfälligen Terminen: Auch aktiv, aber leicht transparenter um zu zeigen dass es überfällig ist
                    holder.binding.btnAbholung.setTextColor(ContextCompat.getColor(context, R.color.white))
                    holder.binding.btnAbholung.alpha = 0.9f
                } else {
                    // Inaktiver Zustand (sollte nicht vorkommen wenn Button angezeigt wird)
                    holder.binding.btnAbholung.setTextColor(ContextCompat.getColor(context, R.color.white).let { Color.argb(128, Color.red(it), Color.green(it), Color.blue(it)) })
                    holder.binding.btnAbholung.alpha = 1.0f
                }
            }
            
            // L (Auslieferung): Prüfe ob am angezeigten Tag ein Auslieferungstermin fällig ist ODER überfällig
            val auslieferungDatumHeute = getAuslieferungDatum?.invoke(customer) ?: 0L
            val hatAuslieferungHeute = auslieferungDatumHeute > 0
            
            // Prüfe ob es einen überfälligen Auslieferungstermin gibt, der am angezeigten Tag sichtbar sein soll
            val hatUeberfaelligeAuslieferung = if (!customer.auslieferungErfolgt) {
                val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                    customer = customer,
                    startDatum = heuteStart - java.util.concurrent.TimeUnit.DAYS.toMillis(365),
                    tageVoraus = 730
                )
                termine.any { termin ->
                    termin.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG &&
                    com.example.we2026_5.util.TerminBerechnungUtils.istUeberfaellig(termin.datum, heuteStart, customer.auslieferungErfolgt) &&
                    com.example.we2026_5.util.TerminBerechnungUtils.sollUeberfaelligAnzeigen(termin.datum, viewDateStart, heuteStart)
                }
            } else {
                false
            }
            
            // L-Button anzeigen wenn: Termin heute fällig ODER überfällig und am angezeigten Tag sichtbar
            val sollLButtonAnzeigen = (hatAuslieferungHeute || hatUeberfaelligeAuslieferung) && !isDone
            // Button ist aktiv wenn: Am tatsächlichen Termin-Tag ODER bei überfälligen Terminen (damit man erledigen kann)
            val lButtonAktiv = (hatAuslieferungHeute || hatUeberfaelligeAuslieferung) && !customer.auslieferungErfolgt
            // Am tatsächlichen Auslieferungstermin-Tag (nicht überfällig)
            val istAmTatsaechlichenAuslieferungTag = hatAuslieferungHeute && !hatUeberfaelligeAuslieferung
            
            holder.binding.btnAuslieferung.visibility = if (sollLButtonAnzeigen) View.VISIBLE else View.GONE
            // L-Button: Grüner Hintergrund wenn nicht geklickt, grauer Hintergrund wenn geklickt
            if (pressedButton == "L") {
                // Button wurde geklickt: Grauer Hintergrund, Text bleibt weiß
                holder.binding.btnAuslieferung.background = ContextCompat.getDrawable(context, R.drawable.button_gray)
                holder.binding.btnAuslieferung.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                // Button nicht geklickt: Grüner Hintergrund
                // Am tatsächlichen Termin-Tag: Voll sichtbar, bei überfälligen: auch aktiv aber ggf. etwas transparenter
                holder.binding.btnAuslieferung.background = ContextCompat.getDrawable(context, R.drawable.button_a_l)
                if (istAmTatsaechlichenAuslieferungTag) {
                    // Am tatsächlichen Termin-Tag: Voll sichtbar
                    holder.binding.btnAuslieferung.setTextColor(ContextCompat.getColor(context, R.color.white))
                    holder.binding.btnAuslieferung.alpha = 1.0f
                } else if (hatUeberfaelligeAuslieferung) {
                    // Bei überfälligen Terminen: Auch aktiv, aber leicht transparenter um zu zeigen dass es überfällig ist
                    holder.binding.btnAuslieferung.setTextColor(ContextCompat.getColor(context, R.color.white))
                    holder.binding.btnAuslieferung.alpha = 0.9f
                } else {
                    // Inaktiver Zustand (sollte nicht vorkommen wenn Button angezeigt wird)
                    holder.binding.btnAuslieferung.setTextColor(ContextCompat.getColor(context, R.color.white).let { Color.argb(128, Color.red(it), Color.green(it), Color.blue(it)) })
                    holder.binding.btnAuslieferung.alpha = 1.0f
                }
            }
            
            // V (Verschieben): Aktiv wenn verschobenAufDatum > 0 ODER verschobeneTermine vorhanden
            // Prüfe ob am angezeigten Tag ein verschobener Termin vorhanden ist
            val hatVerschobenenTerminHeute = customer.verschobeneTermine.any { verschoben ->
                val originalStart = com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(verschoben.originalDatum)
                val verschobenStart = com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(verschoben.verschobenAufDatum)
                originalStart == viewDateStart || verschobenStart == viewDateStart
            }
            val vButtonAktiv = customer.verschobenAufDatum > 0 || hatVerschobenenTerminHeute
            
            holder.binding.btnVerschieben.visibility = if (!isDone && vButtonAktiv) View.VISIBLE else View.GONE
            // V-Button: Immer hellroter Hintergrund
            holder.binding.btnVerschieben.background = ContextCompat.getDrawable(context, R.drawable.button_v)
            // Textfarbe: Weiß wenn aktiv, sonst etwas transparenter
            holder.binding.btnVerschieben.setTextColor(if (vButtonAktiv) ContextCompat.getColor(context, R.color.white) else ContextCompat.getColor(context, R.color.white).let { Color.argb(128, Color.red(it), Color.green(it), Color.blue(it)) })
            
            // U (Urlaub): Aktiv wenn urlaubVon > 0 und urlaubBis > 0 UND ein Termin am angezeigten Tag im Urlaub liegt
            val hatTerminImUrlaub = if (customer.urlaubVon > 0 && customer.urlaubBis > 0) {
                val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                    customer = customer,
                    startDatum = viewDateStart - java.util.concurrent.TimeUnit.DAYS.toMillis(1),
                    tageVoraus = 2
                )
                termine.any { termin ->
                    com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(termin.datum) == viewDateStart &&
                    com.example.we2026_5.util.TerminBerechnungUtils.istTerminImUrlaub(termin.datum, customer.urlaubVon, customer.urlaubBis)
                }
            } else {
                false
            }
            val uButtonAktiv = customer.urlaubVon > 0 && customer.urlaubBis > 0 && hatTerminImUrlaub
            
            holder.binding.btnUrlaub.visibility = if (!isDone && uButtonAktiv) View.VISIBLE else View.GONE
            // U-Button: Immer oranger Hintergrund
            holder.binding.btnUrlaub.background = ContextCompat.getDrawable(context, R.drawable.button_u)
            // Textfarbe: Weiß wenn aktiv, sonst etwas transparenter
            holder.binding.btnUrlaub.setTextColor(if (uButtonAktiv) ContextCompat.getColor(context, R.color.white) else ContextCompat.getColor(context, R.color.white).let { Color.argb(128, Color.red(it), Color.green(it), Color.blue(it)) })
            
            holder.binding.btnRueckgaengig.visibility = if (isDone) View.VISIBLE else View.GONE
        } else {
            // In CustomerManager: Buttons ausblenden
            holder.binding.btnAbholung.visibility = View.GONE
            holder.binding.btnAuslieferung.visibility = View.GONE
            holder.binding.btnVerschieben.visibility = View.GONE
            holder.binding.btnUrlaub.visibility = View.GONE
            holder.binding.btnRueckgaengig.visibility = View.GONE
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

        val heuteStart = com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
        val viewDateStart = displayedDateMillis?.let { 
            com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(it) 
        } ?: heuteStart

        // WICHTIG: Überfällige Termine nicht in der Zukunft anzeigen
        if (viewDateStart > heuteStart) {
            // In der Zukunft: Keine überfälligen Termine anzeigen
            holder.binding.tvStatusLabel.visibility = View.GONE
            return
        }

        val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
        
        // NEUE STRUKTUR: Verwende TerminBerechnungUtils für korrekte Überfällig-Prüfung
        val istUeberfaellig = if (customer.intervalle.isNotEmpty()) {
            val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                startDatum = heuteStart - TimeUnit.DAYS.toMillis(365),
                tageVoraus = 730
            )
            termine.any { termin ->
                com.example.we2026_5.util.TerminBerechnungUtils.istUeberfaellig(
                    terminDatum = termin.datum,
                    aktuellesDatum = heuteStart,
                    erledigt = false
                ) && com.example.we2026_5.util.TerminBerechnungUtils.sollUeberfaelligAnzeigen(
                    terminDatum = termin.datum,
                    anzeigeDatum = viewDateStart,
                    aktuellesDatum = heuteStart
                )
            }
        } else {
            // ALTE STRUKTUR: Rückwärtskompatibilität
            val faelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
            !isDone && faelligAm < heuteStart && faelligAm > 0 && viewDateStart >= faelligAm && viewDateStart <= heuteStart
        }
        
        val showAsOverdue = istUeberfaellig && !isDone

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

    private fun handleAbholung(customer: Customer) {
        if (customer.abholungErfolgt) return
        onAbholung?.invoke(customer)
        // Button-Zustand bleibt bis Daten neu geladen werden (wird in TourPlannerActivity gemacht)
    }

    private fun handleAuslieferung(customer: Customer) {
        if (customer.auslieferungErfolgt) return
        onAuslieferung?.invoke(customer)
        // Button-Zustand bleibt bis Daten neu geladen werden
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
                    // Button-Zustand zurücksetzen nach Aktion
                    pressedButtons.remove(customer.id)
                    val position = items.indexOfFirst { it is ListItem.CustomerItem && it.customer.id == customer.id }
                    if (position != -1) notifyItemChanged(position)
                }
                .setNeutralButton("Alle zukünftigen Termine") { _, _ ->
                    // Alle zukünftigen Termine verschieben
                    onVerschieben?.invoke(customer, newDate, true)
                    pressedButtons.remove(customer.id)
                    val position = items.indexOfFirst { it is ListItem.CustomerItem && it.customer.id == customer.id }
                    if (position != -1) notifyItemChanged(position)
                }
                .setNegativeButton("Abbrechen") { _, _ ->
                    // Bei Abbrechen: Button-Zustand zurücksetzen
                    pressedButtons.remove(customer.id)
                    val position = items.indexOfFirst { it is ListItem.CustomerItem && it.customer.id == customer.id }
                    if (position != -1) notifyItemChanged(position)
                }
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
                    // Button-Zustand zurücksetzen nach Aktion
                    pressedButtons.remove(customer.id)
                    val position = items.indexOfFirst { it is ListItem.CustomerItem && it.customer.id == customer.id }
                    if (position != -1) notifyItemChanged(position)
                } else {
                    Toast.makeText(context, "Enddatum muss nach Startdatum sein!", Toast.LENGTH_SHORT).show()
                    // Bei Fehler: Button-Zustand zurücksetzen
                    pressedButtons.remove(customer.id)
                    val position = items.indexOfFirst { it is ListItem.CustomerItem && it.customer.id == customer.id }
                    if (position != -1) notifyItemChanged(position)
                }
            }

            DatePickerDialog(context, dateSetListenerBis, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                setTitle("Urlaub bis")
                setOnCancelListener {
                    // Bei Abbrechen: Button-Zustand zurücksetzen
                    pressedButtons.remove(customer.id)
                    val position = items.indexOfFirst { it is ListItem.CustomerItem && it.customer.id == customer.id }
                    if (position != -1) notifyItemChanged(position)
                }
                show()
            }
        }

            DatePickerDialog(context, dateSetListenerVon, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                setTitle("Urlaub von")
                setOnCancelListener {
                    // Bei Abbrechen: Button-Zustand zurücksetzen
                    pressedButtons.remove(customer.id)
                    val position = items.indexOfFirst { it is ListItem.CustomerItem && it.customer.id == customer.id }
                    if (position != -1) notifyItemChanged(position)
                }
                show()
            }
        }
    }
