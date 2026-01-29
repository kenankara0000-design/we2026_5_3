package com.example.we2026_5

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.databinding.ActivityTerminRegelErstellenBinding
import com.example.we2026_5.util.TerminRegelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Calendar

class TerminRegelErstellenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTerminRegelErstellenBinding
    private val regelRepository: TerminRegelRepository by inject()
    private val customerRepository: CustomerRepository by inject()
    private var regelId: String? = null // null = neue Regel, sonst = bearbeiten
    private var abholungDatum: Long = 0 // 0 = heute
    private var auslieferungDatum: Long = 0 // 0 = heute
    private var startDatum: Long = 0 // Startdatum für Wochentag-Berechnung
    private var abholungWochentag: Int = -1 // 0=Mo, 1=Di, ..., 6=So, -1=nicht gesetzt
    private var auslieferungWochentag: Int = -1 // 0=Mo, 1=Di, ..., 6=So, -1=nicht gesetzt
    private val calendar = Calendar.getInstance()
    private val wochentage = arrayOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminRegelErstellenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Prüfe ob Regel bearbeitet wird
        regelId = intent.getStringExtra("REGEL_ID")
        if (regelId != null) {
            binding.tvTitle.text = "Regel bearbeiten"
            binding.btnLoeschen.visibility = View.VISIBLE // Löschen-Button nur im Bearbeiten-Modus anzeigen
            loadRegel()
        } else {
            binding.tvTitle.text = "Neue Regel erstellen"
            binding.btnLoeschen.visibility = View.GONE // Löschen-Button ausblenden bei neuer Regel
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.cbWiederholen.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutIntervall.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Wochentag-basiert Checkbox
        binding.cbWochentagBasiert.setOnCheckedChangeListener { _, isChecked ->
            updateWochentagMode(isChecked)
        }

        // Startdatum Button
        binding.btnStartDatum.setOnClickListener {
            showStartDatePicker()
        }

        // Abholung/Auslieferung Buttons
        binding.btnAbholungDatum.setOnClickListener {
            if (binding.cbWochentagBasiert.isChecked) {
                showWochentagPicker(true)
            } else {
                showDatePicker(true)
            }
        }

        binding.btnAuslieferungDatum.setOnClickListener {
            if (binding.cbWochentagBasiert.isChecked) {
                showWochentagPicker(false)
            } else {
                showDatePicker(false)
            }
        }

        binding.btnAbholungWochentag.setOnClickListener {
            showWochentagPicker(true)
        }

        binding.btnAuslieferungWochentag.setOnClickListener {
            showWochentagPicker(false)
        }

        binding.btnSpeichern.setOnClickListener {
            saveRegel()
        }
        
        binding.btnLoeschen.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun updateWochentagMode(isWochentagBasiert: Boolean) {
        if (isWochentagBasiert) {
            // Wochentag-Modus: Zeige Wochentag-Auswahl, verstecke Datum-Auswahl
            binding.layoutAbholungDatum.visibility = View.GONE
            binding.layoutAbholungWochentag.visibility = View.VISIBLE
            binding.layoutAuslieferungDatum.visibility = View.GONE
            binding.layoutAuslieferungWochentag.visibility = View.VISIBLE
            binding.btnStartDatum.visibility = View.VISIBLE
        } else {
            // Datum-Modus: Zeige Datum-Auswahl, verstecke Wochentag-Auswahl
            binding.layoutAbholungDatum.visibility = View.VISIBLE
            binding.layoutAbholungWochentag.visibility = View.GONE
            binding.layoutAuslieferungDatum.visibility = View.VISIBLE
            binding.layoutAuslieferungWochentag.visibility = View.GONE
            binding.btnStartDatum.visibility = View.GONE
        }
    }

    private fun showStartDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Material Design DatePicker für bessere Lesbarkeit
        val datePickerDialog = DatePickerDialog(
            this,
            android.R.style.Theme_Material_Dialog_Alert,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)
                startDatum = selectedCalendar.timeInMillis
                val dateText = String.format("%02d.%02d.%04d", selectedDay, selectedMonth + 1, selectedYear)
                binding.btnStartDatum.text = "Startdatum: $dateText"
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun showWochentagPicker(isAbholung: Boolean) {
        AlertDialog.Builder(this)
            .setTitle("Wochentag wählen")
            .setItems(wochentage) { _, which ->
                // Automatisch beide (Abholung und Auslieferung) auf den ausgewählten Tag setzen
                abholungWochentag = which
                auslieferungWochentag = which
                binding.btnAbholungWochentag.text = "Abholung: ${wochentage[which]}"
                binding.btnAuslieferungWochentag.text = "Auslieferung: ${wochentage[which]}"
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun showDatePicker(isAbholung: Boolean) {
        AlertDialog.Builder(this)
            .setTitle(if (isAbholung) "Abholung" else "Auslieferung")
            .setItems(arrayOf("Heute verwenden", "Datum wählen")) { _, which ->
                when (which) {
                    0 -> {
                        // Heute verwenden
                        if (isAbholung) {
                            abholungDatum = 0
                            binding.btnAbholungDatum.text = "Abholung: Heute"
                        } else {
                            auslieferungDatum = 0
                            binding.btnAuslieferungDatum.text = "Auslieferung: Heute"
                        }
                    }
                    1 -> {
                        // Datum wählen
                        val year = calendar.get(Calendar.YEAR)
                        val month = calendar.get(Calendar.MONTH)
                        val day = calendar.get(Calendar.DAY_OF_MONTH)

                        // Material Design DatePicker für bessere Lesbarkeit
                        val datePickerDialog = DatePickerDialog(
                            this,
                            android.R.style.Theme_Material_Dialog_Alert,
                            { _, selectedYear, selectedMonth, selectedDay ->
                                val selectedCalendar = Calendar.getInstance()
                                selectedCalendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                                selectedCalendar.set(Calendar.MILLISECOND, 0)
                                val timestamp = selectedCalendar.timeInMillis

                                if (isAbholung) {
                                    abholungDatum = timestamp
                                    val dateText = String.format("%02d.%02d.%04d", selectedDay, selectedMonth + 1, selectedYear)
                                    binding.btnAbholungDatum.text = "Abholung: $dateText"
                                } else {
                                    auslieferungDatum = timestamp
                                    val dateText = String.format("%02d.%02d.%04d", selectedDay, selectedMonth + 1, selectedYear)
                                    binding.btnAuslieferungDatum.text = "Auslieferung: $dateText"
                                }
                            },
                            year,
                            month,
                            day
                        )
                        datePickerDialog.show()
                    }
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun loadRegel() {
        regelId?.let { id ->
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val regel = regelRepository.getRegelById(id)
                    if (regel != null) {
                        binding.etRegelName.setText(regel.name)
                        binding.etBeschreibung.setText(regel.beschreibung)
                        binding.cbWiederholen.isChecked = regel.wiederholen
                        binding.etIntervallTage.setText(regel.intervallTage.toString())
                        binding.etIntervallAnzahl.setText(regel.intervallAnzahl.toString())

                        // Wochentag-basiert laden
                        binding.cbWochentagBasiert.isChecked = regel.wochentagBasiert
                        if (regel.startDatum > 0) {
                            startDatum = regel.startDatum
                            val calendar = Calendar.getInstance().apply { timeInMillis = regel.startDatum }
                            val dateText = String.format(
                                "%02d.%02d.%04d",
                                calendar.get(Calendar.DAY_OF_MONTH),
                                calendar.get(Calendar.MONTH) + 1,
                                calendar.get(Calendar.YEAR)
                            )
                            binding.btnStartDatum.text = "Startdatum: $dateText"
                        }
                        if (regel.abholungWochentag >= 0) {
                            abholungWochentag = regel.abholungWochentag
                            binding.btnAbholungWochentag.text = "Abholung: ${wochentage[regel.abholungWochentag]}"
                        }
                        if (regel.auslieferungWochentag >= 0) {
                            auslieferungWochentag = regel.auslieferungWochentag
                            binding.btnAuslieferungWochentag.text = "Auslieferung: ${wochentage[regel.auslieferungWochentag]}"
                        }
                        // startWocheOption wird nicht mehr verwendet - automatische Berechnung basierend auf Startdatum

                        // Datum-basiert laden
                        if (regel.abholungDatum > 0) {
                            abholungDatum = regel.abholungDatum
                            val calendar = Calendar.getInstance().apply { timeInMillis = regel.abholungDatum }
                            val dateText = String.format(
                                "%02d.%02d.%04d",
                                calendar.get(Calendar.DAY_OF_MONTH),
                                calendar.get(Calendar.MONTH) + 1,
                                calendar.get(Calendar.YEAR)
                            )
                            binding.btnAbholungDatum.text = "Abholung: $dateText"
                        }

                        if (regel.auslieferungDatum > 0) {
                            auslieferungDatum = regel.auslieferungDatum
                            val calendar = Calendar.getInstance().apply { timeInMillis = regel.auslieferungDatum }
                            val dateText = String.format(
                                "%02d.%02d.%04d",
                                calendar.get(Calendar.DAY_OF_MONTH),
                                calendar.get(Calendar.MONTH) + 1,
                                calendar.get(Calendar.YEAR)
                            )
                            binding.btnAuslieferungDatum.text = "Auslieferung: $dateText"
                        }

                        binding.layoutIntervall.visibility = if (regel.wiederholen) View.VISIBLE else View.GONE
                        updateWochentagMode(regel.wochentagBasiert)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@TerminRegelErstellenActivity, "Fehler beim Laden: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveRegel() {
        val name = binding.etRegelName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Bitte geben Sie einen Regel-Namen ein", Toast.LENGTH_SHORT).show()
            return
        }

        val beschreibung = binding.etBeschreibung.text.toString().trim()
        val wiederholen = binding.cbWiederholen.isChecked
        val intervallTage = binding.etIntervallTage.text.toString().toIntOrNull() ?: 7
        val intervallAnzahl = binding.etIntervallAnzahl.text.toString().toIntOrNull() ?: 0
        val wochentagBasiert = binding.cbWochentagBasiert.isChecked
        // startWocheOption wird nicht mehr verwendet - automatische Berechnung basierend auf Startdatum

        if (wiederholen && intervallTage < 1) {
            Toast.makeText(this, "Intervall muss mindestens 1 Tag sein", Toast.LENGTH_SHORT).show()
            return
        }

        if (wochentagBasiert) {
            if (startDatum == 0L) {
                Toast.makeText(this, "Bitte wählen Sie ein Startdatum", Toast.LENGTH_SHORT).show()
                return
            }
            if (abholungWochentag == -1) {
                Toast.makeText(this, "Bitte wählen Sie einen Abholung-Wochentag", Toast.LENGTH_SHORT).show()
                return
            }
            if (auslieferungWochentag == -1) {
                Toast.makeText(this, "Bitte wählen Sie einen Auslieferung-Wochentag", Toast.LENGTH_SHORT).show()
                return
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val regel = if (regelId != null) {
                    // Bestehende Regel aktualisieren
                    val existingRegel = regelRepository.getRegelById(regelId!!)
                    existingRegel?.copy(
                        name = name,
                        beschreibung = beschreibung,
                        abholungDatum = abholungDatum,
                        auslieferungDatum = auslieferungDatum,
                        wiederholen = wiederholen,
                        intervallTage = intervallTage,
                        intervallAnzahl = intervallAnzahl,
                        wochentagBasiert = wochentagBasiert,
                        startDatum = startDatum,
                        abholungWochentag = abholungWochentag,
                        auslieferungWochentag = auslieferungWochentag,
                        // startWocheOption wird nicht mehr verwendet - automatische Berechnung
                        geaendertAm = System.currentTimeMillis()
                    ) ?: return@launch
                } else {
                    // Neue Regel erstellen
                    TerminRegel(
                        name = name,
                        beschreibung = beschreibung,
                        abholungDatum = abholungDatum,
                        auslieferungDatum = auslieferungDatum,
                        wiederholen = wiederholen,
                        intervallTage = intervallTage,
                        intervallAnzahl = intervallAnzahl,
                        wochentagBasiert = wochentagBasiert,
                        startDatum = startDatum,
                        abholungWochentag = abholungWochentag,
                        auslieferungWochentag = auslieferungWochentag
                        // startWocheOption wird nicht mehr verwendet - automatische Berechnung
                    )
                }

                val success = regelRepository.saveRegel(regel)
                if (success) {
                    // Wenn Regel bearbeitet wurde, alle betroffenen Kunden aktualisieren
                    if (regelId != null) {
                        aktualisiereBetroffeneKunden(regel)
                    }
                    Toast.makeText(this@TerminRegelErstellenActivity, "Regel gespeichert", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@TerminRegelErstellenActivity, "Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelErstellenActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Zeigt Bestätigungsdialog zum Löschen der Regel.
     * Prüft, ob die Regel von einem Kunden verwendet wird.
     */
    private fun showDeleteConfirmation() {
        val id = regelId ?: return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Prüfen, ob Regel verwendet wird
                val wirdVerwendet = istRegelVerwendet(id)
                
                if (wirdVerwendet) {
                    AlertDialog.Builder(this@TerminRegelErstellenActivity)
                        .setTitle("Regel kann nicht gelöscht werden")
                        .setMessage("Diese Regel wird noch von einem oder mehreren Kunden verwendet. Bitte entfernen Sie die Regel zuerst von allen Kunden, bevor Sie sie löschen können.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    AlertDialog.Builder(this@TerminRegelErstellenActivity)
                        .setTitle("Regel löschen?")
                        .setMessage("Möchten Sie diese Regel wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.")
                        .setPositiveButton("Löschen") { _, _ ->
                            deleteRegel(id)
                        }
                        .setNegativeButton("Abbrechen", null)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelErstellenActivity, "Fehler beim Prüfen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Prüft, ob eine Regel von einem Kunden oder einer Liste verwendet wird.
     * Eine Regel wird verwendet, wenn mindestens ein Kunde ein Intervall mit dieser Regel-ID hat.
     * (Listen verwenden aktuell keine Regel-IDs, daher nur Kunden-Prüfung)
     */
    private suspend fun istRegelVerwendet(regelId: String): Boolean {
        return try {
            val allCustomers = customerRepository.getAllCustomers()
            val wirdVonKundenVerwendet = allCustomers.any { customer ->
                customer.intervalle.any { intervall ->
                    intervall.terminRegelId == regelId
                }
            }
            
            // TODO: Wenn Listen auch Regel-IDs speichern, hier auch Listen prüfen
            // val allListen = listeRepository.getAllListen()
            // val wirdVonListenVerwendet = allListen.any { liste ->
            //     liste.intervalle.any { intervall ->
            //         intervall.terminRegelId == regelId
            //     }
            // }
            
            wirdVonKundenVerwendet // || wirdVonListenVerwendet
        } catch (e: Exception) {
            // Bei Fehler annehmen, dass Regel verwendet wird (sicherer)
            true
        }
    }
    
    /**
     * Aktualisiert alle Kunden, die die bearbeitete Regel verwenden.
     * Ersetzt das alte Intervall durch ein neues, basierend auf der aktualisierten Regel.
     */
    private suspend fun aktualisiereBetroffeneKunden(regel: TerminRegel) {
        try {
            val allCustomers = customerRepository.getAllCustomers()
            var aktualisierteKunden = 0
            
            allCustomers.forEach { customer ->
                // Prüfe ob Kunde ein Intervall mit dieser Regel-ID hat
                val intervallIndex = customer.intervalle.indexOfFirst { it.terminRegelId == regel.id }
                
                if (intervallIndex != -1) {
                    // Neues Intervall mit der aktualisierten Regel berechnen
                    val neuesIntervall = TerminRegelManager.wendeRegelAufKundeAn(regel, customer)
                    
                    // Altes Intervall durch neues ersetzen (ID beibehalten für Konsistenz)
                    val aktualisierteIntervalle = customer.intervalle.toMutableList()
                    aktualisierteIntervalle[intervallIndex] = neuesIntervall.copy(id = customer.intervalle[intervallIndex].id)
                    
                    // Kunden mit aktualisierten Intervallen speichern
                    // Intervalle müssen als Map-Liste für Firebase serialisiert werden
                    val intervalleMap = aktualisierteIntervalle.mapIndexed { index, intervall ->
                        mapOf(
                            "id" to intervall.id,
                            "abholungDatum" to intervall.abholungDatum,
                            "auslieferungDatum" to intervall.auslieferungDatum,
                            "wiederholen" to intervall.wiederholen,
                            "intervallTage" to intervall.intervallTage,
                            "intervallAnzahl" to intervall.intervallAnzahl,
                            "erstelltAm" to intervall.erstelltAm,
                            "terminRegelId" to intervall.terminRegelId
                        )
                    }
                    val updates = mapOf("intervalle" to intervalleMap)
                    customerRepository.updateCustomer(customer.id, updates)
                    aktualisierteKunden++
                }
            }
            
            if (aktualisierteKunden > 0) {
                android.util.Log.d("TerminRegelErstellen", "$aktualisierteKunden Kunden wurden aktualisiert")
            }
        } catch (e: Exception) {
            android.util.Log.e("TerminRegelErstellen", "Fehler beim Aktualisieren der Kunden", e)
            // Fehler wird stillschweigend behandelt, um den Regel-Speichervorgang nicht zu blockieren
        }
    }
    
    /**
     * Löscht die Regel.
     */
    private fun deleteRegel(regelId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val success = regelRepository.deleteRegel(regelId)
                if (success) {
                    Toast.makeText(this@TerminRegelErstellenActivity, "Regel gelöscht", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@TerminRegelErstellenActivity, "Fehler beim Löschen", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelErstellenActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
