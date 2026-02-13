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
import com.example.we2026_5.auth.AdminChecker
import com.example.we2026_5.ui.main.MainScreen
import com.example.we2026_5.ui.main.MainViewModel
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.util.AppNavigation
import com.example.we2026_5.util.runListeArtToTourMigration
import com.example.we2026_5.util.runListeArtTourToListenkundenMigration
import com.example.we2026_5.util.runListeIntervalleMigration
import com.example.we2026_5.util.runListeToTourMigration
import com.example.we2026_5.util.runPauseExpiredReset
import com.example.we2026_5.util.runRemoveDeprecatedFieldsMigration
import com.example.we2026_5.util.runListenPrivatKundenpreiseMigration
import com.example.we2026_5.util.runTourToListenkundenMigration
import com.example.we2026_5.ui.theme.AppTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModel()
    private val customerRepository: CustomerRepository by inject()
    private val listeRepository: KundenListeRepository by inject()
    private val database: com.google.firebase.database.FirebaseDatabase by inject()
    private val adminChecker: AdminChecker by inject()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            startActivity(AppNavigation.toLogin(this))
            finish()
            return
        }

        networkMonitor = NetworkMonitor(this, lifecycleScope)
        networkMonitor.startMonitoring()

        lifecycleScope.launch(Dispatchers.IO) {
            runListeToTourMigration(this@MainActivity, customerRepository)
            runListeArtToTourMigration(this@MainActivity, listeRepository)
            runTourToListenkundenMigration(this@MainActivity, customerRepository)
            runListeArtTourToListenkundenMigration(this@MainActivity, listeRepository)
            runListenPrivatKundenpreiseMigration(this@MainActivity, database)
            runListeIntervalleMigration(this@MainActivity, customerRepository, listeRepository)
            runRemoveDeprecatedFieldsMigration(this@MainActivity, customerRepository)
            runPauseExpiredReset(customerRepository)
        }

        window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_light))
        setContent {
            AppTheme {
                val tourCount by viewModel.tourFälligCount.observeAsState(0)
                val isOnline by networkMonitor.isOnline.observeAsState(true)
                val isSyncing by networkMonitor.isSyncing.observeAsState(false)
                val slotVorschlaege by viewModel.slotVorschlaege.observeAsState(emptyList())
                val isAdmin = adminChecker.isAdmin()

                MainScreen(
                    isAdmin = isAdmin,
                    isOffline = !isOnline,
                    isSyncing = isSyncing,
                    tourCount = tourCount,
                    slotVorschlaege = slotVorschlaege,
                    onNeuKunde = { startActivity(AppNavigation.toAddCustomer(this@MainActivity)) },
                    onKunden = { startActivity(AppNavigation.toCustomerManager(this@MainActivity)) },
                    onTouren = { startActivity(AppNavigation.toTourPlanner(this@MainActivity)) },
                    onKundenListen = { startActivity(AppNavigation.toKundenListen(this@MainActivity)) },
                    onStatistiken = { startActivity(AppNavigation.toStatistics(this@MainActivity)) },
                    onErfassung = { startActivity(AppNavigation.toErfassungMenu(this@MainActivity)) },
                    onSettings = { startActivity(AppNavigation.toSettings(this@MainActivity)) },
                    onSlotSelected = { slot -> handleSlotSelection(slot) }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopCollecting()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startCollecting()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopCollecting()
        if (::networkMonitor.isInitialized) {
            networkMonitor.stopMonitoring()
        }
    }

    private fun handleSlotSelection(slot: TerminSlotVorschlag) {
        if (slot.customerId.isBlank()) {
            Toast.makeText(this, getString(R.string.error_customer_not_found), Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(AppNavigation.toCustomerDetail(this, customerId = slot.customerId))
    }
}
