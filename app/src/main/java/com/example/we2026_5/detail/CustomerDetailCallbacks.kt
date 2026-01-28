package com.example.we2026_5.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.CustomerManagerActivity
import com.example.we2026_5.TerminRegel
import com.example.we2026_5.TerminRegelErstellenActivity
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.databinding.ActivityCustomerDetailBinding
import com.example.we2026_5.util.TerminRegelManager
import com.example.we2026_5.FirebaseRetryHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper-Klasse für Callback-Handler in CustomerDetailActivity.
 * Extrahiert alle Callback-Funktionen aus CustomerDetailActivity.
 */
class CustomerDetailCallbacks(
    private val activity: AppCompatActivity,
    private val binding: ActivityCustomerDetailBinding,
    private val repository: CustomerRepository,
    private val regelRepository: TerminRegelRepository,
    private val customerId: String,
    private val intervalle: MutableList<CustomerIntervall>,
    private val intervallAdapter: com.example.we2026_5.IntervallAdapter,
    private var currentCustomer: Customer?
) {
    
    /**
     * Startet Navigation zu Kundenadresse.
     */
    fun startNavigation() {
        currentCustomer?.let {
            if (it.adresse.isNotBlank()) {
                val gmmIntentUri = Uri.parse("google.navigation:q=${it.adresse}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(activity.packageManager) != null) {
                    activity.startActivity(mapIntent)
                } else {
                    Toast.makeText(activity, "Google Maps ist nicht installiert.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(activity, "Keine Adresse vorhanden.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Öffnet Google Maps für Adressauswahl.
     */
    fun openMapsForLocationSelection() {
        val currentAddress = binding.etDetailAdresse.text.toString().trim()
        val query = if (currentAddress.isNotBlank()) currentAddress else "Deutschland"
        
        // Google Maps öffnen mit Suchfunktion
        val gmmIntentUri = Uri.parse("geo:0,0?q=$query")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        
        if (mapIntent.resolveActivity(activity.packageManager) != null) {
            // Starte Maps - Benutzer kann dann Adresse auswählen und kopieren
            activity.startActivity(mapIntent)
            Toast.makeText(activity, "Wählen Sie einen Ort in Google Maps aus und kopieren Sie die Adresse hierher ein.", Toast.LENGTH_LONG).show()
        } else {
            // Fallback: Browser öffnen
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$query"))
            if (webIntent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(webIntent)
            } else {
                Toast.makeText(activity, "Google Maps ist nicht verfügbar.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Startet Telefonanruf.
     */
    fun startPhoneCall() {
        currentCustomer?.let {
            if (it.telefon.isNotBlank()) {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                dialIntent.data = Uri.parse("tel:${it.telefon}")
                activity.startActivity(dialIntent)
            } else {
                Toast.makeText(activity, "Keine Telefonnummer vorhanden.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Zeigt Bestätigungsdialog zum Löschen des Kunden.
     */
    fun showDeleteConfirmation() {
        AlertDialog.Builder(activity)
            .setTitle("Kunde löschen")
            .setMessage("Bist du sicher, dass du diesen Kunden endgültig löschen möchtest?")
            .setPositiveButton("Löschen") { _, _ -> handleDelete() }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    /**
     * Behandelt das Löschen des Kunden.
     */
    private fun handleDelete() {
        AlertDialog.Builder(activity)
            .setTitle("Kunde löschen?")
            .setMessage("Möchten Sie diesen Kunden wirklich löschen? Alle Termine dieses Kunden werden ebenfalls gelöscht.")
            .setPositiveButton("Löschen") { _, _ ->
                // Optimistische UI-Aktualisierung: Sofort benachrichtigen, dass Kunde gelöscht wurde
                // Damit die Liste in CustomerManagerActivity sofort aktualisiert wird
                val resultIntent = Intent().apply {
                    putExtra("DELETED_CUSTOMER_ID", customerId)
                }
                activity.setResult(CustomerManagerActivity.RESULT_CUSTOMER_DELETED, resultIntent)
                
                CoroutineScope(Dispatchers.Main).launch {
                    val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                        operation = { 
                            // Kunde löschen - alle Termine werden automatisch gelöscht, da sie Teil des Kunden-Objekts sind
                            repository.deleteCustomer(customerId)
                        },
                        context = activity,
                        errorMessage = "Fehler beim Löschen. Bitte erneut versuchen.",
                        maxRetries = 3
                    )
                    
                    if (success == true) {
                        Toast.makeText(activity, "Kunde und alle Termine gelöscht", Toast.LENGTH_LONG).show()
                        activity.finish()
                    } else {
                        // Bei Fehler: Result zurücksetzen
                        activity.setResult(AppCompatActivity.RESULT_CANCELED)
                    }
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
    
    /**
     * Öffnet das Termin-Regeln-Fenster zur Auswahl einer Regel.
     * Immer das richtige Fenster, kein Dialog.
     * Übergibt die Customer-ID, damit beim Auswählen einer Regel geprüft werden kann,
     * ob bereits Intervalle vorhanden sind.
     */
    fun showRegelAuswahlDialog() {
        // Immer direkt das Termin-Regeln-Fenster öffnen
        // Customer-ID mitgeben, damit beim Auswählen geprüft werden kann, ob bereits Intervalle vorhanden sind
        val intent = Intent(activity, com.example.we2026_5.TerminRegelManagerActivity::class.java).apply {
            putExtra("CUSTOMER_ID", customerId)
        }
        activity.startActivity(intent)
    }
    
    /**
     * Wendet eine Termin-Regel auf den Kunden an.
     */
    private fun wendeRegelAn(regel: TerminRegel) {
        val customer = currentCustomer ?: return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Regel auf Kunden anwenden
                val neuesIntervall = TerminRegelManager.wendeRegelAufKundeAn(regel, customer)
                
                // Intervall zur Liste hinzufügen
                val neueIntervalle = customer.intervalle.toMutableList()
                neueIntervalle.add(neuesIntervall)
                
                // Kunden aktualisieren
                val updates = mapOf(
                    "intervalle" to neueIntervalle
                )
                
                val success = repository.updateCustomer(customer.id, updates)
                if (success) {
                    // Verwendungsanzahl erhöhen
                    regelRepository.incrementVerwendungsanzahl(regel.id)
                    
                    Toast.makeText(activity, "Regel '${regel.name}' angewendet", Toast.LENGTH_SHORT).show()
                    
                    // UI aktualisieren
                    intervalle.clear()
                    intervalle.addAll(neueIntervalle)
                    intervallAdapter.updateIntervalle(intervalle.toList())
                } else {
                    Toast.makeText(activity, "Fehler beim Anwenden der Regel", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Aktualisiert den aktuellen Kunden.
     */
    fun updateCurrentCustomer(customer: Customer?) {
        currentCustomer = customer
    }
    
    /**
     * Zeigt den Info-Dialog für eine Termin-Regel.
     */
    fun showRegelInfoDialog(regelId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val regel = regelRepository.getRegelById(regelId)
                if (regel != null) {
                    showRegelInfoDialogInternal(regel)
                } else {
                    Toast.makeText(activity, "Regel nicht gefunden", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "Fehler beim Laden der Regel: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showRegelInfoDialogInternal(regel: TerminRegel) {
        val wochentage = arrayOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
        
        val infoText = buildString {
            append("Name: ${regel.name}\n\n")
            
            if (regel.beschreibung.isNotEmpty()) {
                append("Beschreibung: ${regel.beschreibung}\n\n")
            }
            
            if (regel.wochentagBasiert) {
                append("Typ: Wochentag-basiert\n\n")
                
                if (regel.startDatum > 0) {
                    val startDateText = com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(regel.startDatum)
                    append("Startdatum: $startDateText\n")
                }
                
                if (regel.abholungWochentag >= 0) {
                    append("Abholung: ${wochentage[regel.abholungWochentag]}\n")
                }
                
                if (regel.auslieferungWochentag >= 0) {
                    append("Auslieferung: ${wochentage[regel.auslieferungWochentag]}\n")
                }
                
                // Start-Woche Option wurde entfernt - Berechnung erfolgt automatisch basierend auf Startdatum
                append("\n")
            } else {
                append("Typ: Datum-basiert\n\n")
                
                val abholungText = if (regel.abholungDatum > 0) {
                    com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(regel.abholungDatum)
                } else {
                    "Heute"
                }
                append("Abholung: $abholungText\n")
                
                val auslieferungText = if (regel.auslieferungDatum > 0) {
                    com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(regel.auslieferungDatum)
                } else {
                    "Heute"
                }
                append("Auslieferung: $auslieferungText\n\n")
            }
            
            if (regel.wiederholen) {
                append("Wiederholen: Ja\n")
                append("Intervall: Alle ${regel.intervallTage} Tage\n")
                if (regel.intervallAnzahl > 0) {
                    append("Anzahl: ${regel.intervallAnzahl} Wiederholungen\n")
                } else {
                    append("Anzahl: Unbegrenzt\n")
                }
            } else {
                append("Wiederholen: Nein\n")
            }
            
            append("\nVerwendungsanzahl: ${regel.verwendungsanzahl}x")
        }
        
        AlertDialog.Builder(activity)
            .setTitle("Regel-Informationen")
            .setMessage(infoText)
            .setPositiveButton("Schließen", null)
            .show()
    }
}
