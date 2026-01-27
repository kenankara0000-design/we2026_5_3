package com.example.we2026_5

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.databinding.ActivityListeErstellenBinding
import com.example.we2026_5.FirebaseRetryHelper
import com.example.we2026_5.util.IntervallManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.*

class ListeErstellenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListeErstellenBinding
    private val listeRepository: KundenListeRepository by inject()
    
    // Intervalle-Verwaltung
    private val intervalle = mutableListOf<ListeIntervall>()
    private lateinit var intervallAdapter: ListeIntervallAdapter
    private var aktuellesIntervallPosition: Int = -1
    private var aktuellesDatumTyp: Boolean = true // true = Abholung, false = Auslieferung

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListeErstellenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        
        // Intervall-Adapter initialisieren
        intervallAdapter = ListeIntervallAdapter(
            intervalle = intervalle.toMutableList(),
            onIntervallChanged = { neueIntervalle ->
                intervalle.clear()
                intervalle.addAll(neueIntervalle)
            },
            onDatumSelected = { position, isAbholung ->
                aktuellesIntervallPosition = position
                aktuellesDatumTyp = isAbholung
                IntervallManager.showDatumPickerForListe(
                    context = this@ListeErstellenActivity,
                    intervalle = intervalle,
                    position = position,
                    isAbholung = isAbholung,
                    onDatumSelected = { updatedIntervall ->
                        intervallAdapter.updateIntervalle(intervalle.toList())
                    }
                )
            }
        )
        binding.rvIntervalle.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvIntervalle.adapter = intervallAdapter

        // Intervall hinzufügen Button
        binding.btnIntervallHinzufuegen.setOnClickListener {
            val neuesIntervall = ListeIntervall()
            intervallAdapter.addIntervall(neuesIntervall)
        }

        binding.btnSaveListe.setOnClickListener {
            val name = binding.etListeName.text.toString().trim()
            if (name.isEmpty()) {
                binding.etListeName.error = "Listen-Name fehlt"
                return@setOnClickListener
            }

            // Button sofort deaktivieren und visuelles Feedback geben
            binding.btnSaveListe.isEnabled = false
            binding.btnSaveListe.text = "Speichere..."
            binding.btnSaveListe.alpha = 0.6f

            CoroutineScope(Dispatchers.Main).launch {
                val listeId = UUID.randomUUID().toString()
                
                // Liste-Art bestimmen
                val listeArt = when {
                    binding.rbGewerbe.isChecked -> "Gewerbe"
                    binding.rbPrivat.isChecked -> "Privat"
                    binding.rbListe.isChecked -> "Liste"
                    else -> "Gewerbe"
                }
                
                // Liste mit Intervalle erstellen
                val neueListe = KundenListe(
                    id = listeId,
                    name = name,
                    listeArt = listeArt,
                    intervalle = intervalle.toList(),
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

                // Prüfen ob erfolgreich (Unit != null bedeutet Erfolg)
                if (success != null) {
                    // Erfolg: Button-Text ändern und dann Activity schließen
                    runOnUiThread {
                        binding.btnSaveListe.text = "✓ Gespeichert!"
                        binding.btnSaveListe.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            resources.getColor(com.example.we2026_5.R.color.status_done, theme)
                        )
                        binding.btnSaveListe.alpha = 1.0f

                        // Kurz warten, damit der Benutzer das Feedback sieht, dann Activity schließen
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (!isFinishing) {
                                finish()
                            }
                        }, 800)
                    }
                } else {
                    // Fehler: Button wieder aktivieren
                    runOnUiThread {
                        binding.btnSaveListe.isEnabled = true
                        binding.btnSaveListe.text = "Speichern"
                        binding.btnSaveListe.alpha = 1.0f
                    }
                }
            }
        }
    }
    
    // showDatumPicker Funktion entfernt - jetzt in IntervallManager
}
