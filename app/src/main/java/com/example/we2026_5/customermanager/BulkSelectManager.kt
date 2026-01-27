package com.example.we2026_5.customermanager

import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerAdapter
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.databinding.ActivityCustomerManagerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manager für Bulk-Select-Funktionalität in CustomerManagerActivity.
 * Extrahiert die Multi-Select-Aktivierung, Bulk-Operationen und Selection-State-Management.
 */
class BulkSelectManager(
    private val activity: android.app.Activity,
    private val binding: ActivityCustomerManagerBinding,
    private val adapter: CustomerAdapter,
    private val repository: CustomerRepository,
    private val onSelectionChanged: () -> Unit
) {
    
    fun enableMultiSelectMode() {
        adapter.enableMultiSelectMode()
        updateBulkActionBar()
        onSelectionChanged()
    }
    
    fun disableMultiSelectMode() {
        adapter.disableMultiSelectMode()
        updateBulkActionBar()
        onSelectionChanged()
    }
    
    fun updateBulkActionBar() {
        val hasSelection = adapter.hasSelectedCustomers()
        binding.bulkActionBar.visibility = if (hasSelection) View.VISIBLE else View.GONE
        binding.btnBulkSelect.visibility = if (hasSelection) View.GONE else View.VISIBLE
        
        if (hasSelection) {
            val count = adapter.getSelectedCustomers().size
            binding.tvSelectedCount.text = "$count ausgewählt"
        }
    }
    
    fun handleBulkDone() {
        val selected = adapter.getSelectedCustomers()
        if (selected.isNotEmpty()) {
            markBulkAsDone(selected)
        }
    }
    
    private fun markBulkAsDone(customers: List<Customer>) {
        AlertDialog.Builder(activity)
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
                    disableMultiSelectMode()
                    Toast.makeText(activity, "${customers.size} Kunden als erledigt markiert", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
    
    fun isMultiSelectActive(): Boolean {
        return adapter.hasSelectedCustomers() || adapter.isMultiSelectModeEnabled()
    }
}
