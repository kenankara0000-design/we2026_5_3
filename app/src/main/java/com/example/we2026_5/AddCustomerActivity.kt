package com.example.we2026_5

import android.app.DatePickerDialog
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
import java.util.concurrent.TimeUnit
import org.koin.android.ext.android.inject
import android.widget.ArrayAdapter

class AddCustomerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCustomerBinding
    private val repository: CustomerRepository by inject()
    private val listeRepository: KundenListeRepository by inject()
    private var selectedAbholungDatum: Long = 0
    private var selectedAuslieferungDatum: Long = 0
    private var selectedWochentag: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2 // 0=Montag
    private val wochentagButtons = mutableListOf<android.widget.Button>()
    private var alleListen = listOf<com.example.we2026_5.KundenListe>()
    private var selectedListeId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCustomerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Wochentag-Buttons initialisieren
        wochentagButtons.addAll(listOf(
            binding.btnMo, binding.btnDi, binding.btnMi, binding.btnDo,
            binding.btnFr, binding.btnSa, binding.btnSo
        ))
        
        // Standard-Wochentag setzen (heute)
        val heute = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        selectedWochentag = (heute + 5) % 7 // Calendar.SUNDAY=1 -> 0=Montag
        updateWochentagButtons()
        
        // Wochentag-Button Click-Handler
        binding.btnMo.setOnClickListener { selectWochentag(0) }
        binding.btnDi.setOnClickListener { selectWochentag(1) }
        binding.btnMi.setOnClickListener { selectWochentag(2) }
        binding.btnDo.setOnClickListener { selectWochentag(3) }
        binding.btnFr.setOnClickListener { selectWochentag(4) }
        binding.btnSa.setOnClickListener { selectWochentag(5) }
        binding.btnSo.setOnClickListener { selectWochentag(6) }

        // Initialisiere Datums-Buttons
        updateAbholungDateButtonText(System.currentTimeMillis())
        updateAuslieferungDateButtonText(System.currentTimeMillis())
        selectedAbholungDatum = System.currentTimeMillis()
        selectedAuslieferungDatum = System.currentTimeMillis()

        // Abholungsdatum-Picker
        binding.btnPickAbholungDate.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.timeInMillis = if (selectedAbholungDatum > 0) selectedAbholungDatum else System.currentTimeMillis()
            DatePickerDialog(this, { _, y, m, d ->
                val picked = Calendar.getInstance()
                picked.set(y, m, d, 0, 0, 0)
                selectedAbholungDatum = picked.timeInMillis
                updateAbholungDateButtonText(selectedAbholungDatum)
                
                // Wochentag automatisch basierend auf Abholungsdatum setzen (wenn wiederholen aktiviert)
                if (binding.cbWiederholen.isChecked) {
                    val dayOfWeek = picked.get(Calendar.DAY_OF_WEEK)
                    selectedWochentag = (dayOfWeek + 5) % 7
                    updateWochentagButtons()
                }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Auslieferungsdatum-Picker
        binding.btnPickAuslieferungDate.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.timeInMillis = if (selectedAuslieferungDatum > 0) selectedAuslieferungDatum else System.currentTimeMillis()
            DatePickerDialog(this, { _, y, m, d ->
                val picked = Calendar.getInstance()
                picked.set(y, m, d, 0, 0, 0)
                selectedAuslieferungDatum = picked.timeInMillis
                updateAuslieferungDateButtonText(selectedAuslieferungDatum)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Wiederholen-Checkbox: Sichtbarkeit von Intervall und Wochentag steuern
        binding.cbWiederholen.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutWiederholung.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
        }

        // Kunden-Art RadioButtons: Liste-Auswahl sichtbar/unsichtbar machen
        binding.rgKundenArt.setOnCheckedChangeListener { _, checkedId ->
            val isPrivat = checkedId == binding.rbPrivat.id
            binding.layoutListeAuswahl.visibility = if (isPrivat) android.view.View.VISIBLE else android.view.View.GONE
            
            // Liste-Spinner füllen wenn Privat gewählt
            if (isPrivat) {
                loadListen()
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
        
        // Initial: Listen laden (falls später Privat gewählt wird)
        loadListen()

        binding.btnSaveCustomer.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                binding.etName.error = "Name fehlt"
                return@setOnClickListener
            }

            // Adresse und Telefon sind optional - keine Validierung
            val adresse = binding.etAdresse.text.toString().trim()
            val telefon = binding.etTelefon.text.toString().trim()

            // Validierung: Abholungsdatum muss gesetzt sein
            if (selectedAbholungDatum == 0L) {
                Toast.makeText(this, "Bitte wählen Sie ein Abholungsdatum", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validierung: Auslieferungsdatum muss gesetzt sein
            if (selectedAuslieferungDatum == 0L) {
                Toast.makeText(this, "Bitte wählen Sie ein Auslieferungsdatum", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val kundenArt = if (binding.rbPrivat.isChecked) "Privat" else "Gewerblich"
            
            // Validierung: Wenn Privat, muss eine Liste ausgewählt sein
            if (kundenArt == "Privat" && selectedListeId.isEmpty()) {
                Toast.makeText(this, "Bitte wählen Sie eine Liste für Privat-Kunden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val wiederholen = binding.cbWiederholen.isChecked
            // Validierung: Wenn wiederholen aktiviert, Intervall und Wochentag prüfen
            val intervall = if (wiederholen) {
                val intervallInput = binding.etIntervallTage.text.toString().toIntOrNull() ?: 7
                when {
                    intervallInput < 1 -> {
                        binding.etIntervallTage.error = "Intervall muss mindestens 1 Tag sein"
                        return@setOnClickListener
                    }
                    intervallInput > 365 -> {
                        binding.etIntervallTage.error = "Intervall darf maximal 365 Tage sein"
                        return@setOnClickListener
                    }
                    else -> intervallInput
                }
            } else {
                7 // Wird nicht verwendet
            }
            
            val reihenfolgeInput = binding.etReihenfolge.text.toString().toIntOrNull() ?: 1
            val reihenfolge = when {
                reihenfolgeInput < 1 -> {
                    binding.etReihenfolge.error = "Reihenfolge muss mindestens 1 sein"
                    return@setOnClickListener
                }
                else -> reihenfolgeInput
            }
            
            // Wochentag bestimmen: Für Privat-Kunden aus Liste, sonst aus Abholungsdatum
            val calAbholung = Calendar.getInstance()
            calAbholung.timeInMillis = selectedAbholungDatum
            val abholungWochentag = if (kundenArt == "Privat" && selectedListeId.isNotEmpty()) {
                // Wochentag aus Liste holen
                val liste = alleListen.find { it.id == selectedListeId }
                liste?.abholungWochentag ?: (calAbholung.get(Calendar.DAY_OF_WEEK) + 5) % 7
            } else {
                (calAbholung.get(Calendar.DAY_OF_WEEK) + 5) % 7
            }
            
            // Duplikat-Prüfung: Wochentag + Reihenfolge (nur wenn wiederholen aktiviert)
            CoroutineScope(Dispatchers.Main).launch {
                val existingCustomer = if (wiederholen && kundenArt == "Privat") {
                    ValidationHelper.checkDuplicateReihenfolge(
                        repository = repository,
                        wochentag = abholungWochentag,
                        reihenfolge = reihenfolge
                    )
                } else {
                    null // Keine Duplikat-Prüfung bei einmaligen Terminen oder Gewerblich
                }
                
                if (existingCustomer != null) {
                    runOnUiThread {
                        binding.etReihenfolge.error = "Reihenfolge $reihenfolge ist bereits von ${existingCustomer.name} belegt"
                        Toast.makeText(this@AddCustomerActivity, 
                            "Kunde '${existingCustomer.name}' hat bereits Reihenfolge $reihenfolge am ${getWochentagName(abholungWochentag)}", 
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
                
                // letzterTermin berechnen (für wiederholende Termine)
                val letzterTermin = if (wiederholen) {
                    selectedAbholungDatum - TimeUnit.DAYS.toMillis(intervall.toLong())
                } else {
                    0 // Nicht verwendet bei einmaligen Terminen
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
                    listeId = if (kundenArt == "Privat") selectedListeId else "",
                    // Termine
                    abholungDatum = selectedAbholungDatum,
                    auslieferungDatum = selectedAuslieferungDatum,
                    wiederholen = wiederholen,
                    // Wiederholungs-Felder (nur wenn wiederholen = true)
                    intervallTage = intervall,
                    letzterTermin = letzterTermin,
                    wochentag = if (wiederholen) abholungWochentag else 0,
                    reihenfolge = if (wiederholen) reihenfolge else 1,
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
                        
                        // Kurz warten, damit der Benutzer das Feedback sieht
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            finish()
                        }, 500)
                    } else {
                        // Fehler: Button wieder aktivieren
                        binding.btnSaveCustomer.isEnabled = true
                        binding.btnSaveCustomer.text = "Speichern"
                        binding.btnSaveCustomer.alpha = 1.0f
                    }
                }
            }
        }
    }

    private fun updateAbholungDateButtonText(dateInMillis: Long) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dateInMillis
        binding.btnPickAbholungDate.text = "${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}.${cal.get(Calendar.YEAR)}"
    }
    
    private fun updateAuslieferungDateButtonText(dateInMillis: Long) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dateInMillis
        binding.btnPickAuslieferungDate.text = "${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}.${cal.get(Calendar.YEAR)}"
    }
    
    private fun selectWochentag(tag: Int) {
        selectedWochentag = tag
        updateWochentagButtons()
    }
    
    private fun getWochentagName(tag: Int): String {
        return when (tag) {
            0 -> "Montag"
            1 -> "Dienstag"
            2 -> "Mittwoch"
            3 -> "Donnerstag"
            4 -> "Freitag"
            5 -> "Samstag"
            6 -> "Sonntag"
            else -> "Unbekannt"
        }
    }
    
    private fun updateWochentagButtons() {
        wochentagButtons.forEachIndexed { index, button ->
            // Wochenendtage (Sa=5, So=6) bekommen Orange-Farbe
            val isWeekend = index == 5 || index == 6
            val isSelected = index == selectedWochentag
            
            if (isSelected) {
                button.alpha = 1.0f
                if (isWeekend) {
                    button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        resources.getColor(com.example.we2026_5.R.color.weekend_orange_dark, theme)
                    )
                } else {
                    button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        resources.getColor(com.example.we2026_5.R.color.weekday_blue_dark, theme)
                    )
                }
            } else {
                button.alpha = 0.8f
                if (isWeekend) {
                    button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        resources.getColor(com.example.we2026_5.R.color.weekend_orange, theme)
                    )
                } else {
                    button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        resources.getColor(com.example.we2026_5.R.color.weekday_blue, theme)
                    )
                }
            }
            
            // Text immer sichtbar machen
            button.setTextColor(resources.getColor(com.example.we2026_5.R.color.white, theme))
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
                    val neueListe = com.example.we2026_5.KundenListe(
                        id = listeId,
                        name = listeName,
                        abholungWochentag = selectedAbholungWT,
                        auslieferungWochentag = selectedAuslieferungWT
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
