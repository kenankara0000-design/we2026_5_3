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
import java.util.concurrent.TimeUnit
import org.koin.android.ext.android.inject

class AddCustomerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCustomerBinding
    private val repository: CustomerRepository by inject()
    private var selectedStartDate: Long = System.currentTimeMillis()
    private var selectedWochentag: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2 // 0=Montag
    private val wochentagButtons = mutableListOf<android.widget.Button>()

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

        updateDateButtonText(selectedStartDate)

        binding.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.timeInMillis = selectedStartDate
            DatePickerDialog(this, { _, y, m, d ->
                val picked = Calendar.getInstance()
                picked.set(y, m, d, 0, 0, 0)
                selectedStartDate = picked.timeInMillis
                updateDateButtonText(selectedStartDate)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSaveCustomer.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                binding.etName.error = "Name fehlt"
                return@setOnClickListener
            }

            val intervallInput = binding.etIntervallTage.text.toString().toIntOrNull() ?: 7
            // Validierung: Intervall muss mindestens 1 Tag sein und maximal 365 Tage
            val intervall = when {
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
            
            val reihenfolgeInput = binding.etReihenfolge.text.toString().toIntOrNull() ?: 1
            val reihenfolge = when {
                reihenfolgeInput < 1 -> {
                    binding.etReihenfolge.error = "Reihenfolge muss mindestens 1 sein"
                    return@setOnClickListener
                }
                else -> reihenfolgeInput
            }
            
            // Button deaktivieren während Speichern
            binding.btnSaveCustomer.isEnabled = false
            binding.btnSaveCustomer.text = "Speichere..."
            
            val ersterTermin = selectedStartDate
            val letzterTermin = ersterTermin - TimeUnit.DAYS.toMillis(intervall.toLong())

            val customerId = java.util.UUID.randomUUID().toString()
            val customer = Customer(
                id = customerId,
                name = name,
                adresse = binding.etAdresse.text.toString().trim(),
                telefon = binding.etTelefon.text.toString().trim(),
                notizen = binding.etNotizen.text.toString().trim(),
                intervallTage = intervall,
                letzterTermin = letzterTermin, 
                istImUrlaub = false,
                wochentag = selectedWochentag,
                reihenfolge = reihenfolge
            )

            // Speichern mit Retry-Logik
            CoroutineScope(Dispatchers.Main).launch {
                val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                    operation = { 
                        repository.saveCustomer(customer)
                    },
                    context = this@AddCustomerActivity,
                    errorMessage = "Fehler beim Speichern. Bitte erneut versuchen.",
                    maxRetries = 3
                )
                
                if (success == true) {
                    Toast.makeText(this@AddCustomerActivity, "Kunde erfolgreich gespeichert!", Toast.LENGTH_SHORT).show()
                    // Sicherstellen, dass finish() auf Main-Thread aufgerufen wird
                    runOnUiThread {
                        finish()
                    }
                } else {
                    // Button wieder aktivieren bei Fehler
                    runOnUiThread {
                        binding.btnSaveCustomer.isEnabled = true
                        binding.btnSaveCustomer.text = "Kunde JETZT Speichern"
                    }
                }
            }
        }
    }

    private fun updateDateButtonText(dateInMillis: Long) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dateInMillis
        binding.btnPickDate.text = "${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}.${cal.get(Calendar.YEAR)}"
    }
    
    private fun selectWochentag(tag: Int) {
        selectedWochentag = tag
        updateWochentagButtons()
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
}
