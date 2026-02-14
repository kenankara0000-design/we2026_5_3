package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.we2026_5.auth.AdminChecker
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.ui.customermanager.CustomerManagerScreen
import com.example.we2026_5.ui.customermanager.CustomerManagerViewModel
import com.example.we2026_5.Customer
import com.example.we2026_5.customermanager.CustomerExportHelper
import com.example.we2026_5.ui.theme.AppTheme
import com.example.we2026_5.util.AppNavigation
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class CustomerManagerActivity : AppCompatActivity() {

    private val viewModel: CustomerManagerViewModel by viewModel()
    private val repository: CustomerRepository by inject()
    private val adminChecker: AdminChecker by inject()
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var exportHelper: CustomerExportHelper

    companion object {
        const val RESULT_CUSTOMER_DELETED = 2001
    }

    private val detailLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val customerId = result.data?.getStringExtra("DELETED_CUSTOMER_ID")
        if (result.resultCode == RESULT_CUSTOMER_DELETED && customerId != null) {
            deletedCustomerIds = deletedCustomerIds + customerId
        } else if (result.resultCode == RESULT_CANCELED && customerId != null) {
            deletedCustomerIds = deletedCustomerIds - customerId
        }
        if (result.resultCode == NextCustomerHelper.RESULT_OPEN_NEXT) {
            val ids: List<String> = lastDetailCustomerIds ?: return@registerForActivityResult
            val editedIndex = NextCustomerHelper.getNextCustomerIndex(result.data)
            val nextIndex = editedIndex + 1
            if (nextIndex < ids.size) {
                lastDetailIndex = nextIndex
                val nextIntent = AppNavigation.toCustomerDetail(this@CustomerManagerActivity, ids[nextIndex]).apply {
                    NextCustomerHelper.putNextCustomerExtras(this, ids, nextIndex)
                }
                window.decorView.post { launchDetail(nextIntent) }
            }
        }
    }

    private fun launchDetail(intent: Intent) {
        detailLauncher.launch(intent)
    }

    private var deletedCustomerIds by mutableStateOf(setOf<String>())
    private var pressedHeaderButton by mutableStateOf<String?>(null)
    private var lastDetailCustomerIds: List<String>? = null
    private var lastDetailIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        networkMonitor = NetworkMonitor(this, lifecycleScope)
        networkMonitor.startMonitoring()
        exportHelper = CustomerExportHelper(this, repository)

        setContent {
            AppTheme {
            val customers by viewModel.filteredCustomers.collectAsState(initial = emptyList())
            val selectedTab by viewModel.selectedTab.collectAsState(initial = 0)
            val kundenTypFilter by viewModel.kundenTypFilter.collectAsState(initial = 0)
            val ohneTourFilter by viewModel.ohneTourFilter.collectAsState(initial = 0)
            val keinetermineFilter by viewModel.keinetermineFilter.collectAsState(initial = 0)
            val pausierteFilter by viewModel.pausierteFilter.collectAsState(initial = 0)
            val isBulkMode by viewModel.isBulkMode.collectAsState(initial = false)
            val selectedIds by viewModel.selectedIds.collectAsState(initial = emptySet())
            val isLoading by viewModel.isLoading.collectAsState(initial = false)
            val errorMessage by viewModel.error.collectAsState(initial = null)
            val isOnline by networkMonitor.isOnline.observeAsState(initial = true)

            val displayCustomers = remember(customers, deletedCustomerIds) {
                customers.filter { it.id !in deletedCustomerIds }
            }

            CustomerManagerScreen(
                isAdmin = adminChecker.isAdmin(),
                customers = displayCustomers,
                selectedTab = selectedTab,
                kundenTypFilter = kundenTypFilter,
                ohneTourFilter = ohneTourFilter,
                keinetermineFilter = keinetermineFilter,
                onKeinetermineFilterChange = { viewModel.setKeinetermineFilter(it) },
                pausierteFilter = pausierteFilter,
                searchQuery = "",
                isBulkMode = isBulkMode,
                selectedIds = selectedIds,
                isOffline = !isOnline,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBack = { finish() },
                onTabSelected = { viewModel.setSelectedTab(it) },
                onKundenTypFilterChange = { viewModel.setKundenTypFilter(it) },
                onOhneTourFilterChange = { viewModel.setOhneTourFilter(it) },
                onPausierteFilterChange = { viewModel.setPausierteFilter(it) },
                onSearchQueryChange = { viewModel.filterCustomers(it) },
                pressedHeaderButton = pressedHeaderButton,
                onBulkSelectClick = { pressedHeaderButton = "Auswählen"; viewModel.setBulkMode(true) },
                onExportClick = { pressedHeaderButton = "Exportieren"; exportHelper.showExportDialog() },
                onNewCustomerClick = { pressedHeaderButton = "NeuerKunde"; startActivity(AppNavigation.toAddCustomer(this@CustomerManagerActivity)) },
                onCustomerClick = { customer ->
                    val ids = displayCustomers.map { it.id }
                    val index = ids.indexOf(customer.id).coerceAtLeast(0)
                    lastDetailCustomerIds = ids
                    lastDetailIndex = index
                    val intent = AppNavigation.toCustomerDetail(this@CustomerManagerActivity, customer.id).apply {
                        NextCustomerHelper.putNextCustomerExtras(this, ids, index)
                    }
                    launchDetail(intent)
                },
                onToggleSelection = { viewModel.toggleSelection(it) },
                onBulkDone = { selectedCustomers ->
                    AlertDialog.Builder(this@CustomerManagerActivity)
                        .setTitle(getString(R.string.dialog_mark_multiple_done_title))
                        .setMessage(getString(R.string.dialog_mark_multiple_done_message, selectedCustomers.size))
                        .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->
                            lifecycleScope.launch {
                                selectedCustomers.forEach { customer ->
                                    repository.updateCustomer(customer.id, mapOf(
                                        "abholungErfolgt" to true,
                                        "auslieferungErfolgt" to true
                                    ))
                                }
                                pressedHeaderButton = null
                                viewModel.setBulkMode(false)
                                Toast.makeText(this@CustomerManagerActivity, getString(R.string.toast_kunden_erledigt_markiert, selectedCustomers.size), Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton(getString(R.string.btn_cancel), null)
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
