package com.example.we2026_5

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.databinding.ActivityListeErstellenBinding
import com.example.we2026_5.databinding.ItemListeIntervallBinding
import com.example.we2026_5.FirebaseRetryHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.*

class ListeErstellenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListeErstellenBinding
    private val listeRepository: KundenListeRepository by inject()
    private val intervalle = mutableListOf<IntervallDaten>()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

    data class IntervallDaten(
        var abholungDatum: Long = 0,
        var auslieferungDatum: Long = 0,
        var wiederholen: Boolean = false,
        var intervallTage: Int = 7
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListeErstellenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // Erstes Intervall automatisch hinzufügen
        addIntervall()

        binding.btnNeuesIntervall.setOnClickListener {
            if (intervalle.size >= 12) {
                Toast.makeText(this, "Maximal 12 Intervalle möglich", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            addIntervall()
        }

        binding.btnSaveListe.setOnClickListener {
            val name = binding.etListeName.text.toString().trim()
            if (name.isEmpty()) {
                binding.etListeName.error = "Listen-Name fehlt"
                return@setOnClickListener
            }

            // Validierung: Mindestens ein Intervall muss vorhanden sein
            if (intervalle.isEmpty()) {
                Toast.makeText(this, "Bitte fügen Sie mindestens ein Intervall hinzu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validierung: Alle Intervalle müssen Daten haben
            val unvollstaendigeIntervalle = intervalle.filterIndexed { index, intervall ->
                intervall.abholungDatum == 0L || intervall.auslieferungDatum == 0L
            }
            if (unvollstaendigeIntervalle.isNotEmpty()) {
                Toast.makeText(this, "Bitte wählen Sie für alle Intervalle Abholungs- und Auslieferungsdatum", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Validierung: Wiederholungsintervalle müssen gültige Tage haben
            val ungültigeIntervalle = intervalle.filterIndexed { index, intervall ->
                intervall.wiederholen && (intervall.intervallTage < 1 || intervall.intervallTage > 365)
            }
            if (ungültigeIntervalle.isNotEmpty()) {
                Toast.makeText(this, "Intervall-Tage müssen zwischen 1 und 365 liegen", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Button sofort deaktivieren und visuelles Feedback geben
            binding.btnSaveListe.isEnabled = false
            binding.btnSaveListe.text = "Speichere..."
            binding.btnSaveListe.alpha = 0.6f

            CoroutineScope(Dispatchers.Main).launch {
                val listeId = UUID.randomUUID().toString()
                
                // Intervalle in ListeIntervall-Objekte konvertieren
                val listeIntervalle = intervalle.map { intervall ->
                    ListeIntervall(
                        abholungDatum = intervall.abholungDatum,
                        auslieferungDatum = intervall.auslieferungDatum,
                        wiederholen = intervall.wiederholen,
                        intervallTage = intervall.intervallTage
                    )
                }
                
                val neueListe = KundenListe(
                    id = listeId,
                    name = name,
                    intervalle = listeIntervalle,
                    erstelltAm = System.currentTimeMillis()
                )

                // Speichern mit Retry-Logik
                val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                    operation = {
                        listeRepository.saveListe(neueListe)
                    },
                    context = this@ListeErstellenActivity,
                    errorMessage = "Fehler beim Speichern. Bitte erneut versuchen.",
                    maxRetries = 3
                )

                runOnUiThread {
                    if (success != null) {
                        // Erfolg: Button-Text ändern und dann Activity schließen
                        binding.btnSaveListe.text = "✓ Gespeichert!"
                        binding.btnSaveListe.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            resources.getColor(com.example.we2026_5.R.color.status_done, theme)
                        )
                        binding.btnSaveListe.alpha = 1.0f

                        // Kurz warten, damit der Benutzer das Feedback sieht
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            finish()
                        }, 800)
                    } else {
                        // Fehler: Button wieder aktivieren
                        binding.btnSaveListe.isEnabled = true
                        binding.btnSaveListe.text = "Speichern"
                        binding.btnSaveListe.alpha = 1.0f
                    }
                }
            }
        }
    }

    private fun addIntervall() {
        val intervallIndex = intervalle.size
        val intervallDaten = IntervallDaten()
        intervalle.add(intervallDaten)

        // Intervall-Karte erstellen
        val intervallBinding = ItemListeIntervallBinding.inflate(LayoutInflater.from(this), binding.layoutIntervalleContainer, false)
        intervallBinding.tvIntervallNummer.text = "Intervall ${intervallIndex + 1}"

        // Abholungsdatum-Button
        intervallBinding.btnPickAbholungDate.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.timeInMillis = if (intervallDaten.abholungDatum > 0) intervallDaten.abholungDatum else System.currentTimeMillis()
            DatePickerDialog(this, { _, y, m, d ->
                val picked = Calendar.getInstance()
                picked.set(y, m, d, 0, 0, 0)
                intervallDaten.abholungDatum = picked.timeInMillis
                intervallBinding.btnPickAbholungDate.text = dateFormat.format(picked.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Auslieferungsdatum-Button
        intervallBinding.btnPickAuslieferungDate.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.timeInMillis = if (intervallDaten.auslieferungDatum > 0) intervallDaten.auslieferungDatum else System.currentTimeMillis()
            DatePickerDialog(this, { _, y, m, d ->
                val picked = Calendar.getInstance()
                picked.set(y, m, d, 0, 0, 0)
                intervallDaten.auslieferungDatum = picked.timeInMillis
                intervallBinding.btnPickAuslieferungDate.text = dateFormat.format(picked.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Wiederholen-Checkbox
        intervallBinding.cbWiederholen.setOnCheckedChangeListener { _, isChecked ->
            intervallDaten.wiederholen = isChecked
            intervallBinding.layoutWiederholung.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Intervall-Tage EditText
        intervallBinding.etIntervallTage.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val tage = s.toString().toIntOrNull() ?: 7
                if (tage in 1..365) {
                    intervallDaten.intervallTage = tage
                }
            }
        })

        // Löschen-Button
        intervallBinding.btnIntervallLoeschen.setOnClickListener {
            if (intervalle.size <= 1) {
                Toast.makeText(this, "Mindestens ein Intervall muss vorhanden sein", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val indexToRemove = intervalle.indexOf(intervallDaten)
            if (indexToRemove != -1) {
                intervalle.removeAt(indexToRemove)
                binding.layoutIntervalleContainer.removeView(intervallBinding.root)
                updateIntervallNummern()
            }
        }

        binding.layoutIntervalleContainer.addView(intervallBinding.root)
    }

    private fun updateIntervallNummern() {
        for (i in 0 until binding.layoutIntervalleContainer.childCount) {
            val view = binding.layoutIntervalleContainer.getChildAt(i)
            val tvNummer = view.findViewById<TextView>(com.example.we2026_5.R.id.tvIntervallNummer)
            if (tvNummer != null) {
                tvNummer.text = "Intervall ${i + 1}"
            }
        }
    }
}
