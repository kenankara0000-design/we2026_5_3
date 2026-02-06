package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.we2026_5.TerminSlotVorschlag
import com.example.we2026_5.ui.main.MainScreen
import com.example.we2026_5.ui.main.MainViewModel
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.util.runListeIntervalleMigration
import com.example.we2026_5.util.runListeToTourMigration
import com.google.firebase.auth.FirebaseAuth
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModel()
    private val customerRepository: CustomerRepository by inject()
    private val listeRepository: KundenListeRepository by inject()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        networkMonitor = NetworkMonitor(this, lifecycleScope)
        networkMonitor.startMonitoring()

        lifecycleScope.launch {
            runListeToTourMigration(this@MainActivity, customerRepository)
            runListeIntervalleMigration(this@MainActivity, customerRepository, listeRepository)
        }

        window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_light))
        setContent {
            val tourCount by viewModel.tourFälligCount.observeAsState(0)
            val isOnline by networkMonitor.isOnline.observeAsState(true)
            val isSyncing by networkMonitor.isSyncing.observeAsState(false)
            val slotVorschlaege by viewModel.slotVorschlaege.observeAsState(emptyList())

            MainScreen(
                isOffline = !isOnline,
                isSyncing = isSyncing,
                tourCount = tourCount,
                slotVorschlaege = slotVorschlaege,
                onNeuKunde = { startActivity(Intent(this@MainActivity, AddCustomerActivity::class.java)) },
                onKunden = { startActivity(Intent(this@MainActivity, CustomerManagerActivity::class.java)) },
                onTouren = { startActivity(Intent(this@MainActivity, TourPlannerActivity::class.java)) },
                onKundenListen = { startActivity(Intent(this@MainActivity, KundenListenActivity::class.java)) },
                onStatistiken = { startActivity(Intent(this@MainActivity, StatisticsActivity::class.java)) },
                onSlotSelected = { slot -> handleSlotSelection(slot) }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::networkMonitor.isInitialized) {
            networkMonitor.stopMonitoring()
        }
    }

    private fun handleSlotSelection(slot: TerminSlotVorschlag) {
        if (slot.customerId.isBlank()) {
            Toast.makeText(this, getString(R.string.error_customer_not_found), Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, CustomerDetailActivity::class.java).apply {
            putExtra("CUSTOMER_ID", slot.customerId)
        })
    }
}
