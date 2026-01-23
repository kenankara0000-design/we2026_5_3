package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.databinding.ActivityMainBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- OFFLINE FUNKTION AKTIVIEREN ---
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Daten lokal speichern
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        db.firestoreSettings = settings
        // -----------------------------------

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnKunden.setOnClickListener {
            startActivity(Intent(this, CustomerManagerActivity::class.java))
        }

        binding.btnTouren.setOnClickListener {
            startActivity(Intent(this, TourPlannerActivity::class.java))
        }

        binding.btnNeuerKunde.setOnClickListener {
            startActivity(Intent(this, AddCustomerActivity::class.java))
        }

        updateTourCount()
    }

    private fun updateTourCount() {
        db.collection("customers").addSnapshotListener { snapshot, _ ->
            val all = snapshot?.toObjects(Customer::class.java) ?: listOf()
            val heute = System.currentTimeMillis()
            val count = all.count {
                val faelligAm = it.letzterTermin + TimeUnit.DAYS.toMillis(it.intervallTage.toLong())
                heute >= faelligAm && !it.istImUrlaub
            }
            binding.btnTouren.text = "Tour Planner ($count fällig)"
        }
    }
}