package com.example.we2026_5

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.we2026_5.ListItem
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.databinding.ActivityCustomerManagerBinding
import com.example.we2026_5.ui.customermanager.CustomerManagerViewModel
import com.example.we2026_5.customermanager.CustomerExportHelper
import com.example.we2026_5.customermanager.BulkSelectManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.util.Date

class CustomerManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerManagerBinding
    private val viewModel: CustomerManagerViewModel by viewModel()
    private val repository: CustomerRepository by inject()
    private lateinit var adapter: CustomerAdapter
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var exportHelper: CustomerExportHelper
    private lateinit var bulkSelectManager: BulkSelectManager
    private var pressedHeaderButton: String? = null // "Auswählen", "Exportieren", "NeuerKunde"
    private val deletedCustomerIds = mutableSetOf<String>() // Liste von gelöschten Kunden-IDs (optimistische UI-Aktualisierung)
    
    companion object {
        private const val REQUEST_CODE_CUSTOMER_DETAIL = 1001
        const val RESULT_CUSTOMER_DELETED = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CustomerAdapter(
            items = mutableListOf(),
            context = this,
            onClick = { customer ->
                val intent = Intent(this, CustomerDetailActivity::class.java).apply {
                    putExtra("CUSTOMER_ID", customer.id)
                }
                startActivityForResult(intent, REQUEST_CODE_CUSTOMER_DETAIL)
            }
        )

        binding.rvCustomerList.layoutManager = LinearLayoutManager(this)
        binding.rvCustomerList.adapter = adapter
        
        // TabLayout einrichten
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Gewerblich"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Privat"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Liste"))
        
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                tab?.position?.let { position ->
                    viewModel.setSelectedTab(position)
                }
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
        
        // Initial: Gewerblich-Tab ausgewählt
        viewModel.setSelectedTab(0)
        
        // ExportHelper initialisieren
        exportHelper = CustomerExportHelper(this, repository)
        
        // BulkSelectManager initialisieren
        bulkSelectManager = BulkSelectManager(
            activity = this,
            binding = binding,
            adapter = adapter,
            repository = repository,
            onSelectionChanged = {
                updateButtonStates()
            }
        )

        binding.btnBackFromManager.setOnClickListener { finish() }

        binding.btnNewCustomer.setOnClickListener {
            pressedHeaderButton = "NeuerKunde"
            updateHeaderButtonStates()
            val intent = Intent(this, AddCustomerActivity::class.java)
            startActivity(intent)
        }

        binding.btnExport.setOnClickListener {
            pressedHeaderButton = "Exportieren"
            updateHeaderButtonStates()
            exportHelper.showExportDialog()
        }

        binding.btnBulkSelect.setOnClickListener {
            pressedHeaderButton = "Auswählen"
            updateHeaderButtonStates()
            bulkSelectManager.enableMultiSelectMode()
        }

        binding.btnBulkCancel.setOnClickListener {
            bulkSelectManager.disableMultiSelectMode()
        }

        binding.btnBulkDone.setOnClickListener {
            bulkSelectManager.handleBulkDone()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.filterCustomers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Pull-to-Refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadCustomers()
            binding.swipeRefresh.isRefreshing = false
        }
        
        // Offline-Status-Monitoring
        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()
        networkMonitor.isOnline.observe(this) { isOnline ->
            binding.tvOfflineStatus.visibility = if (isOnline) View.GONE else View.VISIBLE
        }
        
        // ViewModel Observer einrichten
        observeViewModel()
        
        // Initial: Button-Zustände setzen
        updateButtonStates()
    }
    
    override fun onResume() {
        super.onResume()
        // Liste neu laden wenn Activity wieder sichtbar wird (z.B. nach Löschen eines Kunden)
        viewModel.loadCustomers()
        // Button-Zustand zurücksetzen wenn von AddCustomerActivity zurückgekehrt
        if (pressedHeaderButton == "NeuerKunde") {
            pressedHeaderButton = null
            updateHeaderButtonStates()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CUSTOMER_DETAIL) {
            val customerId = data?.getStringExtra("DELETED_CUSTOMER_ID")
            
            if (resultCode == RESULT_CUSTOMER_DELETED && customerId != null) {
                // Kunde wurde gelöscht - Liste sofort aktualisieren
                // Optimistische UI-Aktualisierung: Kunde sofort aus der Liste entfernen
                // Dies erfolgt BEVOR die Löschung abgeschlossen ist, für sofortiges visuelles Feedback
                deletedCustomerIds.add(customerId)
                android.util.Log.d("CustomerManager", "Added customer $customerId to deleted list (optimistic update)")
                
                // Adapter sofort aktualisieren
                adapter.removeCustomer(customerId)
                
                // Flow-Updates werden automatisch gefiltert, da deletedCustomerIds verwendet wird
                // Wenn die Löschung erfolgreich ist, wird der Kunde auch aus Realtime Database entfernt
                // und erscheint nicht mehr in zukünftigen Flow-Updates
            } else if (resultCode == RESULT_CANCELED && customerId != null) {
                // Löschung fehlgeschlagen - Kunde wieder zur Liste hinzufügen
                deletedCustomerIds.remove(customerId)
                android.util.Log.d("CustomerManager", "Removed customer $customerId from deleted list (deletion failed)")
                // Flow wird automatisch aktualisiert und zeigt den Kunden wieder an
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
    }
    
    private fun observeViewModel() {
        // Kunden-Liste beobachten
        viewModel.filteredCustomers.observe(this) { customers ->
            // Gelöschte Kunden aus der Liste filtern (optimistische UI-Aktualisierung)
            val filteredCustomers = customers.filter { customer ->
                !deletedCustomerIds.contains(customer.id)
            }
            
            adapter.updateData(filteredCustomers.map { ListItem.CustomerItem(it) })
            bulkSelectManager.updateBulkActionBar()
            updateButtonStates()
            
            // Empty State anzeigen wenn keine Kunden vorhanden
            if (filteredCustomers.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.rvCustomerList.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.rvCustomerList.visibility = View.VISIBLE
            }
        }
        
        // Loading-State beobachten
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.emptyStateLayout.visibility = View.GONE
                binding.errorStateLayout.visibility = View.GONE
            }
        }
        
        // Error-State beobachten
        viewModel.error.observe(this) { errorMessage ->
            if (errorMessage != null) {
                showErrorState(errorMessage)
            } else {
                binding.errorStateLayout.visibility = View.GONE
            }
        }
    }
    
    private fun showErrorState(message: String) {
        binding.errorStateLayout.visibility = View.VISIBLE
        binding.rvCustomerList.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.tvErrorMessage.text = message
        
        binding.btnRetry.setOnClickListener {
            viewModel.loadCustomers()
        }
    }
    
    // Export-Funktionen entfernt - jetzt in CustomerExportHelper
    
    // updateBulkActionBar Funktion entfernt - jetzt in BulkSelectManager
    
    private fun updateButtonStates() {
        // Header-Button-Zustände aktualisieren
        updateHeaderButtonStates()
    }
    
    
    private fun updateHeaderButtonStates() {
        // Farben definieren
        val activeBackgroundColor = ContextCompat.getColor(this, R.color.status_warning) // Orange
        val inactiveBackgroundColor = ContextCompat.getColor(this, R.color.button_blue) // Blau
        val textColor = ContextCompat.getColor(this, R.color.white)
        
        // Button-Zeile bleibt immer blau
        val barBackgroundColor = ContextCompat.getColor(this, R.color.primary_blue)
        binding.buttonBar.setBackgroundColor(barBackgroundColor)
        
        when (pressedHeaderButton) {
            "Auswählen" -> {
                // Aktiver Button: Orange Hintergrund
                binding.btnBulkSelect.setBackgroundColor(activeBackgroundColor)
                binding.btnBulkSelect.setTextColor(textColor)
                binding.btnBulkSelect.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                // Inaktiver Button: Blau Hintergrund
                binding.btnExport.setBackgroundColor(inactiveBackgroundColor)
                binding.btnExport.setTextColor(textColor)
                binding.btnExport.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                // FAB: Blau wenn nicht aktiv
                binding.btnNewCustomer.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveBackgroundColor)
            }
            "Exportieren" -> {
                // Aktiver Button: Orange Hintergrund
                binding.btnExport.setBackgroundColor(activeBackgroundColor)
                binding.btnExport.setTextColor(textColor)
                binding.btnExport.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                // Inaktiver Button: Blau Hintergrund
                binding.btnBulkSelect.setBackgroundColor(inactiveBackgroundColor)
                binding.btnBulkSelect.setTextColor(textColor)
                binding.btnBulkSelect.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                // FAB: Blau wenn nicht aktiv
                binding.btnNewCustomer.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveBackgroundColor)
            }
            "NeuerKunde" -> {
                // FAB: Orange wenn aktiv
                binding.btnNewCustomer.backgroundTintList = android.content.res.ColorStateList.valueOf(activeBackgroundColor)
                
                // Andere Buttons: Blau
                binding.btnBulkSelect.setBackgroundColor(inactiveBackgroundColor)
                binding.btnBulkSelect.setTextColor(textColor)
                binding.btnBulkSelect.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                binding.btnExport.setBackgroundColor(inactiveBackgroundColor)
                binding.btnExport.setTextColor(textColor)
                binding.btnExport.iconTint = android.content.res.ColorStateList.valueOf(textColor)
            }
            else -> {
                // Kein Button gedrückt: Prüfe ob Multi-Select aktiv ist
                val isMultiSelectActive = bulkSelectManager.isMultiSelectActive()
                if (isMultiSelectActive) {
                    // Multi-Select aktiv: Auswählen-Button orange
                    binding.btnBulkSelect.setBackgroundColor(activeBackgroundColor)
                    binding.btnBulkSelect.setTextColor(textColor)
                    binding.btnBulkSelect.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                } else {
                    // Multi-Select inaktiv: Auswählen-Button blau
                    binding.btnBulkSelect.setBackgroundColor(inactiveBackgroundColor)
                    binding.btnBulkSelect.setTextColor(textColor)
                    binding.btnBulkSelect.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                }
                
                // Export-Button immer blau wenn nicht aktiv
                binding.btnExport.setBackgroundColor(inactiveBackgroundColor)
                binding.btnExport.setTextColor(textColor)
                binding.btnExport.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                // FAB: Blau wenn nicht aktiv
                binding.btnNewCustomer.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveBackgroundColor)
            }
        }
    }
    
    // markBulkAsDone Funktion entfernt - jetzt in BulkSelectManager
}
