package com.example.we2026_5.customermanager

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.we2026_5.Customer
import com.example.we2026_5.ExportHelper
import com.example.we2026_5.data.repository.CustomerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

/**
 * Helper für Export-Funktionalität in CustomerManagerActivity.
 * Extrahiert die Export-Dialog- und Export-Logik.
 */
class CustomerExportHelper(
    private val activity: Activity,
    private val repository: CustomerRepository
) {
    
    /**
     * Zeigt einen Dialog zur Auswahl des Export-Formats.
     */
    fun showExportDialog() {
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle("Exportieren")
            .setItems(arrayOf("Als CSV exportieren", "Als Text exportieren")) { _, which ->
                when (which) {
                    0 -> exportAsCSV()
                    1 -> exportAsText()
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
    
    private fun exportAsCSV() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val allCustomers = repository.getAllCustomers()
                val file = ExportHelper.exportToCSV(activity, allCustomers)
                
                if (file != null) {
                    shareFile(file, "text/csv")
                    Toast.makeText(activity, "CSV exportiert: ${file.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "Fehler beim Exportieren", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun exportAsText() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val allCustomers = repository.getAllCustomers()
                val file = ExportHelper.exportTourAsText(activity, allCustomers, Date())
                
                if (file != null) {
                    shareFile(file, "text/plain")
                    Toast.makeText(activity, "Text exportiert: ${file.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "Fehler beim Exportieren", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            activity,
            "com.example.we2026_5.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        activity.startActivity(Intent.createChooser(shareIntent, "Datei teilen"))
    }
}
