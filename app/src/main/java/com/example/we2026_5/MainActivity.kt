package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repository: CustomerRepository by inject()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prüfen ob eingeloggt
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

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

        binding.btnStatistiken.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        updateTourCount()
    }

    private fun updateTourCount() {
        repository.addCustomersListener(
            onUpdate = { customers ->
                val heute = System.currentTimeMillis()
                val count = customers.count { customer ->
                    val faelligAm = customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
                    // istImUrlaub konsistent berechnen wie in TourPlannerActivity
                    val imUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                   heute in customer.urlaubVon..customer.urlaubBis
                    heute >= faelligAm && !imUrlaub
                }
                binding.btnTouren.text = "Tour Planner ($count fällig)"
            },
            onError = { 
                // Fehler ignorieren für Tour-Count
            }
        )
    }
}
