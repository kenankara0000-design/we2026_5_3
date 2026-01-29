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
        
        setupClickListeners(holder, customer)
        applyMultiSelectStyles(holder, customer)
        
        // Status-Styles ZULETZT anwenden (nach MultiSelect, damit sie nicht überschrieben werden)
        if (displayedDateMillis != null) {
            applyStatusStyles(holder, customer)
        } else {
            holder.binding.tvStatusLabel.visibility = View.GONE
        }
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
    
    private fun setupCompletionHints(holder: CustomerViewHolder, customer: Customer) {
        val heuteStart = getStartOfDay(System.currentTimeMillis())
        val viewDateStart = displayedDateMillis?.let {
            TerminBerechnungUtils.getStartOfDay(it)
        } ?: heuteStart
        val istHeute = viewDateStart == heuteStart
        
        val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt
        
        // Prüfe ob am angezeigten Tag (Tag X oder Tag Y) erledigt wurde oder überfällig war
        val abholungErledigtAmTag = customer.abholungErfolgt && customer.abholungErledigtAm > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.abholungErledigtAm) == viewDateStart
        val auslieferungErledigtAmTag = customer.auslieferungErfolgt && customer.auslieferungErledigtAm > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.auslieferungErledigtAm) == viewDateStart
        val warUeberfaelligAmTag = customer.faelligAmDatum > 0 &&
            TerminBerechnungUtils.getStartOfDay(customer.faelligAmDatum) == viewDateStart
        
        // Hinweise anzeigen wenn: am Tag erledigt ODER überfällig war am Tag ODER am Tag "Heute"
        val sollHinweiseAnzeigen = abholungErledigtAmTag || auslieferungErledigtAmTag || warUeberfaelligAmTag || istHeute
        
        if (!sollHinweiseAnzeigen || !isDone) {
            holder.binding.tvErledigungsHinweise.visibility = View.GONE
            holder.binding.tvUeberfaelligIndikator.visibility = View.GONE
            return
        }
        
        if (isDone) {
            val hinweise = mutableListOf<String>()
            val warUeberfaellig = customer.faelligAmDatum > 0
            
            // Überfällig-Indikator: anzeigen wenn überfällig war UND (am Tag erledigt ODER am Tag "Heute")
            if (warUeberfaelligAmTag || (warUeberfaellig && istHeute)) {
                holder.binding.tvUeberfaelligIndikator.visibility = View.VISIBLE
                val faelligStr = DateFormatter.formatDate(customer.faelligAmDatum)
                holder.binding.tvUeberfaelligIndikator.text = "Überfällig: $faelligStr"
            } else {
                holder.binding.tvUeberfaelligIndikator.visibility = View.GONE
            }
            
            // Abholung erledigt: anzeigen wenn
            // - am angezeigten Tag erledigt ODER
            // - am Tag "Heute" erledigt ODER
            // - am Tag X (Fälligkeitstag) überfällig war und inzwischen erledigt
            val sollAbholungHinweisAnzeigen = abholungErledigtAmTag || 
                (customer.abholungErfolgt && istHeute) ||
                (customer.abholungErfolgt && warUeberfaelligAmTag)
            if (sollAbholungHinweisAnzeigen) {
                if (customer.abholungErfolgt && customer.abholungZeitstempel > 0) {
                    val datumStr = DateFormatter.formatDateTime(customer.abholungZeitstempel)
                    hinweise.add("Abholung: $datumStr")
                } else if (customer.abholungErfolgt && customer.abholungErledigtAm > 0) {
                    val erledigtStr = DateFormatter.formatDate(customer.abholungErledigtAm)
                    hinweise.add("Abholung: $erledigtStr")
                }
            }
            
            // Auslieferung erledigt: gleiche Logik wie Abholung
            val sollAuslieferungHinweisAnzeigen = auslieferungErledigtAmTag || 
                (customer.auslieferungErfolgt && istHeute) ||
                (customer.auslieferungErfolgt && warUeberfaelligAmTag)
            if (sollAuslieferungHinweisAnzeigen) {
                if (customer.auslieferungErfolgt && customer.auslieferungZeitstempel > 0) {
                    val datumStr = DateFormatter.formatDateTime(customer.auslieferungZeitstempel)
                    hinweise.add("Auslieferung: $datumStr")
                } else if (customer.auslieferungErfolgt && customer.auslieferungErledigtAm > 0) {
                    val erledigtStr = DateFormatter.formatDate(customer.auslieferungErledigtAm)
                    hinweise.add("Auslieferung: $erledigtStr")
                }
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
            
            // Ü-Button (Überfällig) - prüfe mit gleicher Logik wie TourDataProcessor
            val hatUeberfaelligeAbholungFuerUeButton = run {
                val alleTermine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                    customer = customer,
                    startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365),
                    tageVoraus = 730
                )
                alleTermine.any { termin ->
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
            }
            
            val hatUeberfaelligeAuslieferungFuerUeButton = run {
                val alleTermine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                    customer = customer,
                    startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365),
                    tageVoraus = 730
                )
                alleTermine.any { termin ->
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
            }
            
            val hatUeberfaelligeTermine = hatUeberfaelligeAbholungFuerUeButton || hatUeberfaelligeAuslieferungFuerUeButton
            val istZukunft = viewDateStart > heuteStart
            
            // Ü-Button und Pfeil entfernt - immer versteckt
            holder.binding.btnUeberfaellig.visibility = View.GONE
            holder.binding.arrowUeberfaellig.visibility = View.GONE
            
            // Überfällig-Hinweis: nur wenn NICHT in der Zukunft (Zukunft hat keine überfälligen Termine)
            if (!istZukunft && hatUeberfaelligeTermine) {
                val ueberfaelligeTermineInfo = mutableListOf<String>()
                val ueberfaelligeDaten = mutableSetOf<Long>()
                
                // Sammle überfällige Termine mit Datum
                val alleTermine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                    customer = customer,
                    startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365),
                    tageVoraus = 730
                )
                
                alleTermine.forEach { termin ->
                    val terminStart = TerminBerechnungUtils.getStartOfDay(termin.datum)
                    val istAmTagXFaellig = terminStart == viewDateStart
                    val warVorHeuteFaellig = terminStart < heuteStart
                    val istHeute = viewDateStart == heuteStart
                    val istHeuteFaellig = terminStart == heuteStart
                    val istUeberfaellig = (istAmTagXFaellig || (warVorHeuteFaellig && istHeute)) && !istHeuteFaellig
                    
                    if (istUeberfaellig) {
                        val terminTyp = when (termin.typ) {
                            com.example.we2026_5.TerminTyp.ABHOLUNG -> "A"
                            com.example.we2026_5.TerminTyp.AUSLIEFERUNG -> "L"
                            else -> ""
                        }
                        val istNichtErledigt = (termin.typ == com.example.we2026_5.TerminTyp.ABHOLUNG && !customer.abholungErfolgt) ||
                                (termin.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG && !customer.auslieferungErfolgt)
                        
                        if (terminTyp.isNotEmpty() && istNichtErledigt) {
                            ueberfaelligeTermineInfo.add(terminTyp)
                            ueberfaelligeDaten.add(terminStart)
                        }
                    }
                }
                
                if (ueberfaelligeTermineInfo.isNotEmpty() && ueberfaelligeDaten.isNotEmpty()) {
                    val terminTypenDistinct = ueberfaelligeTermineInfo.distinct()
                    val hatA = terminTypenDistinct.contains("A")
                    val hatL = terminTypenDistinct.contains("L")
                    val aeltestesDatum = ueberfaelligeDaten.minOrNull() ?: 0L
                    val datumStr = if (aeltestesDatum > 0) DateFormatter.formatDate(aeltestesDatum) else ""
                    
                    // A-Button anzeigen wenn A überfällig - rot machen
                    if (hatA) {
                        holder.binding.btnUeberfaelligA.visibility = View.VISIBLE
                        holder.binding.btnUeberfaelligA.background = ContextCompat.getDrawable(context, R.drawable.button_a_overdue)
                    } else {
                        holder.binding.btnUeberfaelligA.visibility = View.GONE
                    }
                    // L-Button anzeigen wenn L überfällig - rot machen
                    if (hatL) {
                        holder.binding.btnUeberfaelligL.visibility = View.VISIBLE
                        holder.binding.btnUeberfaelligL.background = ContextCompat.getDrawable(context, R.drawable.button_l_overdue)
                    } else {
                        holder.binding.btnUeberfaelligL.visibility = View.GONE
                    }
                    // Datum anzeigen
                    holder.binding.tvUeberfaelligDatum.text = datumStr
                    holder.binding.tvUeberfaelligDatum.visibility = if (datumStr.isNotEmpty()) View.VISIBLE else View.GONE
                } else {
                    holder.binding.btnUeberfaelligA.visibility = View.GONE
                    holder.binding.btnUeberfaelligL.visibility = View.GONE
                    holder.binding.tvUeberfaelligDatum.visibility = View.GONE
                }
            } else {
                holder.binding.btnUeberfaelligA.visibility = View.GONE
                holder.binding.btnUeberfaelligL.visibility = View.GONE
                holder.binding.tvUeberfaelligDatum.visibility = View.GONE
            }
            
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
            // Button anzeigen wenn: heute fällig, überfällig, oder am angezeigten Tag erledigt (unabhängig von Überfälligkeit)
            val sollAButtonAnzeigen = hatAbholungHeute || hatUeberfaelligeAbholung || wurdeHeuteErledigt
            val istAmTatsaechlichenAbholungTag = hatAbholungHeute && !hatUeberfaelligeAbholung
            
            val istHeute = viewDateStart == heuteStart
            holder.binding.btnAbholung.visibility = if (sollAButtonAnzeigen) View.VISIBLE else View.GONE
            holder.binding.btnAbholung.isClickable = istHeute
            // WICHTIG: A-Button nur grau, wenn am Tag "Heute" erledigt wurde
            val wurdeHeuteErledigtA = istHeute && customer.abholungErledigtAm > 0 &&
                TerminBerechnungUtils.getStartOfDay(customer.abholungErledigtAm) == heuteStart
            if (wurdeHeuteErledigtA || pressedButton == "A") {
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
            
            val wurdeAmTagErledigtL = customer.auslieferungErledigtAm > 0 &&
                TerminBerechnungUtils.getStartOfDay(customer.auslieferungErledigtAm) == viewDateStart
            val warUeberfaelligL = customer.faelligAmDatum > 0
            // Button anzeigen wenn: heute fällig, überfällig, oder am angezeigten Tag erledigt (unabhängig von Überfälligkeit)
            val sollLButtonAnzeigen = hatAuslieferungHeute || hatUeberfaelligeAuslieferung || wurdeAmTagErledigtL
            val istAmTatsaechlichenAuslieferungTag = hatAuslieferungHeute && !hatUeberfaelligeAuslieferung
            
            holder.binding.btnAuslieferung.visibility = if (sollLButtonAnzeigen) View.VISIBLE else View.GONE
            holder.binding.btnAuslieferung.isClickable = istHeute
            // WICHTIG: L-Button nur grau, wenn am Tag "Heute" erledigt wurde
            val wurdeHeuteErledigtL = istHeute && customer.auslieferungErledigtAm > 0 &&
                TerminBerechnungUtils.getStartOfDay(customer.auslieferungErledigtAm) == heuteStart
            if (wurdeHeuteErledigtL || pressedButton == "L") {
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
            
            // Prüfe ob beide A und L heute relevant sind (fällig oder überfällig)
            val hatAbholungRelevantAmTag = hatAbholungHeute || hatUeberfaelligeAbholung
            val hatAuslieferungRelevantAmTag = hatAuslieferungHeute || hatUeberfaelligeAuslieferung
            val beideRelevantAmTag = hatAbholungRelevantAmTag && hatAuslieferungRelevantAmTag
            
            // Rückgängig-Button: Wenn beide A und L relevant, nur anzeigen wenn beide erledigt
            // WICHTIG: Rückgängig-Button nur am Tag "Heute" anzeigen
            val sollRueckgaengigAnzeigen = if (istHeute) {
                if (beideRelevantAmTag) {
                    hatErledigtenATerminAmDatum && hatErledigtenLTerminAmDatum
                } else {
                    hatErledigtenATerminAmDatum || hatErledigtenLTerminAmDatum
                }
            } else {
                false
            }
            
            holder.binding.btnRueckgaengig.visibility = if (sollRueckgaengigAnzeigen) View.VISIBLE else View.GONE
        } else {
            // In CustomerManager: Buttons ausblenden UND nicht klickbar machen
            holder.binding.btnAbholung.visibility = View.GONE
            holder.binding.btnAbholung.isClickable = false
            holder.binding.btnAuslieferung.visibility = View.GONE
            holder.binding.btnAuslieferung.isClickable = false
            holder.binding.btnVerschieben.visibility = View.GONE
            holder.binding.btnVerschieben.isClickable = false
            holder.binding.btnUrlaub.visibility = View.GONE
            holder.binding.btnUrlaub.isClickable = false
            holder.binding.btnRueckgaengig.visibility = View.GONE
            holder.binding.btnRueckgaengig.isClickable = false
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
            val alleTermine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365),
                tageVoraus = 730
            )
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
            val alleTermine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365),
                tageVoraus = 730
            )
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
        
        // "ERLEDIGT" Badge: Wenn beide A und L relevant, nur anzeigen wenn beide erledigt
        val sollAlsErledigtAnzeigen = if (beideRelevantAmTag) {
            customer.abholungErfolgt && customer.auslieferungErfolgt
        } else {
            isDone
        }
        
        // Gleiche Logik wie in setupButtonVisibility verwenden
        val istUeberfaellig = run {
            val alleTermine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365),
                tageVoraus = 730
            )
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
