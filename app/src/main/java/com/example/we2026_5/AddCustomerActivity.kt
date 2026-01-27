package com.example.we2026_5

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.databinding.ActivityAddCustomerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import org.koin.android.ext.android.inject

class AddCustomerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCustomerBinding
    private val repository: CustomerRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCustomerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Gesuchter Name aus Intent übernehmen (falls vorhanden)
        val customerName = intent.getStringExtra("CUSTOMER_NAME")
        if (!customerName.isNullOrEmpty()) {
            binding.etName.setText(customerName)
        }


        binding.btnBack.setOnClickListener { finish() }

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
            val kundenArt = when {
                binding.rbPrivat.isChecked -> "Privat"
                binding.rbListe.isChecked -> "Liste"
                else -> "Gewerblich"
            }
            
            // Button sofort deaktivieren und visuelles Feedback geben
            binding.btnSaveCustomer.isEnabled = false
            binding.btnSaveCustomer.text = "Speichere..."
            binding.btnSaveCustomer.alpha = 0.6f
            
            CoroutineScope(Dispatchers.Main).launch {
                val customerId = java.util.UUID.randomUUID().toString()
                
                // Kunden werden ohne Intervalle erstellt - Intervalle werden später über Regeln hinzugefügt
                val customer = Customer(
                    id = customerId,
                    name = name,
                    adresse = adresse,
                    telefon = telefon,
                    notizen = binding.etNotizen.text.toString().trim(),
                    // Kunden-Art
                    kundenArt = kundenArt,
                    listeId = "", // Keine Liste-Zuordnung mehr
                    // Intervalle werden später über "Termin Anlegen" hinzugefügt
                    intervalle = emptyList(),
                    // ALTE STRUKTUR: Für Rückwärtskompatibilität (alle auf 0/false gesetzt)
                    abholungDatum = 0,
                    auslieferungDatum = 0,
                    wiederholen = false,
                    intervallTage = 0,
                    letzterTermin = 0,
                    wochentag = 0,
                    istImUrlaub = false
                )

                // Speichern mit Retry-Logik
                var success: Boolean? = null
                try {
                    success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                        operation = { 
                            repository.saveCustomer(customer)
                        },
                        context = this@AddCustomerActivity,
                        errorMessage = "Fehler beim Speichern. Bitte erneut versuchen.",
                        maxRetries = 3
                    )
                } catch (e: Exception) {
                    android.util.Log.e("AddCustomer", "Exception in save operation", e)
                    success = null
                }
                
                // Prüfen ob erfolgreich
                val saveSuccessful = (success == true)
                
                // UI-Update auf Main-Thread
                runOnUiThread {
                    if (saveSuccessful) {
                        // Erfolg: Button-Text ändern und dann Activity schließen
                        binding.btnSaveCustomer.text = "✓ Gespeichert!"
                        binding.btnSaveCustomer.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            resources.getColor(com.example.we2026_5.R.color.status_done, theme)
                        )
                        binding.btnSaveCustomer.alpha = 1.0f
                        
                        // Kurz warten, damit der Benutzer das Feedback sieht, dann Activity schließen
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (!isFinishing) {
                                finish()
                            }
                        }, 800)
                    } else {
                        // Fehler: Button wieder aktivieren
                        binding.btnSaveCustomer.isEnabled = true
                        binding.btnSaveCustomer.text = "Speichern"
                        binding.btnSaveCustomer.alpha = 1.0f
                        // Toast wird bereits von FirebaseRetryHelper angezeigt (falls Fehler)
                    }
                }
            }
        }
    }

    // showDatumPicker Funktion entfernt - jetzt in IntervallManager
    
}
