package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.we2026_5.databinding.ActivityMainBinding
import com.example.we2026_5.ui.main.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModel()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var networkMonitor: NetworkMonitor

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
        
        // Offline-Status beobachten (inkl. Hinweis zur Synchronisation)
        networkMonitor.isOnline.observe(this, Observer { isOnline ->
            val offline = !isOnline
            binding.tvOfflineStatus.visibility = if (offline) View.VISIBLE else View.GONE
            binding.tvOfflineSyncHinweis.visibility = if (offline) View.VISIBLE else View.GONE
        })
        
        // Synchronisierungs-Status beobachten
        networkMonitor.isSyncing.observe(this, Observer { isSyncing ->
            binding.tvSyncStatus.visibility = if (isSyncing) View.VISIBLE else View.GONE
        })

        // Tour-Count aus ViewModel (Flow-basiert)
        viewModel.tourFälligCount.observe(this, Observer { count ->
            binding.btnTouren.text = getString(R.string.main_tour_btn_with_count, count)
        })
    }
    
    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
    }

    override fun onStart() {
        super.onStart()
    }
}
