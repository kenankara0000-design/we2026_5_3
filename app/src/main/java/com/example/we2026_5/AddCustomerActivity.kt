package com.example.we2026_5

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.databinding.ActivityAddCustomerBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import java.util.concurrent.TimeUnit

class AddCustomerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCustomerBinding
    private val db = FirebaseFirestore.getInstance()
    private var selectedStartDate: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCustomerBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            
            val ersterTermin = selectedStartDate
            val letzterTermin = ersterTermin - TimeUnit.DAYS.toMillis(intervall.toLong())

            val customerId = db.collection("customers").document().id
            val customer = Customer(
                id = customerId,
                name = name,
                adresse = binding.etAdresse.text.toString().trim(),
                telefon = binding.etTelefon.text.toString().trim(),
                notizen = binding.etNotizen.text.toString().trim(),
                intervallTage = intervall,
                letzterTermin = letzterTermin, 
                istImUrlaub = false
            )

            db.collection("customers").document(customerId).set(customer)
                .addOnSuccessListener {
                    Toast.makeText(this, "Kunde gespeichert", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { 
                    Toast.makeText(this, "Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateDateButtonText(dateInMillis: Long) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dateInMillis
        binding.btnPickDate.text = "${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}.${cal.get(Calendar.YEAR)}"
    }
}
