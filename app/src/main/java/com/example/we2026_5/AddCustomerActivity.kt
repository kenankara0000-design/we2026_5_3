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

        binding.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val picked = Calendar.getInstance()
                picked.set(y, m, d, 0, 0, 0)
                selectedStartDate = picked.timeInMillis
                binding.btnPickDate.text = "$d.${m + 1}.$y"
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSaveCustomer.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                binding.etName.error = "Name fehlt"
                return@setOnClickListener
            }

            val intervall = binding.etIntervallTage.text.toString().toIntOrNull() ?: 7

            // **KORRIGIERTE LOGIK:** Berechnet den "letzten Termin" so, dass der Kunde
            // am ausgewählten Startdatum fällig ist.
            val letzterTermin = selectedStartDate - TimeUnit.DAYS.toMillis(intervall.toLong())

            val customerId = db.collection("customers").document().id
            val customer = Customer(
                id = customerId,
                name = name,
                adresse = binding.etAdresse.text.toString().trim(),
                telefon = binding.etTelefon.text.toString().trim(),
                notizen = binding.etNotizen.text.toString().trim(),
                intervallTage = intervall,
                letzterTermin = letzterTermin, // Korrigierter Wert wird hier verwendet
                istImUrlaub = false
            )

            // OFFLINE-LOGIK: Wir schicken es ab und schließen SOFORT.
            // Firebase kümmert sich im Hintergrund um den Rest.
            db.collection("customers").document(customerId).set(customer)

            Toast.makeText(this, "Kunde lokal gespeichert!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
