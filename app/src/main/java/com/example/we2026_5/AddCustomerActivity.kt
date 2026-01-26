package com.example.we2026_5

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.databinding.ActivityAddCustomerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import org.koin.android.ext.android.inject
import android.widget.ArrayAdapter

class AddCustomerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCustomerBinding
    private val repository: CustomerRepository by inject()
    private val listeRepository: KundenListeRepository by inject()
    private var alleListen = listOf<com.example.we2026_5.KundenListe>()
    private var selectedListeId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCustomerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Liste-Auswahl Switch: Liste aktivieren/deaktivieren
        binding.switchListeAktivieren.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutListeAuswahl.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
            binding.cardListeAuswahl.visibility = android.view.View.VISIBLE // Card immer sichtbar
            
            // Reihenfolge-Feld nur anzeigen wenn Liste aktiviert ist
            binding.etReihenfolge.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
            
            if (isChecked) {
                loadListen()
            } else {
                selectedListeId = ""
            }
        }

        // Liste-Spinner
        binding.spinnerListe.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position > 0 && position <= alleListen.size) {
                    selectedListeId = alleListen[position - 1].id
                } else {
                    selectedListeId = ""
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedListeId = ""
            }
        }

        // Neue Liste erstellen Button
        binding.btnNeueListe.setOnClickListener {
            showNeueListeDialog()
        }

        binding.btnBack.setOnClickListener { finish() }
        
        // Initial: Listen laden (für beide Kunden-Arten verfügbar)
        loadListen()
        
        // Card immer sichtbar machen
        binding.cardListeAuswahl.visibility = android.view.View.VISIBLE

        binding.btnSaveCustomer.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                binding.etName.error = "Name fehlt"
                return@setOnClickListener
            }

            // Adresse und Telefon sind optional - keine Validierung
            val adresse = binding.etAdresse.text.toString().trim()
            val telefon = binding.etTelefon.text.toString().trim()

            // Kunden-Art bestimmen
            val kundenArt = if (binding.rbPrivat.isChecked) "Privat" else "Gewerblich"
            val listeAktiviert = binding.switchListeAktivieren.isChecked

            // Validierung: Wenn Liste aktiviert, muss eine Liste ausgewählt sein
            if (listeAktiviert && selectedListeId.isEmpty()) {
                Toast.makeText(this, "Bitte wählen Sie eine Liste aus", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Reihenfolge nur validieren wenn Liste aktiviert ist (Feld ist dann sichtbar)
            val reihenfolge = if (listeAktiviert && selectedListeId.isNotEmpty()) {
                val reihenfolgeInput = binding.etReihenfolge.text.toString().toIntOrNull() ?: 1
                when {
                    reihenfolgeInput < 1 -> {
                        binding.etReihenfolge.error = "Reihenfolge muss mindestens 1 sein"
                        return@setOnClickListener
                    }
                    else -> reihenfolgeInput
                }
            } else {
                1 // Standard-Reihenfolge wenn keine Liste aktiviert
            }
            
            // Duplikat-Prüfung: Reihenfolge (nur für Privat-Kunden in Listen)
            CoroutineScope(Dispatchers.Main).launch {
                val existingCustomer = if (listeAktiviert && selectedListeId.isNotEmpty() && kundenArt == "Privat") {
                    ValidationHelper.checkDuplicateReihenfolge(
                        repository = repository,
                        wochentag = 0, // Wochentag wird nicht mehr verwendet
                        reihenfolge = reihenfolge
                    )
                } else {
                    null // Keine Duplikat-Prüfung bei Gewerblich-Kunden oder Kunden ohne Liste
                }
                
                if (existingCustomer != null) {
                    runOnUiThread {
                        binding.etReihenfolge.error = "Reihenfolge $reihenfolge ist bereits von ${existingCustomer.name} belegt"
                        Toast.makeText(this@AddCustomerActivity, 
                            "Kunde '${existingCustomer.name}' hat bereits Reihenfolge $reihenfolge", 
                            Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                // Button sofort deaktivieren und visuelles Feedback geben
                runOnUiThread {
                    binding.btnSaveCustomer.isEnabled = false
                    binding.btnSaveCustomer.text = "Speichere..."
                    binding.btnSaveCustomer.alpha = 0.6f
                }
                
                // Für Kunden in Listen: Daten der Liste verwenden (keine eigenen Daten)
                val liste = if (listeAktiviert && selectedListeId.isNotEmpty()) {
                    alleListen.find { it.id == selectedListeId }
                } else {
                    null
                }
                
                val customerId = java.util.UUID.randomUUID().toString()
                val customer = Customer(
                    id = customerId,
                    name = name,
                    adresse = adresse,
                    telefon = telefon,
                    notizen = binding.etNotizen.text.toString().trim(),
                    // Kunden-Art und Liste
                    kundenArt = kundenArt,
                    listeId = if (listeAktiviert && selectedListeId.isNotEmpty()) selectedListeId else "",
                    // Termine: Für Kunden in Listen werden Daten der Liste verwendet (0 = wird ignoriert)
                    abholungDatum = if (liste != null) 0 else 0,
                    auslieferungDatum = if (liste != null) 0 else 0,
                    wiederholen = if (liste != null) true else false,
                    // Wiederholungs-Felder: Für Listen-Kunden werden Intervalle von der Liste verwaltet
                    intervallTage = if (liste != null) 0 else 0,
                    letzterTermin = if (liste != null) 0 else 0,
                    wochentag = 0, // Wochentag wird nicht mehr verwendet
                    reihenfolge = reihenfolge, // Wird bereits korrekt berechnet (1 wenn keine Liste, sonst eingegebener Wert)
                    istImUrlaub = false
                )

                // Speichern mit Retry-Logik
                val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                    operation = { 
                        repository.saveCustomer(customer)
                    },
                    context = this@AddCustomerActivity,
                    errorMessage = "Fehler beim Speichern. Bitte erneut versuchen.",
                    maxRetries = 3
                )
                
                runOnUiThread {
                    if (success == true) {
                        // Erfolg: Button-Text ändern und dann Activity schließen
                        binding.btnSaveCustomer.text = "✓ Gespeichert!"
                        binding.btnSaveCustomer.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            resources.getColor(com.example.we2026_5.R.color.status_done, theme)
                        )
                        binding.btnSaveCustomer.alpha = 1.0f
                        
                        // Kurz warten, damit der Benutzer das Feedback sieht
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            finish()
                        }, 800)
                    } else {
                        // Fehler: Button wieder aktivieren
                        binding.btnSaveCustomer.isEnabled = true
                        binding.btnSaveCustomer.text = "Speichern"
                        binding.btnSaveCustomer.alpha = 1.0f
                        // Toast wird bereits von FirebaseRetryHelper angezeigt
                    }
                }
            }
        }
    }

    private fun loadListen() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                alleListen = listeRepository.getAllListen()
                val listenNamen = mutableListOf<String>()
                listenNamen.add("Liste auswählen...")
                listenNamen.addAll(alleListen.map { it.name })
                
                val adapter = ArrayAdapter(this@AddCustomerActivity, android.R.layout.simple_spinner_item, listenNamen)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerListe.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(this@AddCustomerActivity, "Fehler beim Laden der Listen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showNeueListeDialog() {
        val etListeName = android.widget.EditText(this).apply {
            hint = "Listen-Name (z.B. Borna P)"
            setPadding(32, 16, 32, 16)
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Neue Liste erstellen")
            .setView(etListeName)
            .setPositiveButton("Weiter") { _, _ ->
                val listeName = etListeName.text.toString().trim()
                if (listeName.isEmpty()) {
                    Toast.makeText(this, "Bitte geben Sie einen Listen-Namen ein", Toast.LENGTH_SHORT).show()
                } else {
                    showListeWochentageDialog(listeName)
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
    
    private fun showListeWochentageDialog(listeName: String) {
        var selectedAbholungWT = -1
        var selectedAuslieferungWT = -1
        
        val wochentage = arrayOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
        
        val dialogView = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }
        
        val tvAbholung = android.widget.TextView(this).apply {
            text = "Abholung Wochentag:"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        }
        val spinnerAbholung = android.widget.Spinner(this)
        val adapterAbholung = ArrayAdapter(this, android.R.layout.simple_spinner_item, wochentage)
        adapterAbholung.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAbholung.adapter = adapterAbholung
        spinnerAbholung.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedAbholungWT = position
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        val tvAuslieferung = android.widget.TextView(this).apply {
            text = "Auslieferung Wochentag:"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        }
        val spinnerAuslieferung = android.widget.Spinner(this)
        val adapterAuslieferung = ArrayAdapter(this, android.R.layout.simple_spinner_item, wochentage)
        adapterAuslieferung.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAuslieferung.adapter = adapterAuslieferung
        spinnerAuslieferung.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedAuslieferungWT = position
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        dialogView.addView(tvAbholung)
        dialogView.addView(spinnerAbholung)
        dialogView.addView(tvAuslieferung)
        dialogView.addView(spinnerAuslieferung)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Wochentage für $listeName")
            .setView(dialogView)
            .setPositiveButton("Erstellen") { _, _ ->
                if (selectedAbholungWT >= 0 && selectedAuslieferungWT >= 0) {
                    val listeId = java.util.UUID.randomUUID().toString()
                    
                    // Wochentag in Datum umwandeln (nächster Wochentag)
                    val cal = Calendar.getInstance()
                    val heuteWochentag = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0=Montag
                    
                    // Abholungsdatum: Nächster ausgewählter Wochentag
                    var tageBisAbholung = selectedAbholungWT - heuteWochentag
                    if (tageBisAbholung <= 0) tageBisAbholung += 7
                    cal.add(Calendar.DAY_OF_YEAR, tageBisAbholung)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val abholungDatum = cal.timeInMillis
                    
                    // Auslieferungsdatum: Nächster ausgewählter Wochentag
                    cal.timeInMillis = System.currentTimeMillis()
                    val heuteWochentag2 = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
                    var tageBisAuslieferung = selectedAuslieferungWT - heuteWochentag2
                    if (tageBisAuslieferung <= 0) tageBisAuslieferung += 7
                    cal.add(Calendar.DAY_OF_YEAR, tageBisAuslieferung)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val auslieferungDatum = cal.timeInMillis
                    
                    // Standard-Intervall erstellen (wöchentlich)
                    val standardIntervall = com.example.we2026_5.ListeIntervall(
                        abholungDatum = abholungDatum,
                        auslieferungDatum = auslieferungDatum,
                        wiederholen = true,
                        intervallTage = 7 // Wöchentlich
                    )
                    
                    val neueListe = com.example.we2026_5.KundenListe(
                        id = listeId,
                        name = listeName,
                        intervalle = listOf(standardIntervall),
                        erstelltAm = System.currentTimeMillis()
                    )
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            listeRepository.saveListe(neueListe)
                            Toast.makeText(this@AddCustomerActivity, "Liste '$listeName' erstellt", Toast.LENGTH_SHORT).show()
                            loadListen()
                            // Neue Liste im Spinner auswählen
                            val position = alleListen.indexOfFirst { it.id == listeId } + 1
                            if (position > 0) {
                                binding.spinnerListe.setSelection(position)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@AddCustomerActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
    
}
