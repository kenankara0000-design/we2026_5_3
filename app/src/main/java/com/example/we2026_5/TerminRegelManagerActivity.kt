package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.databinding.ActivityTerminRegelManagerBinding
import com.example.we2026_5.util.TerminRegelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class TerminRegelManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTerminRegelManagerBinding
    private val regelRepository: TerminRegelRepository by inject()
    private val customerRepository: CustomerRepository by inject()
    private lateinit var adapter: TerminRegelAdapter
    private val regeln = mutableListOf<TerminRegel>()
    private var pressedHeaderButton: String? = null // "NeueRegel"
    private var customerId: String? = null // ID des Kunden, für den eine Regel ausgewählt werden soll

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminRegelManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Customer-ID aus Intent holen (falls vorhanden)
        customerId = intent.getStringExtra("CUSTOMER_ID")

        setupRecyclerView()
        setupClickListeners()
        
        // Initial: Button-Zustand setzen
        updateHeaderButtonState()
    }

    override fun onResume() {
        super.onResume()
        setupRegelnFlow()
        // Button-Zustand zurücksetzen wenn von TerminRegelErstellenActivity zurückgekehrt
        if (pressedHeaderButton == "NeueRegel") {
            pressedHeaderButton = null
            updateHeaderButtonState()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Flow wird automatisch beendet durch lifecycleScope
    }

    private fun setupRecyclerView() {
        adapter = TerminRegelAdapter(
            regeln = regeln,
            onRegelClick = { regel ->
                // Wenn eine Customer-ID vorhanden ist, Regel auf Kunden anwenden
                // Sonst Info-Dialog zeigen
                if (customerId != null) {
                    wendeRegelAufKundeAn(regel)
                } else {
                    showRegelInfoDialog(regel)
                }
            },
            onRegelDelete = { /* Nicht mehr verwendet - Löschen nur über Bearbeiten-Fenster */ }
        )
        binding.rvRegeln.layoutManager = LinearLayoutManager(this)
        binding.rvRegeln.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnNewRegel.setOnClickListener {
            pressedHeaderButton = "NeueRegel"
            updateHeaderButtonState()
            val intent = Intent(this, TerminRegelErstellenActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun updateHeaderButtonState() {
        val activeBackgroundColor = ContextCompat.getColor(this, R.color.status_warning) // Orange
        val inactiveBackgroundColor = ContextCompat.getColor(this, R.color.button_blue) // Blau
        
        if (pressedHeaderButton == "NeueRegel") {
            // FAB: Orange wenn aktiv
            binding.btnNewRegel.backgroundTintList = android.content.res.ColorStateList.valueOf(activeBackgroundColor)
        } else {
            // FAB: Blau wenn nicht aktiv
            binding.btnNewRegel.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveBackgroundColor)
        }
    }

    private fun setupRegelnFlow() {
        regelRepository.getAllRegelnFlow()
            .onEach { allRegeln ->
                regeln.clear()
                regeln.addAll(allRegeln)
                adapter.updateRegeln(regeln)
                
                if (regeln.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.rvRegeln.visibility = View.GONE
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.rvRegeln.visibility = View.VISIBLE
                }
            }
            .launchIn(lifecycleScope)
    }
    
    private fun loadRegeln() {
        // Wird nicht mehr verwendet - Flow übernimmt
        setupRegelnFlow()
    }

    // Delete-Funktionen entfernt - Löschen nur über Bearbeiten-Fenster möglich

    private fun showRegelInfoDialog(regel: TerminRegel) {
        // Regel immer aktuell aus der Datenbank laden, um die neueste Verwendungsanzahl zu bekommen
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val aktuelleRegel = regelRepository.getRegelById(regel.id) ?: regel
                showRegelInfoDialogInternal(aktuelleRegel)
            } catch (e: Exception) {
                // Bei Fehler die übergebene Regel verwenden
                showRegelInfoDialogInternal(regel)
            }
        }
    }
    
    private fun showRegelInfoDialogInternal(regel: TerminRegel) {
        val wochentage = arrayOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
        
        val infoText = buildString {
            append("Name: ${regel.name}\n\n")
            
            if (regel.beschreibung.isNotEmpty()) {
                append("Beschreibung: ${regel.beschreibung}\n\n")
            }
            
            if (regel.wochentagBasiert) {
                append("Typ: Wochentag-basiert\n\n")
                
                if (regel.startDatum > 0) {
                    val startDateText = com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(regel.startDatum)
                    append("Startdatum: $startDateText\n")
                }
                
                if (regel.abholungWochentag >= 0) {
                    append("Abholung: ${wochentage[regel.abholungWochentag]}\n")
                }
                
                if (regel.auslieferungWochentag >= 0) {
                    append("Auslieferung: ${wochentage[regel.auslieferungWochentag]}\n")
                }
                
                // Start-Woche Option wurde entfernt - Berechnung erfolgt automatisch basierend auf Startdatum
                append("\n")
            } else {
                append("Typ: Datum-basiert\n\n")
                
                val abholungText = if (regel.abholungDatum > 0) {
                    com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(regel.abholungDatum)
                } else {
                    "Heute"
                }
                append("Abholung: $abholungText\n")
                
                val auslieferungText = if (regel.auslieferungDatum > 0) {
                    com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(regel.auslieferungDatum)
                } else {
                    "Heute"
                }
                append("Auslieferung: $auslieferungText\n\n")
            }
            
            if (regel.wiederholen) {
                append("Wiederholen: Ja\n")
                append("Intervall: Alle ${regel.intervallTage} Tage\n")
                if (regel.intervallAnzahl > 0) {
                    append("Anzahl: ${regel.intervallAnzahl} Wiederholungen\n")
                } else {
                    append("Anzahl: Unbegrenzt\n")
                }
            } else {
                append("Wiederholen: Nein\n")
            }
            
            append("\nVerwendungsanzahl: ${regel.verwendungsanzahl}x")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Regel-Informationen")
            .setMessage(infoText)
            .setPositiveButton("Bearbeiten") { _, _ ->
                val intent = Intent(this, TerminRegelErstellenActivity::class.java).apply {
                    putExtra("REGEL_ID", regel.id)
                }
                startActivity(intent)
            }
            .setNegativeButton("Schließen", null)
            .show()
    }
    
    /**
     * Wendet eine Regel auf einen Kunden an.
     * Prüft, ob der Kunde bereits Intervalle hat und fragt dann, ob hinzufügen oder ersetzen.
     */
    private fun wendeRegelAufKundeAn(regel: TerminRegel) {
        val id = customerId ?: return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val customer = customerRepository.getCustomerById(id)
                if (customer == null) {
                    Toast.makeText(this@TerminRegelManagerActivity, "Kunde nicht gefunden", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Prüfen, ob Kunde bereits Intervalle hat
                val hatBereitsIntervalle = customer.intervalle.isNotEmpty()
                
                if (hatBereitsIntervalle) {
                    // Kunde hat bereits Intervalle - Dialog: Hinzufügen oder Ersetzen?
                    val bestehendeRegelnText = customer.intervalle.joinToString("\n") { intervall ->
                        val abholungText = if (intervall.abholungDatum > 0) {
                            com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(intervall.abholungDatum)
                        } else "Heute"
                        val auslieferungText = if (intervall.auslieferungDatum > 0) {
                            com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(intervall.auslieferungDatum)
                        } else "Heute"
                        "• Abholung: $abholungText, Auslieferung: $auslieferungText"
                    }
                    
                    AlertDialog.Builder(this@TerminRegelManagerActivity)
                        .setTitle("Regel anwenden")
                        .setMessage("Der Kunde hat bereits ${customer.intervalle.size} Termin-Regel(n):\n\n$bestehendeRegelnText\n\nNeue Regel: ${regel.name}\n\nMöchten Sie diese Regel hinzufügen oder die bestehenden Regeln ersetzen?")
                        .setPositiveButton("Hinzufügen") { _, _ ->
                            regelHinzufuegenMitBestaetigung(customer, regel)
                        }
                        .setNeutralButton("Ersetzen") { _, _ ->
                            regelErsetzenMitBestaetigung(customer, regel, bestehendeRegelnText)
                        }
                        .setNegativeButton("Abbrechen", null)
                        .show()
                } else {
                    // Kunde hat keine Intervalle - Bestätigung vor dem Hinzufügen
                    regelHinzufuegenMitBestaetigung(customer, regel)
                }
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelManagerActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Zeigt Bestätigungsdialog vor dem Hinzufügen einer Regel.
     */
    private fun regelHinzufuegenMitBestaetigung(customer: Customer, regel: TerminRegel) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Berechne das neue Intervall, um Details anzuzeigen
                val neuesIntervall = TerminRegelManager.wendeRegelAufKundeAn(regel, customer)
                val abholungText = if (neuesIntervall.abholungDatum > 0) {
                    com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(neuesIntervall.abholungDatum)
                } else "Heute"
                val auslieferungText = if (neuesIntervall.auslieferungDatum > 0) {
                    com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(neuesIntervall.auslieferungDatum)
                } else "Heute"
                
                val hinzufuegenText = if (customer.intervalle.isNotEmpty()) {
                    "Diese Regel wird zu den bestehenden ${customer.intervalle.size} Regel(n) hinzugefügt:\n\n"
                } else {
                    "Diese Regel wird als erste Regel hinzugefügt:\n\n"
                }
                
                AlertDialog.Builder(this@TerminRegelManagerActivity)
                    .setTitle("Regel hinzufügen - Bestätigung")
                    .setMessage("$hinzufuegenText" +
                            "Regel: ${regel.name}\n" +
                            "Abholung: $abholungText\n" +
                            "Auslieferung: $auslieferungText\n" +
                            (if (neuesIntervall.wiederholen) "Wiederholen: Alle ${neuesIntervall.intervallTage} Tage\n" else "Wiederholen: Nein\n") +
                            "\nMöchten Sie fortfahren?")
                    .setPositiveButton("Ja, hinzufügen") { _, _ ->
                        regelHinzufuegen(customer, regel, neuesIntervall)
                    }
                    .setNegativeButton("Abbrechen", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelManagerActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Zeigt Bestätigungsdialog vor dem Ersetzen aller Regeln.
     */
    private fun regelErsetzenMitBestaetigung(customer: Customer, regel: TerminRegel, bestehendeRegelnText: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Berechne das neue Intervall, um Details anzuzeigen
                val neuesIntervall = TerminRegelManager.wendeRegelAufKundeAn(regel, customer)
                val abholungText = if (neuesIntervall.abholungDatum > 0) {
                    com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(neuesIntervall.abholungDatum)
                } else "Heute"
                val auslieferungText = if (neuesIntervall.auslieferungDatum > 0) {
                    com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(neuesIntervall.auslieferungDatum)
                } else "Heute"
                
                AlertDialog.Builder(this@TerminRegelManagerActivity)
                    .setTitle("Regeln ersetzen - Bestätigung")
                    .setMessage("ACHTUNG: Alle bestehenden Regeln werden gelöscht!\n\n" +
                            "Wird gelöscht (${customer.intervalle.size} Regel(n)):\n$bestehendeRegelnText\n\n" +
                            "Wird hinzugefügt:\n" +
                            "Regel: ${regel.name}\n" +
                            "Abholung: $abholungText\n" +
                            "Auslieferung: $auslieferungText\n" +
                            (if (neuesIntervall.wiederholen) "Wiederholen: Alle ${neuesIntervall.intervallTage} Tage\n" else "Wiederholen: Nein\n") +
                            "\nMöchten Sie wirklich fortfahren?")
                    .setPositiveButton("Ja, ersetzen") { _, _ ->
                        regelErsetzen(customer, regel, neuesIntervall)
                    }
                    .setNegativeButton("Abbrechen", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelManagerActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Fügt eine Regel zu den bestehenden Intervallen hinzu.
     */
    private fun regelHinzufuegen(customer: Customer, regel: TerminRegel, neuesIntervall: CustomerIntervall) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val neueIntervalle = customer.intervalle.toMutableList()
                neueIntervalle.add(neuesIntervall)
                
                val updates = mapOf("intervalle" to neueIntervalle)
                val success = customerRepository.updateCustomer(customer.id, updates)
                
                if (success) {
                    regelRepository.incrementVerwendungsanzahl(regel.id)
                    Toast.makeText(this@TerminRegelManagerActivity, "Regel '${regel.name}' hinzugefügt", Toast.LENGTH_SHORT).show()
                    finish() // Zurück zur CustomerDetailActivity
                } else {
                    Toast.makeText(this@TerminRegelManagerActivity, "Fehler beim Hinzufügen", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelManagerActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Ersetzt alle bestehenden Intervalle durch die neue Regel.
     */
    private fun regelErsetzen(customer: Customer, regel: TerminRegel, neuesIntervall: CustomerIntervall) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val neueIntervalle = listOf(neuesIntervall) // Alte Intervalle ersetzen
                
                val updates = mapOf("intervalle" to neueIntervalle)
                val success = customerRepository.updateCustomer(customer.id, updates)
                
                if (success) {
                    regelRepository.incrementVerwendungsanzahl(regel.id)
                    Toast.makeText(this@TerminRegelManagerActivity, "Regel '${regel.name}' ersetzt alle bestehenden Regeln", Toast.LENGTH_SHORT).show()
                    finish() // Zurück zur CustomerDetailActivity
                } else {
                    Toast.makeText(this@TerminRegelManagerActivity, "Fehler beim Ersetzen", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelManagerActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
