package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repository: CustomerRepository by inject()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var networkMonitor: NetworkMonitor
    private var customersListener: ValueEventListener? = null

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

        binding.btnNeuKunde.setOnClickListener {
            startActivity(Intent(this, AddCustomerActivity::class.java))
        }

        binding.btnKunden.setOnClickListener {
            startActivity(Intent(this, CustomerManagerActivity::class.java))
        }

        binding.btnTouren.setOnClickListener {
            startActivity(Intent(this, TourPlannerActivity::class.java))
        }

        binding.btnKundenListen.setOnClickListener {
            startActivity(Intent(this, KundenListenActivity::class.java))
        }

        binding.btnStatistiken.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        binding.btnTerminRegeln.setOnClickListener {
            startActivity(Intent(this, TerminRegelManagerActivity::class.java))
        }
        
        // Globaler NetworkMonitor für Offline-Status
        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()
        
        // Offline-Status beobachten
        networkMonitor.isOnline.observe(this, Observer { isOnline ->
            binding.tvOfflineStatus.visibility = if (isOnline) View.GONE else View.VISIBLE
        })
        
        // Synchronisierungs-Status beobachten
        networkMonitor.isSyncing.observe(this, Observer { isSyncing ->
            binding.tvSyncStatus.visibility = if (isSyncing) View.VISIBLE else View.GONE
        })
    }
    
    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
        customersListener?.let { repository.removeListener(it) }
    }

    override fun onStart() {
        super.onStart()
        updateTourCount()
    }

    private fun updateTourCount() {
        customersListener = repository.addCustomersListener(
            onUpdate = { customers ->
                val heute = System.currentTimeMillis()
                val count = customers.count { customer ->
                    val faelligAm = customer.getFaelligAm()
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
