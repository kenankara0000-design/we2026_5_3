package com.example.we2026_5

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.R
import com.example.we2026_5.databinding.ActivityTerminRegelErstellenBinding
import com.example.we2026_5.util.TerminRegelDatePickerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar

class TerminRegelErstellenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTerminRegelErstellenBinding
    private val viewModel: com.example.we2026_5.ui.terminregel.TerminRegelErstellenViewModel by viewModel()
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

        regelId = intent.getStringExtra("REGEL_ID")
        if (regelId != null) {
            binding.tvTitle.text = getString(R.string.termin_regel_titel_bearbeiten)
            binding.btnLoeschen.visibility = View.VISIBLE
            loadRegel()
        } else {
            binding.tvTitle.text = getString(R.string.termin_regel_titel_neu)
            binding.btnLoeschen.visibility = View.GONE
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
        TerminRegelDatePickerHelper.showDatePicker(this, year, month, day) { timestamp ->
            startDatum = timestamp
            binding.btnStartDatum.text = getString(R.string.termin_regel_startdatum, TerminRegelDatePickerHelper.formatDateFromMillis(timestamp))
        }
    }

    private fun showWochentagPicker(isAbholung: Boolean) {
        AlertDialog.Builder(this)
            .setTitle(R.string.termin_regel_wochentag_waehlen)
            .setItems(wochentage) { _, which ->
                abholungWochentag = which
                auslieferungWochentag = which
                binding.btnAbholungWochentag.text = getString(R.string.termin_regel_abholung_datum, wochentage[which])
                binding.btnAuslieferungWochentag.text = getString(R.string.termin_regel_auslieferung_datum, wochentage[which])
            }
            .setNegativeButton(R.string.termin_regel_abbrechen, null)
            .show()
    }

    private fun showDatePicker(isAbholung: Boolean) {
        AlertDialog.Builder(this)
            .setTitle(if (isAbholung) R.string.termin_regel_abholung else R.string.termin_regel_auslieferung)
            .setItems(arrayOf(getString(R.string.termin_regel_heute_verwenden), getString(R.string.termin_regel_datum_waehlen))) { _, which ->
                when (which) {
                    0 -> {
                        if (isAbholung) {
                            abholungDatum = 0
                            binding.btnAbholungDatum.text = getString(R.string.termin_regel_abholung_heute)
                        } else {
                            auslieferungDatum = 0
                            binding.btnAuslieferungDatum.text = getString(R.string.termin_regel_auslieferung_heute)
                        }
                    }
                    1 -> {
                        val year = calendar.get(Calendar.YEAR)
                        val month = calendar.get(Calendar.MONTH)
                        val day = calendar.get(Calendar.DAY_OF_MONTH)
                        TerminRegelDatePickerHelper.showDatePicker(this, year, month, day) { timestamp ->
                            val dateText = TerminRegelDatePickerHelper.formatDateFromMillis(timestamp)
                            if (isAbholung) {
                                abholungDatum = timestamp
                                binding.btnAbholungDatum.text = getString(R.string.termin_regel_abholung_datum, dateText)
                            } else {
                                auslieferungDatum = timestamp
                                binding.btnAuslieferungDatum.text = getString(R.string.termin_regel_auslieferung_datum, dateText)
                            }
                        }
                    }
                }
            }
            .setNegativeButton(R.string.termin_regel_abbrechen, null)
            .show()
    }

    private fun loadRegel() {
        val id = regelId ?: return
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val regel = viewModel.loadRegel(id)
                if (regel != null) {
                    binding.etRegelName.setText(regel.name)
                    binding.etBeschreibung.setText(regel.beschreibung)
                    binding.cbWiederholen.isChecked = regel.wiederholen
                    binding.etIntervallTage.setText(regel.intervallTage.toString())
                    binding.etIntervallAnzahl.setText(regel.intervallAnzahl.toString())
                    binding.cbWochentagBasiert.isChecked = regel.wochentagBasiert
                    if (regel.startDatum > 0) {
                        startDatum = regel.startDatum
                        binding.btnStartDatum.text = getString(R.string.termin_regel_startdatum, TerminRegelDatePickerHelper.formatDateFromMillis(regel.startDatum))
                    }
                    if (regel.abholungWochentag >= 0) {
                        abholungWochentag = regel.abholungWochentag
                        binding.btnAbholungWochentag.text = getString(R.string.termin_regel_abholung_datum, wochentage[regel.abholungWochentag])
                    }
                    if (regel.auslieferungWochentag >= 0) {
                        auslieferungWochentag = regel.auslieferungWochentag
                        binding.btnAuslieferungWochentag.text = getString(R.string.termin_regel_auslieferung_datum, wochentage[regel.auslieferungWochentag])
                    }
                    if (regel.abholungDatum > 0) {
                        abholungDatum = regel.abholungDatum
                        binding.btnAbholungDatum.text = getString(R.string.termin_regel_abholung_datum, TerminRegelDatePickerHelper.formatDateFromMillis(regel.abholungDatum))
                    }
                    if (regel.auslieferungDatum > 0) {
                        auslieferungDatum = regel.auslieferungDatum
                        binding.btnAuslieferungDatum.text = getString(R.string.termin_regel_auslieferung_datum, TerminRegelDatePickerHelper.formatDateFromMillis(regel.auslieferungDatum))
                    }
                    binding.layoutIntervall.visibility = if (regel.wiederholen) View.VISIBLE else View.GONE
                    updateWochentagMode(regel.wochentagBasiert)
                }
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelErstellenActivity, getString(R.string.error_laden, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveRegel() {
        val name = binding.etRegelName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.validierung_regel_name), Toast.LENGTH_SHORT).show()
            return
        }
        val beschreibung = binding.etBeschreibung.text.toString().trim()
        val wiederholen = binding.cbWiederholen.isChecked
        val intervallTage = binding.etIntervallTage.text.toString().toIntOrNull() ?: 7
        val intervallAnzahl = binding.etIntervallAnzahl.text.toString().toIntOrNull() ?: 0
        val wochentagBasiert = binding.cbWochentagBasiert.isChecked

        if (wiederholen && intervallTage < 1) {
            Toast.makeText(this, getString(R.string.validierung_intervall_min), Toast.LENGTH_SHORT).show()
            return
        }
        if (wochentagBasiert) {
            if (startDatum == 0L) {
                Toast.makeText(this, getString(R.string.validierung_startdatum), Toast.LENGTH_SHORT).show()
                return
            }
            if (abholungWochentag == -1) {
                Toast.makeText(this, getString(R.string.validierung_abholung_wochentag), Toast.LENGTH_SHORT).show()
                return
            }
            if (auslieferungWochentag == -1) {
                Toast.makeText(this, getString(R.string.validierung_auslieferung_wochentag), Toast.LENGTH_SHORT).show()
                return
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val currentRegelId = regelId
                val regel = when {
                    currentRegelId == null -> TerminRegel(
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
                    )
                    else -> {
                        val existing = viewModel.loadRegel(currentRegelId) ?: return@launch
                        existing.copy(
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
                            geaendertAm = System.currentTimeMillis()
                        )
                    }
                }

                val success = viewModel.saveRegel(regel)
                if (success) {
                    if (regelId != null) {
                        viewModel.aktualisiereBetroffeneKunden(regel)
                    }
                    Toast.makeText(this@TerminRegelErstellenActivity, getString(R.string.toast_regel_gespeichert), Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@TerminRegelErstellenActivity, getString(R.string.toast_fehler_speichern), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelErstellenActivity, getString(R.string.toast_fehler, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
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
                val wirdVerwendet = viewModel.istRegelVerwendet(id)
                if (wirdVerwendet) {
                    AlertDialog.Builder(this@TerminRegelErstellenActivity)
                        .setTitle(R.string.dialog_regel_loeschen_titel)
                        .setMessage(R.string.dialog_regel_loeschen_message)
                        .setPositiveButton(R.string.dialog_ok, null)
                        .show()
                } else {
                    AlertDialog.Builder(this@TerminRegelErstellenActivity)
                        .setTitle(R.string.dialog_regel_loeschen_confirm_titel)
                        .setMessage(R.string.dialog_regel_loeschen_confirm_message)
                        .setPositiveButton(R.string.dialog_loeschen) { _, _ -> deleteRegel(id) }
                        .setNegativeButton(R.string.termin_regel_abbrechen, null)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelErstellenActivity, getString(R.string.toast_fehler_pruefen, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteRegel(regelId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val success = viewModel.deleteRegel(regelId)
                if (success) {
                    Toast.makeText(this@TerminRegelErstellenActivity, getString(R.string.toast_regel_geloescht), Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@TerminRegelErstellenActivity, getString(R.string.toast_fehler_loeschen), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelErstellenActivity, getString(R.string.toast_fehler, e.message.orEmpty()), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
