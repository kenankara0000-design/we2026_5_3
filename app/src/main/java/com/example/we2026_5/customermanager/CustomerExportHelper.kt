package com.example.we2026_5.customermanager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.we2026_5.Customer
import com.example.we2026_5.data.repository.CustomerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

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
                val file = exportToCSV(activity, allCustomers)
                
                if (file != null) {
                    shareFile(file, "text/csv")
                    Toast.makeText(activity, activity.getString(com.example.we2026_5.R.string.toast_csv_exported, file.name), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, activity.getString(com.example.we2026_5.R.string.error_export), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(activity, activity.getString(com.example.we2026_5.R.string.error_message_generic, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun exportAsText() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val allCustomers = repository.getAllCustomers()
                val file = exportTourAsText(activity, allCustomers, Date())
                
                if (file != null) {
                    shareFile(file, "text/plain")
                    Toast.makeText(activity, activity.getString(com.example.we2026_5.R.string.toast_text_exported, file.name), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, activity.getString(com.example.we2026_5.R.string.error_export), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(activity, activity.getString(com.example.we2026_5.R.string.error_message_generic, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Exportiert Kunden als CSV
     */
    private fun exportToCSV(context: Context, customers: List<Customer>): File? {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "kunden_export_${dateFormat.format(Date())}.csv"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            FileWriter(file).use { writer ->
                // Header
                writer.append("Name,Adresse,Telefon,Intervall,Letzter Termin,Status\n")
                
                // Daten
                customers.forEach { customer ->
                    val status = when {
                        customer.abholungErfolgt && customer.auslieferungErfolgt -> "Erledigt"
                        customer.verschobeneTermine.isNotEmpty() -> "Verschoben"
                        com.example.we2026_5.util.TerminFilterUtils.getEffectiveUrlaubEintraege(customer).isNotEmpty() -> "Urlaub"
                        else -> "Offen"
                    }
                    
                    val letzterTermin = if (customer.letzterTermin > 0) {
                        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(customer.letzterTermin))
                    } else {
                        "Nicht gesetzt"
                    }
                    
                    writer.append("\"${customer.name}\",")
                    writer.append("\"${customer.adresse}\",")
                    writer.append("\"${customer.telefon}\",")
                    writer.append("${customer.intervallTage},")
                    writer.append("$letzterTermin,")
                    writer.append("$status\n")
                }
            }
            
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Exportiert Tages-Tour als Text
     */
    private fun exportTourAsText(context: Context, customers: List<Customer>, date: Date): File? {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fileName = "tour_${dateFormat.format(date)}.txt"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            FileWriter(file).use { writer ->
                writer.append("TOUR PLANER - ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date)}\n")
                writer.append("=".repeat(50) + "\n\n")
                
                customers.forEachIndexed { index, customer ->
                    writer.append("${index + 1}. ${customer.name}\n")
                    writer.append("   Adresse: ${customer.adresse}\n")
                    writer.append("   Telefon: ${customer.telefon}\n")
                    writer.append("\n")
                }
            }
            
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
