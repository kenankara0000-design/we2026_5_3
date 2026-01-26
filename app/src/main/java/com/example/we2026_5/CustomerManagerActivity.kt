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
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.we2026_5.ListItem
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.databinding.ActivityCustomerManagerBinding
import com.example.we2026_5.ui.customermanager.CustomerManagerViewModel
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
                startActivity(intent)
            }
        )

        binding.rvCustomerList.layoutManager = LinearLayoutManager(this)
        binding.rvCustomerList.adapter = adapter

        binding.btnBackFromManager.setOnClickListener { finish() }

        binding.btnNewCustomer.setOnClickListener {
            val intent = Intent(this, AddCustomerActivity::class.java)
            startActivity(intent)
        }

        binding.btnExport.setOnClickListener {
            showExportDialog()
        }

        binding.btnBulkSelect.setOnClickListener {
            adapter.enableMultiSelectMode()
            updateBulkActionBar()
            updateButtonStates()
        }

        binding.btnBulkCancel.setOnClickListener {
            adapter.disableMultiSelectMode()
            updateBulkActionBar()
            updateButtonStates()
        }

        binding.btnBulkDone.setOnClickListener {
            val selected = adapter.getSelectedCustomers()
            if (selected.isNotEmpty()) {
                markBulkAsDone(selected)
            }
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
    
    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
    }
    
    private fun observeViewModel() {
        // Kunden-Liste beobachten
        viewModel.filteredCustomers.observe(this) { customers ->
            adapter.updateData(customers.map { ListItem.CustomerItem(it) })
            updateBulkActionBar()
            updateButtonStates()
            
            // Empty State anzeigen wenn keine Kunden vorhanden
            if (customers.isEmpty()) {
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
    
    private fun showExportDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exportieren")
            .setItems(arrayOf("Als CSV exportieren", "Als Text exportieren")) { _, which ->
                when (which) {
                    0 -> exportAsCSV()
                    1 -> exportAsText()
                }
            }
            .show()
    }
    
    private fun exportAsCSV() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val allCustomers = repository.getAllCustomers()
                val file = ExportHelper.exportToCSV(this@CustomerManagerActivity, allCustomers)
                
                if (file != null) {
                    shareFile(file, "text/csv")
                    Toast.makeText(this@CustomerManagerActivity, "CSV exportiert: ${file.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@CustomerManagerActivity, "Fehler beim Exportieren", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CustomerManagerActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun exportAsText() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val allCustomers = repository.getAllCustomers()
                val file = ExportHelper.exportTourAsText(this@CustomerManagerActivity, allCustomers, Date())
                
                if (file != null) {
                    shareFile(file, "text/plain")
                    Toast.makeText(this@CustomerManagerActivity, "Text exportiert: ${file.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@CustomerManagerActivity, "Fehler beim Exportieren", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CustomerManagerActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            this,
            "com.example.we2026_5.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(Intent.createChooser(shareIntent, "Datei teilen"))
    }
    
    private fun updateBulkActionBar() {
        val hasSelection = adapter.hasSelectedCustomers()
        binding.bulkActionBar.visibility = if (hasSelection) View.VISIBLE else View.GONE
        binding.btnBulkSelect.visibility = if (hasSelection) View.GONE else View.VISIBLE
        
        if (hasSelection) {
            val count = adapter.getSelectedCustomers().size
            binding.tvSelectedCount.text = "$count ausgewählt"
        }
    }
    
    private fun updateButtonStates() {
        val isMultiSelectActive = adapter.hasSelectedCustomers() || adapter.isMultiSelectModeEnabled()
        binding.btnBulkSelect.isSelected = isMultiSelectActive
        if (isMultiSelectActive) {
            binding.btnBulkSelect.background = resources.getDrawable(com.example.we2026_5.R.drawable.button_icon_active, theme)
        } else {
            binding.btnBulkSelect.background = resources.getDrawable(com.example.we2026_5.R.drawable.button_icon_pressed, theme)
        }
    }
    
    private fun markBulkAsDone(customers: List<Customer>) {
        AlertDialog.Builder(this)
            .setTitle("Mehrere Kunden als erledigt markieren?")
            .setMessage("${customers.size} Kunden werden als erledigt markiert (Abholung + Auslieferung).")
            .setPositiveButton("Ja") { _, _ ->
                CoroutineScope(Dispatchers.Main).launch {
                    customers.forEach { customer ->
                        val updates = mapOf(
                            "abholungErfolgt" to true,
                            "auslieferungErfolgt" to true
                        )
                        repository.updateCustomer(customer.id, updates)
                    }
                    adapter.disableMultiSelectMode()
                    updateBulkActionBar()
                    updateButtonStates()
                    Toast.makeText(this@CustomerManagerActivity, "${customers.size} Kunden als erledigt markiert", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
}
