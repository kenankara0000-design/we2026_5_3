package com.example.we2026_5

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.databinding.ActivityListeErstellenBinding
import com.example.we2026_5.FirebaseRetryHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.*

class ListeErstellenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListeErstellenBinding
    private val listeRepository: KundenListeRepository by inject()
    private var selectedAbholungWochentag: Int = 1 // Dienstag
    private var selectedAuslieferungWochentag: Int = 3 // Donnerstag
    private val wochentagButtonsAbholung = mutableListOf<android.widget.Button>()
    private val wochentagButtonsAuslieferung = mutableListOf<android.widget.Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListeErstellenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Wochentag-Buttons initialisieren
        wochentagButtonsAbholung.addAll(listOf(
            binding.btnAbholungMo, binding.btnAbholungDi, binding.btnAbholungMi,
            binding.btnAbholungDo, binding.btnAbholungFr, binding.btnAbholungSa, binding.btnAbholungSo
        ))
        
        wochentagButtonsAuslieferung.addAll(listOf(
            binding.btnAuslieferungMo, binding.btnAuslieferungDi, binding.btnAuslieferungMi,
            binding.btnAuslieferungDo, binding.btnAuslieferungFr, binding.btnAuslieferungSa, binding.btnAuslieferungSo
        ))

        // Standard-Wochentage setzen
        updateWochentagButtons()

        // Abholung-Wochentag-Buttons
        binding.btnAbholungMo.setOnClickListener { selectAbholungWochentag(0) }
        binding.btnAbholungDi.setOnClickListener { selectAbholungWochentag(1) }
        binding.btnAbholungMi.setOnClickListener { selectAbholungWochentag(2) }
        binding.btnAbholungDo.setOnClickListener { selectAbholungWochentag(3) }
        binding.btnAbholungFr.setOnClickListener { selectAbholungWochentag(4) }
        binding.btnAbholungSa.setOnClickListener { selectAbholungWochentag(5) }
        binding.btnAbholungSo.setOnClickListener { selectAbholungWochentag(6) }

        // Auslieferung-Wochentag-Buttons
        binding.btnAuslieferungMo.setOnClickListener { selectAuslieferungWochentag(0) }
        binding.btnAuslieferungDi.setOnClickListener { selectAuslieferungWochentag(1) }
        binding.btnAuslieferungMi.setOnClickListener { selectAuslieferungWochentag(2) }
        binding.btnAuslieferungDo.setOnClickListener { selectAuslieferungWochentag(3) }
        binding.btnAuslieferungFr.setOnClickListener { selectAuslieferungWochentag(4) }
        binding.btnAuslieferungSa.setOnClickListener { selectAuslieferungWochentag(5) }
        binding.btnAuslieferungSo.setOnClickListener { selectAuslieferungWochentag(6) }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSaveListe.setOnClickListener {
            val name = binding.etListeName.text.toString().trim()
            if (name.isEmpty()) {
                binding.etListeName.error = "Listen-Name fehlt"
                return@setOnClickListener
            }

            val wiederholen = binding.cbWiederholen.isChecked

            // Button sofort deaktivieren und visuelles Feedback geben
            binding.btnSaveListe.isEnabled = false
            binding.btnSaveListe.text = "Speichere..."
            binding.btnSaveListe.alpha = 0.6f

            CoroutineScope(Dispatchers.Main).launch {
                val listeId = UUID.randomUUID().toString()
                val neueListe = KundenListe(
                    id = listeId,
                    name = name,
                    abholungWochentag = selectedAbholungWochentag,
                    auslieferungWochentag = selectedAuslieferungWochentag,
                    wiederholen = wiederholen,
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

    private fun selectAbholungWochentag(tag: Int) {
        selectedAbholungWochentag = tag
        updateWochentagButtons()
    }

    private fun selectAuslieferungWochentag(tag: Int) {
        selectedAuslieferungWochentag = tag
        updateWochentagButtons()
    }

    private fun updateWochentagButtons() {
        // Abholung-Buttons
        wochentagButtonsAbholung.forEachIndexed { index, button ->
            val isWeekend = index == 5 || index == 6
            val isSelected = index == selectedAbholungWochentag

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
                button.alpha = 0.6f
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
            button.setTextColor(resources.getColor(com.example.we2026_5.R.color.white, theme))
        }

        // Auslieferung-Buttons
        wochentagButtonsAuslieferung.forEachIndexed { index, button ->
            val isWeekend = index == 5 || index == 6
            val isSelected = index == selectedAuslieferungWochentag

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
                button.alpha = 0.6f
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
            button.setTextColor(resources.getColor(com.example.we2026_5.R.color.white, theme))
        }
    }
}
