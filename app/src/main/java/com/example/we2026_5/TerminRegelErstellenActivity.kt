package com.example.we2026_5

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.databinding.ActivityTerminRegelErstellenBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Calendar

class TerminRegelErstellenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTerminRegelErstellenBinding
    private val regelRepository: TerminRegelRepository by inject()
    private var regelId: String? = null // null = neue Regel, sonst = bearbeiten
    private var abholungDatum: Long = 0 // 0 = heute
    private var auslieferungDatum: Long = 0 // 0 = heute
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminRegelErstellenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Prüfe ob Regel bearbeitet wird
        regelId = intent.getStringExtra("REGEL_ID")
        if (regelId != null) {
            binding.tvTitle.text = "Regel bearbeiten"
            loadRegel()
        } else {
            binding.tvTitle.text = "Neue Regel erstellen"
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

        binding.btnAbholungDatum.setOnClickListener {
            showDatePicker(true)
        }

        binding.btnAuslieferungDatum.setOnClickListener {
            showDatePicker(false)
        }

        binding.btnSpeichern.setOnClickListener {
            saveRegel()
        }
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

                        DatePickerDialog(
                            this,
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
                        ).show()
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

        if (wiederholen && intervallTage < 1) {
            Toast.makeText(this, "Intervall muss mindestens 1 Tag sein", Toast.LENGTH_SHORT).show()
            return
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
                        intervallAnzahl = intervallAnzahl
                    )
                }

                val success = regelRepository.saveRegel(regel)
                if (success) {
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
}
