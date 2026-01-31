package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.ui.customermanager.CustomerManagerScreen
import com.example.we2026_5.ui.customermanager.CustomerManagerViewModel
import com.example.we2026_5.Customer
import com.example.we2026_5.customermanager.CustomerExportHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class CustomerManagerActivity : AppCompatActivity() {

    private val viewModel: CustomerManagerViewModel by viewModel()
    private val repository: CustomerRepository by inject()
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var exportHelper: CustomerExportHelper

    companion object {
        const val RESULT_CUSTOMER_DELETED = 2001
    }

    private val detailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val customerId = result.data?.getStringExtra("DELETED_CUSTOMER_ID")
        if (result.resultCode == RESULT_CUSTOMER_DELETED && customerId != null) {
            deletedCustomerIds = deletedCustomerIds + customerId
        } else if (result.resultCode == RESULT_CANCELED && customerId != null) {
            deletedCustomerIds = deletedCustomerIds - customerId
        }
    }

    private var deletedCustomerIds by mutableStateOf(setOf<String>())
    private var pressedHeaderButton by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()
        exportHelper = CustomerExportHelper(this, repository)

        setContent {
            val customers by viewModel.filteredCustomers.observeAsState(initial = emptyList())
            val selectedTab by viewModel.selectedTab.collectAsState(initial = 0)
            val isBulkMode by viewModel.isBulkMode.collectAsState(initial = false)
            val selectedIds by viewModel.selectedIds.collectAsState(initial = emptySet())
            val isLoading by viewModel.isLoading.observeAsState(initial = false)
            val errorMessage by viewModel.error.observeAsState(initial = null)
            val isOnline by networkMonitor.isOnline.observeAsState(initial = true)

            val displayCustomers = remember(customers, deletedCustomerIds) {
                customers.filter { it.id !in deletedCustomerIds }
            }

            CustomerManagerScreen(
                customers = displayCustomers,
                selectedTab = selectedTab,
                searchQuery = "",
                isBulkMode = isBulkMode,
                selectedIds = selectedIds,
                isOffline = !isOnline,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBack = { finish() },
                onTabSelected = { viewModel.setSelectedTab(it) },
                onSearchQueryChange = { viewModel.filterCustomers(it) },
                pressedHeaderButton = pressedHeaderButton,
                onBulkSelectClick = { pressedHeaderButton = "Auswählen"; viewModel.setBulkMode(true) },
                onExportClick = { pressedHeaderButton = "Exportieren"; exportHelper.showExportDialog() },
                onNewCustomerClick = { pressedHeaderButton = "NeuerKunde"; startActivity(Intent(this@CustomerManagerActivity, AddCustomerActivity::class.java)) },
                onCustomerClick = { customer ->
                    val intent = Intent(this@CustomerManagerActivity, CustomerDetailActivity::class.java).apply {
                        putExtra("CUSTOMER_ID", customer.id)
                    }
                    detailLauncher.launch(intent)
                },
                onToggleSelection = { viewModel.toggleSelection(it) },
                onBulkDone = { selectedCustomers ->
                    AlertDialog.Builder(this@CustomerManagerActivity)
                        .setTitle("Mehrere Kunden als erledigt markieren?")
                        .setMessage("${selectedCustomers.size} Kunden werden als erledigt markiert (Abholung + Auslieferung).")
                        .setPositiveButton("Ja") { _, _ ->
                            CoroutineScope(Dispatchers.Main).launch {
                                selectedCustomers.forEach { customer ->
                                    repository.updateCustomer(customer.id, mapOf(
                                        "abholungErfolgt" to true,
                                        "auslieferungErfolgt" to true
                                    ))
                                }
                                pressedHeaderButton = null
                                viewModel.setBulkMode(false)
                                Toast.makeText(this@CustomerManagerActivity, "${selectedCustomers.size} Kunden als erledigt markiert", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("Abbrechen", null)
                        .show()
                },
                onBulkCancel = { pressedHeaderButton = null; viewModel.setBulkMode(false) },
                onRetry = {
                    viewModel.clearError()
                    viewModel.loadCustomers()
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // viewModel.loadCustomers() // Nicht mehr nötig durch Flow-Listener im ViewModel
        pressedHeaderButton = null
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
    }
}
