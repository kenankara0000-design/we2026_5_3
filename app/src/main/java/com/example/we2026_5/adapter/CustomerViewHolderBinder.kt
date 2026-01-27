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
import com.example.we2026_5.ui.CustomerTypeButtonHelper
import java.util.Calendar
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
    private val onTerminClick: ((Customer, Long) -> Unit)?,
    private val onClick: (Customer) -> Unit,
    private val dialogHelper: CustomerDialogHelper,
    private val onAbholung: (Customer) -> Unit,
    private val onAuslieferung: (Customer) -> Unit,
    private val enableMultiSelectMode: () -> Unit,
    private val toggleCustomerSelection: (String, CustomerViewHolder) -> Unit
) {
    
    fun bind(holder: CustomerViewHolder, customer: Customer) {
        setupBasicInfo(holder, customer)
        setupCustomerTypeButton(holder, customer)
        setupNavigation(holder, customer)
        resetStyles(holder)
        
        // Status-Styles anwenden wenn im TourPlanner
        if (displayedDateMillis != null) {
            applyStatusStyles(holder, customer)
        } else {
            holder.binding.tvStatusLabel.visibility = View.GONE
        }
        
        setupClickListeners(holder, customer)
        applyMultiSelectStyles(holder, customer)
        setupCompletionHints(holder, customer)
        setupButtonVisibility(holder, customer)
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
        
        // Nächstes Tour-Datum berechnen und anzeigen
        val naechsteTour = customer.getFaelligAm()
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
        // Button-Handler
        holder.binding.btnAbholung.setOnClickListener {
            pressedButtons[customer.id] = "A"
            onAbholung(customer)
        }
        holder.binding.btnAuslieferung.setOnClickListener {
            pressedButtons[customer.id] = "L"
            onAuslieferung(customer)
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
    
    private fun setupCompletionHints(holder: CustomerViewHolder, customer: Customer) {
        val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt
        if (isDone) {
            val hinweise = mutableListOf<String>()
            val warUeberfaellig = customer.faelligAmDatum > 0
            
            // Überfällig-Indikator anzeigen
            if (warUeberfaellig) {
                holder.binding.tvUeberfaelligIndikator.visibility = View.VISIBLE
                val faelligStr = DateFormatter.formatDate(customer.faelligAmDatum)
                holder.binding.tvUeberfaelligIndikator.text = "Überfällig war: $faelligStr"
            } else {
                holder.binding.tvUeberfaelligIndikator.visibility = View.GONE
            }
            
            // Abholung erledigt
            if (customer.abholungErfolgt && customer.abholungZeitstempel > 0) {
                val datumStr = DateFormatter.formatDateTime(customer.abholungZeitstempel)
                hinweise.add("Abholung erledigt: $datumStr")
            } else if (customer.abholungErfolgt && customer.abholungErledigtAm > 0) {
                val erledigtStr = DateFormatter.formatDate(customer.abholungErledigtAm)
                hinweise.add("Abholung erledigt: $erledigtStr")
            }
            
            // Auslieferung erledigt
            if (customer.auslieferungErfolgt && customer.auslieferungZeitstempel > 0) {
                val datumStr = DateFormatter.formatDateTime(customer.auslieferungZeitstempel)
                hinweise.add("Auslieferung erledigt: $datumStr")
            } else if (customer.auslieferungErfolgt && customer.auslieferungErledigtAm > 0) {
                val erledigtStr = DateFormatter.formatDate(customer.auslieferungErledigtAm)
                hinweise.add("Auslieferung erledigt: $erledigtStr")
            }
            
            if (hinweise.isNotEmpty()) {
                holder.binding.tvErledigungsHinweise.text = hinweise.joinToString("\n")
                holder.binding.tvErledigungsHinweise.visibility = View.VISIBLE
            } else {
                holder.binding.tvErledigungsHinweise.visibility = View.GONE
            }
        } else {
            holder.binding.tvErledigungsHinweise.visibility = View.GONE
            holder.binding.tvUeberfaelligIndikator.visibility = View.GONE
        }
    }
    
    private fun setupButtonVisibility(holder: CustomerViewHolder, customer: Customer) {
        if (displayedDateMillis != null) {
            val heuteStart = getStartOfDay(System.currentTimeMillis())
            val viewDateStart = displayedDateMillis?.let {
                TerminBerechnungUtils.getStartOfDay(it)
            } ?: TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
            
            val pressedButton = pressedButtons[customer.id]
            
            // A (Abholung) Button
            val abholungDatumHeute = getAbholungDatum?.invoke(customer) ?: 0L
            val hatAbholungHeute = abholungDatumHeute > 0
            
            val hatUeberfaelligeAbholung = run {
                val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                    customer = customer,
                    startDatum = heuteStart - TimeUnit.DAYS.toMillis(365),
                    tageVoraus = 730
                )
                termine.any { termin ->
                    termin.typ == com.example.we2026_5.TerminTyp.ABHOLUNG &&
                    TerminFilterUtils.istUeberfaellig(termin.datum, heuteStart, customer.abholungErfolgt) &&
                    TerminFilterUtils.sollUeberfaelligAnzeigen(termin.datum, viewDateStart, heuteStart)
                }
            }
            
            val wurdeHeuteErledigt = customer.abholungErledigtAm > 0 &&
                TerminBerechnungUtils.getStartOfDay(customer.abholungErledigtAm) == viewDateStart
            val warUeberfaellig = customer.faelligAmDatum > 0
            val sollAButtonAnzeigen = hatAbholungHeute || hatUeberfaelligeAbholung || (wurdeHeuteErledigt && warUeberfaellig)
            val istAmTatsaechlichenAbholungTag = hatAbholungHeute && !hatUeberfaelligeAbholung
            
            holder.binding.btnAbholung.visibility = if (sollAButtonAnzeigen) View.VISIBLE else View.GONE
            if (customer.abholungErfolgt || pressedButton == "A") {
                holder.binding.btnAbholung.background = ContextCompat.getDrawable(context, R.drawable.button_gray)
                holder.binding.btnAbholung.setTextColor(ContextCompat.getColor(context, R.color.white))
                holder.binding.btnAbholung.alpha = 0.7f
            } else {
                holder.binding.btnAbholung.background = ContextCompat.getDrawable(context, R.drawable.button_a_glossy)
                if (istAmTatsaechlichenAbholungTag) {
                    holder.binding.btnAbholung.setTextColor(ContextCompat.getColor(context, R.color.white))
                    holder.binding.btnAbholung.alpha = 1.0f
                } else if (hatUeberfaelligeAbholung) {
                    holder.binding.btnAbholung.setTextColor(ContextCompat.getColor(context, R.color.white))
                    holder.binding.btnAbholung.alpha = 0.9f
                } else {
                    holder.binding.btnAbholung.setTextColor(
                        ContextCompat.getColor(context, R.color.white).let {
                            Color.argb(128, Color.red(it), Color.green(it), Color.blue(it))
                        }
                    )
                    holder.binding.btnAbholung.alpha = 1.0f
                }
            }
            
            // L (Auslieferung) Button
            val auslieferungDatumHeute = getAuslieferungDatum?.invoke(customer) ?: 0L
            val hatAuslieferungHeute = auslieferungDatumHeute > 0
            
            val hatUeberfaelligeAuslieferung = run {
                val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                    customer = customer,
                    startDatum = heuteStart - TimeUnit.DAYS.toMillis(365),
                    tageVoraus = 730
                )
                termine.any { termin ->
                    termin.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG &&
                    TerminFilterUtils.istUeberfaellig(termin.datum, heuteStart, customer.auslieferungErfolgt) &&
                    TerminFilterUtils.sollUeberfaelligAnzeigen(termin.datum, viewDateStart, heuteStart)
                }
            }
            
            val wurdeHeuteErledigtL = customer.auslieferungErledigtAm > 0 &&
                TerminBerechnungUtils.getStartOfDay(customer.auslieferungErledigtAm) == viewDateStart
            val warUeberfaelligL = customer.faelligAmDatum > 0
            val sollLButtonAnzeigen = hatAuslieferungHeute || hatUeberfaelligeAuslieferung || (wurdeHeuteErledigtL && warUeberfaelligL)
            val istAmTatsaechlichenAuslieferungTag = hatAuslieferungHeute && !hatUeberfaelligeAuslieferung
            
            holder.binding.btnAuslieferung.visibility = if (sollLButtonAnzeigen) View.VISIBLE else View.GONE
            if (customer.auslieferungErfolgt || pressedButton == "L") {
                holder.binding.btnAuslieferung.background = ContextCompat.getDrawable(context, R.drawable.button_gray)
                holder.binding.btnAuslieferung.setTextColor(ContextCompat.getColor(context, R.color.white))
                holder.binding.btnAuslieferung.alpha = 0.7f
            } else {
                holder.binding.btnAuslieferung.background = ContextCompat.getDrawable(context, R.drawable.button_l_glossy)
                if (istAmTatsaechlichenAuslieferungTag) {
                    holder.binding.btnAuslieferung.setTextColor(ContextCompat.getColor(context, R.color.white))
                    holder.binding.btnAuslieferung.alpha = 1.0f
                } else if (hatUeberfaelligeAuslieferung) {
                    holder.binding.btnAuslieferung.setTextColor(ContextCompat.getColor(context, R.color.white))
                    holder.binding.btnAuslieferung.alpha = 0.9f
                } else {
                    holder.binding.btnAuslieferung.setTextColor(
                        ContextCompat.getColor(context, R.color.white).let {
                            Color.argb(128, Color.red(it), Color.green(it), Color.blue(it))
                        }
                    )
                    holder.binding.btnAuslieferung.alpha = 1.0f
                }
            }
            
            // V (Verschieben) Button
            val hatVerschobenenTerminHeute = customer.verschobeneTermine.any { verschoben ->
                val originalStart = TerminBerechnungUtils.getStartOfDay(verschoben.originalDatum)
                val verschobenStart = TerminBerechnungUtils.getStartOfDay(verschoben.verschobenAufDatum)
                originalStart == viewDateStart || verschobenStart == viewDateStart
            }
            val vButtonAktiv = customer.verschobenAufDatum > 0 || hatVerschobenenTerminHeute
            val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt
            
            holder.binding.btnVerschieben.visibility = if (!isDone && vButtonAktiv) View.VISIBLE else View.GONE
            holder.binding.btnVerschieben.background = ContextCompat.getDrawable(context, R.drawable.button_v_glossy)
            holder.binding.btnVerschieben.setTextColor(
                if (vButtonAktiv) ContextCompat.getColor(context, R.color.white)
                else ContextCompat.getColor(context, R.color.white).let {
                    Color.argb(128, Color.red(it), Color.green(it), Color.blue(it))
                }
            )
            
            // U (Urlaub) Button
            val hatTerminImUrlaub = if (customer.urlaubVon > 0 && customer.urlaubBis > 0) {
                val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                    customer = customer,
                    startDatum = viewDateStart - TimeUnit.DAYS.toMillis(1),
                    tageVoraus = 2
                )
                termine.any { termin ->
                    TerminBerechnungUtils.getStartOfDay(termin.datum) == viewDateStart &&
                    TerminFilterUtils.istTerminImUrlaub(termin.datum, customer.urlaubVon, customer.urlaubBis)
                }
            } else {
                false
            }
            val uButtonAktiv = customer.urlaubVon > 0 && customer.urlaubBis > 0 && hatTerminImUrlaub
            
            holder.binding.btnUrlaub.visibility = if (!isDone && uButtonAktiv) View.VISIBLE else View.GONE
            holder.binding.btnUrlaub.background = ContextCompat.getDrawable(context, R.drawable.button_u_glossy)
            holder.binding.btnUrlaub.setTextColor(
                if (uButtonAktiv) ContextCompat.getColor(context, R.color.white)
                else ContextCompat.getColor(context, R.color.white).let {
                    Color.argb(128, Color.red(it), Color.green(it), Color.blue(it))
                }
            )
            
            // Rückgängig-Button
            val hatErledigtenATerminAmDatum = if (customer.abholungErfolgt) {
                val abholungErledigtAmStart = if (customer.abholungErledigtAm > 0) {
                    TerminBerechnungUtils.getStartOfDay(customer.abholungErledigtAm)
                } else {
                    0L
                }
                if (abholungErledigtAmStart > 0 && viewDateStart == abholungErledigtAmStart) {
                    true
                } else {
                    val abholungDatumHeute = getAbholungDatum?.invoke(customer) ?: 0L
                    abholungDatumHeute > 0 && TerminBerechnungUtils.getStartOfDay(abholungDatumHeute) == viewDateStart
                }
            } else {
                false
            }
            
            val hatErledigtenLTerminAmDatum = if (customer.auslieferungErfolgt) {
                val auslieferungErledigtAmStart = if (customer.auslieferungErledigtAm > 0) {
                    TerminBerechnungUtils.getStartOfDay(customer.auslieferungErledigtAm)
                } else {
                    0L
                }
                if (auslieferungErledigtAmStart > 0 && viewDateStart == auslieferungErledigtAmStart) {
                    true
                } else {
                    val auslieferungDatumHeute = getAuslieferungDatum?.invoke(customer) ?: 0L
                    auslieferungDatumHeute > 0 && TerminBerechnungUtils.getStartOfDay(auslieferungDatumHeute) == viewDateStart
                }
            } else {
                false
            }
            
            holder.binding.btnRueckgaengig.visibility = if (hatErledigtenATerminAmDatum || hatErledigtenLTerminAmDatum) View.VISIBLE else View.GONE
        } else {
            // In CustomerManager: Buttons ausblenden
            holder.binding.btnAbholung.visibility = View.GONE
            holder.binding.btnAuslieferung.visibility = View.GONE
            holder.binding.btnVerschieben.visibility = View.GONE
            holder.binding.btnUrlaub.visibility = View.GONE
            holder.binding.btnRueckgaengig.visibility = View.GONE
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
        
        val istUeberfaellig = if (customer.intervalle.isNotEmpty()) {
            val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                startDatum = heuteStart - TimeUnit.DAYS.toMillis(365),
                tageVoraus = 730
            )
            termine.any { termin ->
                TerminFilterUtils.istUeberfaellig(termin.datum, heuteStart, false) &&
                TerminFilterUtils.sollUeberfaelligAnzeigen(termin.datum, viewDateStart, heuteStart)
            }
        } else {
            val faelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum
                else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
            !isDone && faelligAm < heuteStart && faelligAm > 0 && viewDateStart >= faelligAm && viewDateStart <= heuteStart
        }
        
        val showAsOverdue = istUeberfaellig && !isDone
        val cardView = holder.itemView as? androidx.cardview.widget.CardView
        
        when {
            isDone -> {
                holder.binding.tvStatusLabel.text = "ERLEDIGT"
                holder.binding.tvStatusLabel.setBackgroundResource(R.drawable.status_badge_done)
                holder.binding.tvStatusLabel.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
                holder.binding.itemContainer.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.customer_done_bg))
                holder.binding.itemContainer.alpha = 0.8f
                cardView?.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.customer_done_bg))
            }
            showAsOverdue -> {
                holder.binding.tvStatusLabel.text = "ÜBERFÄLLIG"
                holder.binding.tvStatusLabel.setBackgroundResource(R.drawable.status_badge_overdue)
                holder.binding.tvStatusLabel.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
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
                if (customer.kundenArt == "Gewerblich" && displayedDateMillis != null) {
                    cardView?.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.customer_gewerblich_bg))
                } else {
                    cardView?.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.surface_white))
                }
            }
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
}
