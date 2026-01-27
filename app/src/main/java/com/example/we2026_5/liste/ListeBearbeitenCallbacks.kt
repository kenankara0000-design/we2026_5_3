package com.example.we2026_5.liste

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.Customer
import com.example.we2026_5.FirebaseRetryHelper
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.TerminRegel
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.util.TerminRegelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper-Klasse für Callback-Handler in ListeBearbeitenActivity
 */
class ListeBearbeitenCallbacks(
    private val activity: AppCompatActivity,
    private val customerRepository: CustomerRepository,
    private val listeRepository: KundenListeRepository,
    private val regelRepository: TerminRegelRepository,
    private val onDataReload: () -> Unit
) {
    
    /**
     * Entfernt einen Kunden aus der Liste
     */
    fun entferneKundeAusListe(customer: Customer) {
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = {
                    customerRepository.updateCustomer(customer.id, mapOf("listeId" to ""))
                },
                context = activity,
                errorMessage = "Fehler beim Entfernen. Bitte erneut versuchen.",
                maxRetries = 3
            )

            if (success != null) {
                Toast.makeText(activity, "Kunde aus Liste entfernt", Toast.LENGTH_SHORT).show()
                onDataReload()
            }
        }
    }
    
    /**
     * Fügt einen Kunden zur Liste hinzu
     */
    fun fuegeKundeZurListeHinzu(customer: Customer, listeId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = {
                    customerRepository.updateCustomer(customer.id, mapOf("listeId" to listeId))
                },
                context = activity,
                errorMessage = "Fehler beim Hinzufügen. Bitte erneut versuchen.",
                maxRetries = 3
            )

            if (success != null) {
                Toast.makeText(activity, "Kunde zur Liste hinzugefügt", Toast.LENGTH_SHORT).show()
                onDataReload()
            }
        }
    }
    
    /**
     * Speichert die Liste
     */
    fun handleSave(
        liste: KundenListe,
        name: String,
        listeArt: String,
        intervalle: List<ListeIntervall>,
        onSuccess: (KundenListe) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Intervalle für Firebase vorbereiten
                val intervalleMap = intervalle.map { 
                    mapOf(
                        "abholungDatum" to it.abholungDatum,
                        "auslieferungDatum" to it.auslieferungDatum,
                        "wiederholen" to it.wiederholen,
                        "intervallTage" to it.intervallTage,
                        "intervallAnzahl" to it.intervallAnzahl
                    )
                }
                
                val updatedData = mapOf(
                    "name" to name,
                    "listeArt" to listeArt,
                    "intervalle" to intervalleMap
                )
                
                val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                    operation = {
                        listeRepository.updateListe(liste.id, updatedData)
                    },
                    context = activity,
                    errorMessage = "Fehler beim Speichern. Bitte erneut versuchen.",
                    maxRetries = 3
                )
                
                if (success != null) {
                    // Lokale Liste aktualisieren
                    val updatedListe = liste.copy(
                        name = name,
                        listeArt = listeArt,
                        intervalle = intervalle
                    )
                    
                    Toast.makeText(activity, "Liste gespeichert", Toast.LENGTH_SHORT).show()
                    onSuccess(updatedListe)
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Zeigt Bestätigungsdialog zum Löschen
     */
    fun showDeleteConfirmation(onConfirm: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle("Liste löschen")
            .setMessage("Bist du sicher, dass du diese Liste endgültig löschen möchtest?")
            .setPositiveButton("Löschen") { _, _ -> onConfirm() }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
    
    /**
     * Löscht die Liste
     */
    fun deleteListe(listeId: String, onSuccess: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = {
                    listeRepository.deleteListe(listeId)
                },
                context = activity,
                errorMessage = "Fehler beim Löschen. Bitte erneut versuchen.",
                maxRetries = 3
            )
            
            if (success != null) {
                Toast.makeText(activity, "Liste gelöscht", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        }
    }
    
    /**
     * Zeigt Dialog zur Regel-Auswahl
     */
    fun showRegelAuswahlDialog(
        onRegelSelected: (TerminRegel) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val regeln = regelRepository.getAllRegeln()
                
                if (regeln.isEmpty()) {
                    Toast.makeText(activity, "Keine Regeln vorhanden. Bitte erstellen Sie zuerst eine Regel.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                val regelNamen = regeln.map { it.name }.toTypedArray()
                
                AlertDialog.Builder(activity)
                    .setTitle("Termin-Regel auswählen")
                    .setItems(regelNamen) { _, which ->
                        val ausgewaehlteRegel = regeln[which]
                        onRegelSelected(ausgewaehlteRegel)
                    }
                    .setNegativeButton("Abbrechen", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(activity, "Fehler beim Laden der Regeln: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Wendet eine Regel auf die Liste an
     */
    fun wendeRegelAn(
        regel: TerminRegel,
        liste: KundenListe,
        intervalle: MutableList<ListeIntervall>,
        intervallAdapter: com.example.we2026_5.ListeIntervallAdapter,
        onSuccess: (KundenListe) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Regel auf Liste anwenden
                val neuesIntervall = TerminRegelManager.wendeRegelAufListeAn(regel, liste)
                
                // Intervall zur Liste hinzufügen
                val neueIntervalle = liste.intervalle.toMutableList()
                neueIntervalle.add(neuesIntervall)
                
                // Liste aktualisieren
                val intervalleMap = neueIntervalle.mapIndexed { index, intervall ->
                    mapOf(
                        "abholungDatum" to intervall.abholungDatum,
                        "auslieferungDatum" to intervall.auslieferungDatum,
                        "wiederholen" to intervall.wiederholen,
                        "intervallTage" to intervall.intervallTage,
                        "intervallAnzahl" to intervall.intervallAnzahl
                    )
                }
                
                val updates = mapOf(
                    "intervalle" to intervalleMap
                )
                
                listeRepository.updateListe(liste.id, updates)
                
                // Verwendungsanzahl erhöhen
                regelRepository.incrementVerwendungsanzahl(regel.id)
                
                // Lokale Liste aktualisieren
                val updatedListe = liste.copy(intervalle = neueIntervalle)
                intervalle.clear()
                intervalle.addAll(neueIntervalle)
                intervallAdapter.updateIntervalle(intervalle.toList())
                
                Toast.makeText(activity, "Regel '${regel.name}' angewendet", Toast.LENGTH_SHORT).show()
                onSuccess(updatedListe)
            } catch (e: Exception) {
                Toast.makeText(activity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
