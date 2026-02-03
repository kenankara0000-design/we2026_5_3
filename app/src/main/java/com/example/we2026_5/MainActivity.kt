package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.lifecycleScope
import com.example.we2026_5.ui.main.MainScreen
import com.example.we2026_5.ui.main.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModel()
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

        window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_light))
        setContent {
            val tourCount by viewModel.tourFälligCount.observeAsState(0)
            val isOnline by networkMonitor.isOnline.observeAsState(true)
            val isSyncing by networkMonitor.isSyncing.observeAsState(false)

            MainScreen(
                isOffline = !isOnline,
                isSyncing = isSyncing,
                tourCount = tourCount,
                onNeuKunde = { startActivity(Intent(this@MainActivity, AddCustomerActivity::class.java)) },
                onKunden = { startActivity(Intent(this@MainActivity, CustomerManagerActivity::class.java)) },
                onTouren = { startActivity(Intent(this@MainActivity, TourPlannerActivity::class.java)) },
                onKundenListen = { startActivity(Intent(this@MainActivity, KundenListenActivity::class.java)) },
                onStatistiken = { startActivity(Intent(this@MainActivity, StatisticsActivity::class.java)) },
                onTerminRegeln = { startActivity(Intent(this@MainActivity, TerminRegelManagerActivity::class.java)) }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::networkMonitor.isInitialized) {
            networkMonitor.stopMonitoring()
        }
    }
}
