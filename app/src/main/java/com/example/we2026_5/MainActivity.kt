package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.databinding.ActivityMainBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db = FirebaseFirestore.getInstance()
    private var tourCountListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        db.firestoreSettings = settings

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
    }

    override fun onStart() {
        super.onStart()
        updateTourCount()
    }

    override fun onStop() {
        super.onStop()
        tourCountListener?.remove()
    }

    private fun updateTourCount() {
        tourCountListener = db.collection("customers").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener

            val all = snapshot.toObjects(Customer::class.java)
            val heute = System.currentTimeMillis()
            val count = all.count { customer ->
                val faelligAm = customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
                heute >= faelligAm && !customer.istImUrlaub
            }
            binding.btnTouren.text = "Tour Planner ($count fällig)"
        }
    }
}
