package com.example.we2026_5

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object ExportHelper {
    
    /**
     * Exportiert Kunden als CSV
     */
    fun exportToCSV(context: Context, customers: List<Customer>): File? {
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
                        customer.verschobenAufDatum > 0 -> "Verschoben"
                        customer.urlaubVon > 0 && customer.urlaubBis > 0 -> "Urlaub"
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
    fun exportTourAsText(context: Context, customers: List<Customer>, date: Date): File? {
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
}
