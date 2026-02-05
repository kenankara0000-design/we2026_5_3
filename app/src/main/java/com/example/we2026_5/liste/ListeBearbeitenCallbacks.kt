package com.example.we2026_5.liste

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.Customer
import com.example.we2026_5.FirebaseRetryHelper
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.R
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
    private val onDataReload: () -> Unit
) {
    
    /**
     * Entfernt einen Kunden aus der Liste.
     * Wochentagslisten: setzt A-/L-Tag auf -1. Manuelle Listen: setzt listeId auf "".
     */
    fun entferneKundeAusListe(customer: Customer, liste: KundenListe) {
        CoroutineScope(Dispatchers.Main).launch {
            val updates = if (liste.wochentag in 0..6) {
                val map = mutableMapOf<String, Any>()
                if (customer.defaultAbholungWochentag == liste.wochentag) map["defaultAbholungWochentag"] = -1
                if (customer.defaultAuslieferungWochentag == liste.wochentag) map["defaultAuslieferungWochentag"] = -1
                map
            } else {
                mapOf("listeId" to "")
            }
            if (updates.isEmpty()) {
                onDataReload()
                return@launch
            }
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = { customerRepository.updateCustomer(customer.id, updates) },
                context = activity,
                errorMessage = activity.getString(R.string.error_delete_generic),
                maxRetries = 3
            )
            if (success != null) {
                Toast.makeText(activity, activity.getString(R.string.toast_kunde_aus_liste_entfernt), Toast.LENGTH_SHORT).show()
                onDataReload()
            }
        }
    }

    /**
     * Fügt einen Kunden zur Liste hinzu.
     * Wochentagslisten: setzt A- oder L-Tag auf den Listen-Wochentag. Manuelle Listen: setzt listeId.
     */
    fun fuegeKundeZurListeHinzu(customer: Customer, liste: KundenListe) {
        CoroutineScope(Dispatchers.Main).launch {
            val updates = if (liste.wochentag in 0..6) {
                val w = liste.wochentag
                when {
                    customer.defaultAbholungWochentag != w && customer.defaultAuslieferungWochentag != w ->
                        mapOf("defaultAbholungWochentag" to w)
                    customer.defaultAbholungWochentag != w -> mapOf("defaultAbholungWochentag" to w)
                    else -> mapOf("defaultAuslieferungWochentag" to w)
                }
            } else {
                mapOf("listeId" to liste.id)
            }
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = { customerRepository.updateCustomer(customer.id, updates) },
                context = activity,
                errorMessage = activity.getString(R.string.error_save_generic),
                maxRetries = 3
            )
            if (success != null) {
                Toast.makeText(activity, activity.getString(R.string.toast_kunde_zur_liste_hinzugefuegt), Toast.LENGTH_SHORT).show()
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
                    errorMessage = activity.getString(R.string.error_save_generic),
                    maxRetries = 3
                )
                
                if (success != null) {
                    // Lokale Liste aktualisieren
                    val updatedListe = liste.copy(
                        name = name,
                        listeArt = listeArt,
                        intervalle = intervalle
                    )
                    
                    Toast.makeText(activity, activity.getString(R.string.toast_liste_gespeichert), Toast.LENGTH_SHORT).show()
                    onSuccess(updatedListe)
                }
            } catch (e: Exception) {
                Toast.makeText(activity, activity.getString(R.string.error_message_generic, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Zeigt Bestätigungsdialog zum Löschen
     */
    fun showDeleteConfirmation(onConfirm: () -> Unit) {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(com.example.we2026_5.R.string.dialog_delete_list_title))
            .setMessage(activity.getString(com.example.we2026_5.R.string.dialog_list_delete_confirm_message))
            .setPositiveButton(activity.getString(com.example.we2026_5.R.string.dialog_loeschen)) { _, _ -> onConfirm() }
            .setNegativeButton(activity.getString(com.example.we2026_5.R.string.btn_cancel), null)
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
                errorMessage = activity.getString(R.string.error_delete_generic),
                maxRetries = 3
            )
            
            if (success != null) {
                Toast.makeText(activity, activity.getString(R.string.toast_liste_geloescht), Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        }
    }
    
}
