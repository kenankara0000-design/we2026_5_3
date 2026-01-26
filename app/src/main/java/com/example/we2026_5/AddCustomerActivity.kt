package com.example.we2026_5

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
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
    private var intervalle = mutableListOf<CustomerIntervall>()
    private lateinit var intervallAdapter: IntervallAdapter
    private var aktuellesIntervallPosition: Int = -1
    private var aktuellesDatumTyp: Boolean = true // true = Abholung, false = Auslieferung

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

        // Intervall-Card initial anzeigen für Gewerblich-Kunden (Standard)
        binding.cardIntervall.visibility = android.view.View.VISIBLE

        // Intervall-Adapter initialisieren
        intervallAdapter = IntervallAdapter(
            intervalle = intervalle,
            onIntervallChanged = { neueIntervalle ->
                intervalle.clear()
                intervalle.addAll(neueIntervalle)
            },
            onDatumSelected = { position, isAbholung ->
                aktuellesIntervallPosition = position
                aktuellesDatumTyp = isAbholung
                showDatumPicker(position, isAbholung)
            }
        )
        binding.rvIntervalle.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvIntervalle.adapter = intervallAdapter

        // Intervall hinzufügen Button
        binding.btnIntervallHinzufuegen.setOnClickListener {
            val neuesIntervall = CustomerIntervall()
            intervallAdapter.addIntervall(neuesIntervall)
        }

        // Kunden-Art ändern: Intervall-Card anzeigen/ausblenden
        binding.rgKundenArt.setOnCheckedChangeListener { _, checkedId ->
            val isGewerblich = checkedId == binding.rbGewerblich.id
            val isListe = checkedId == binding.rbListe.id
            
            // Intervall-Card anzeigen für Gewerblich-Kunden und Liste-Kunden
            binding.cardIntervall.visibility = if (isGewerblich || isListe) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }

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
                
                // NEUE STRUKTUR: Intervalle für Gewerblich-Kunden und Liste-Kunden
                val customerIntervalle = if ((kundenArt == "Gewerblich" || kundenArt == "Liste") && intervalle.isNotEmpty()) {
                    intervalle.toList()
                } else {
                    emptyList()
                }
                
                val customer = Customer(
                    id = customerId,
                    name = name,
                    adresse = adresse,
                    telefon = telefon,
                    notizen = binding.etNotizen.text.toString().trim(),
                    // Kunden-Art
                    kundenArt = kundenArt,
                    listeId = "", // Keine Liste-Zuordnung mehr
                    // NEUE STRUKTUR: Intervalle-Liste
                    intervalle = customerIntervalle,
                    // ALTE STRUKTUR: Für Rückwärtskompatibilität (wird ignoriert wenn intervalle vorhanden)
                    abholungDatum = if (customerIntervalle.isNotEmpty()) 0 else 0,
                    auslieferungDatum = if (customerIntervalle.isNotEmpty()) 0 else 0,
                    wiederholen = customerIntervalle.any { it.wiederholen },
                    intervallTage = if (customerIntervalle.isNotEmpty()) 0 else 7,
                    letzterTermin = if (customerIntervalle.isNotEmpty()) 0 else 0,
                    wochentag = 0, // Wochentag wird nicht mehr verwendet
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

    private fun showDatumPicker(position: Int, isAbholung: Boolean) {
        val cal = Calendar.getInstance()
        val intervall = intervalle.getOrNull(position) ?: return
        
        // Aktuelles Datum oder Intervall-Datum verwenden
        val initialDatum = if (isAbholung && intervall.abholungDatum > 0) {
            cal.timeInMillis = intervall.abholungDatum
            intervall.abholungDatum
        } else if (!isAbholung && intervall.auslieferungDatum > 0) {
            cal.timeInMillis = intervall.auslieferungDatum
            intervall.auslieferungDatum
        } else {
            System.currentTimeMillis()
        }
        
        cal.timeInMillis = initialDatum
        
        DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { _, year: Int, month: Int, dayOfMonth: Int ->
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val selectedDatum = cal.timeInMillis
                
                // Intervall aktualisieren
                val updatedIntervall = if (isAbholung) {
                    intervall.copy(abholungDatum = selectedDatum)
                } else {
                    intervall.copy(auslieferungDatum = selectedDatum)
                }
                
                intervalle[position] = updatedIntervall
                intervallAdapter.updateIntervalle(intervalle.toList())
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
}
