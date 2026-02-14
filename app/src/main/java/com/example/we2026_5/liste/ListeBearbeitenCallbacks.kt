package com.example.we2026_5.liste

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.Customer
import com.example.we2026_5.FirebaseRetryHelper
import com.example.we2026_5.KundenListe
import com.example.we2026_5.util.Result
import com.example.we2026_5.ListeIntervall
import java.util.UUID
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.data.repository.CustomerSnapshotParser
import com.example.we2026_5.KundenTermin
import com.example.we2026_5.R
import com.example.we2026_5.util.AppTimeZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                val w = liste.wochentag
                if (w in customer.effectiveAbholungWochentage) {
                    val newList = customer.effectiveAbholungWochentage.filter { it != w }
                    map["defaultAbholungWochentage"] = newList
                    map["defaultAbholungWochentag"] = newList.firstOrNull() ?: -1
                }
                if (w in customer.effectiveAuslieferungWochentage) {
                    val newList = customer.effectiveAuslieferungWochentage.filter { it != w }
                    map["defaultAuslieferungWochentage"] = newList
                    map["defaultAuslieferungWochentag"] = newList.firstOrNull() ?: -1
                }
                map
            } else {
                // Liste ohne Wochentag: listeId leeren und von der Liste übernommene Termine entfernen
                mapOf(
                    "listeId" to "",
                    "termineVonListe" to CustomerSnapshotParser.serializeKundenTermine(emptyList())
                )
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
                val aDays = customer.effectiveAbholungWochentage
                val lDays = customer.effectiveAuslieferungWochentage
                when {
                    w !in aDays && w !in lDays -> mapOf(
                        "defaultAbholungWochentage" to (aDays + w).sorted(),
                        "defaultAbholungWochentag" to w
                    )
                    w !in aDays -> mapOf(
                        "defaultAbholungWochentage" to (aDays + w).sorted(),
                        "defaultAbholungWochentag" to w
                    )
                    else -> mapOf(
                        "defaultAuslieferungWochentage" to (lDays + w).sorted(),
                        "defaultAuslieferungWochentag" to w
                    )
                }
            } else {
                // Liste ohne Wochentag: listeId setzen; Listen-Termine auf Kunden übertragen (termineVonListe)
                val base = mutableMapOf<String, Any>("listeId" to liste.id)
                base["termineVonListe"] = CustomerSnapshotParser.serializeKundenTermine(liste.listenTermine)
                if (liste.intervalle.isNotEmpty()) {
                    base["intervalle"] = liste.intervalle.map {
                        mapOf(
                            "id" to UUID.randomUUID().toString(),
                            "abholungDatum" to it.abholungDatum,
                            "auslieferungDatum" to it.auslieferungDatum,
                            "wiederholen" to it.wiederholen,
                            "intervallTage" to it.intervallTage,
                            "intervallAnzahl" to it.intervallAnzahl,
                            "erstelltAm" to System.currentTimeMillis(),
                            "terminRegelId" to ""
                        )
                    }
                }
                base
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
                
                when (val r = withContext(Dispatchers.IO) { listeRepository.updateListe(liste.id, updatedData) }) {
                    is Result.Success -> {
                        val updatedListe = liste.copy(name = name, listeArt = listeArt, intervalle = intervalle)
                        Toast.makeText(activity, activity.getString(R.string.toast_liste_gespeichert), Toast.LENGTH_SHORT).show()
                        onSuccess(updatedListe)
                    }
                    is Result.Error -> Toast.makeText(activity, r.message, Toast.LENGTH_SHORT).show()
                    is Result.Loading -> { }
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
     * Fügt einen Listen-Termin hinzu (A + L = A + tageAzuL). Gilt für alle Kunden der Liste.
     */
    fun addListenTermin(liste: KundenListe, abholungDatum: Long, tageAzuL: Int, onSuccess: (KundenListe) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val aStart = com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(abholungDatum)
            val tage = tageAzuL.coerceIn(0, 365)
            val lStart = com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(
                aStart + java.util.concurrent.TimeUnit.DAYS.toMillis(tage.toLong())
            )
            val newTermine = liste.listenTermine + KundenTermin(datum = aStart, typ = "A") + KundenTermin(datum = lStart, typ = "L")
            val updates = mapOf("listenTermine" to CustomerSnapshotParser.serializeKundenTermine(newTermine))
            when (val r = withContext(Dispatchers.IO) { listeRepository.updateListe(liste.id, updates) }) {
                is Result.Success -> {
                    Toast.makeText(activity, activity.getString(R.string.toast_liste_gespeichert), Toast.LENGTH_SHORT).show()
                    onSuccess(liste.copy(listenTermine = newTermine))
                    syncTermineVonListeToKunden(liste.id, newTermine)
                }
                is Result.Error -> Toast.makeText(activity, r.message, Toast.LENGTH_SHORT).show()
                is Result.Loading -> { }
            }
        }
    }

    /**
     * Fügt einen einzelnen Listen-Termin hinzu (nur A oder nur L am gewählten Datum).
     * Gilt für alle Kunden der Liste. Bestehende A+L-Logik bleibt unberührt.
     */
    fun addSingleListenTermin(liste: KundenListe, datum: Long, typ: String, onSuccess: (KundenListe) -> Unit) {
        if (typ != "A" && typ != "L") return
        CoroutineScope(Dispatchers.Main).launch {
            val startOfDay = com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(datum)
            val newTermine = liste.listenTermine + KundenTermin(datum = startOfDay, typ = typ)
            val updates = mapOf("listenTermine" to CustomerSnapshotParser.serializeKundenTermine(newTermine))
            when (val r = withContext(Dispatchers.IO) { listeRepository.updateListe(liste.id, updates) }) {
                is Result.Success -> {
                    Toast.makeText(activity, activity.getString(R.string.toast_liste_gespeichert), Toast.LENGTH_SHORT).show()
                    onSuccess(liste.copy(listenTermine = newTermine))
                    syncTermineVonListeToKunden(liste.id, newTermine)
                }
                is Result.Error -> Toast.makeText(activity, r.message, Toast.LENGTH_SHORT).show()
                is Result.Loading -> { }
            }
        }
    }

    /**
     * Überträgt die Listen-Termine auf alle Kunden dieser Liste ohne Wochentag (termineVonListe).
     */
    private fun syncTermineVonListeToKunden(listeId: String, newTermine: List<KundenTermin>) {
        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {
                val customers = customerRepository.getCustomersByListeId(listeId)
                val serialized = CustomerSnapshotParser.serializeKundenTermine(newTermine)
                customers.forEach { customerRepository.updateCustomer(it.id, mapOf("termineVonListe" to serialized)) }
            }
            onDataReload()
        }
    }

    /**
     * Aktualisiert Einstellungen der Liste ohne Wochentag (wochentagA, tageAzuL).
     */
    fun updateListenTourEinstellungen(
        liste: KundenListe,
        wochentagA: Int?,
        tageAzuL: Int,
        onSuccess: (KundenListe) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val wVal = wochentagA?.takeIf { it in 0..6 }
            val tVal = tageAzuL.coerceIn(1, 365)
            val updates = mutableMapOf<String, Any>("tageAzuL" to tVal)
            updates["wochentagA"] = wVal ?: -1
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = { listeRepository.updateListe(liste.id, updates) },
                context = activity,
                errorMessage = activity.getString(R.string.error_save_generic),
                maxRetries = 3
            )
            if (success != null) {
                onSuccess(liste.copy(wochentagA = wVal, tageAzuL = tVal))
            }
        }
    }

    /**
     * Berechnet das nächste A-Datum für den gegebenen Wochentag (0=Mo..6=So) ab heute.
     */
    private fun naechstesADatumFuerWochentag(wochentag: Int, abDatum: Long): Long {
        val cal = AppTimeZone.newCalendar()
        cal.timeInMillis = com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(abDatum)
        val heuteWochentag = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        var tageBis = (wochentag - heuteWochentag + 7) % 7
        if (tageBis == 0) tageBis = 7
        return cal.timeInMillis + java.util.concurrent.TimeUnit.DAYS.toMillis(tageBis.toLong())
    }

    /**
     * Fügt den nächsten Listen-Termin hinzu (basierend auf wochentagA). Findet das nächste A-Datum,
     * das noch nicht in listenTermine existiert.
     */
    fun addListenTerminFromWochentag(liste: KundenListe, onSuccess: (KundenListe) -> Unit) {
        val wochentagA = liste.wochentagA ?: return
        if (wochentagA !in 0..6) return
        val tageAzuL = liste.tageAzuL.coerceIn(1, 365)
        val existingADates = liste.listenTermine.filter { it.typ == "A" }.map { it.datum }.toSet()
        var searchStart = com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
        var aDatum = naechstesADatumFuerWochentag(wochentagA, searchStart)
        while (aDatum in existingADates) {
            searchStart = aDatum + java.util.concurrent.TimeUnit.DAYS.toMillis(1)
            aDatum = naechstesADatumFuerWochentag(wochentagA, searchStart)
        }
        addListenTermin(liste, aDatum, tageAzuL, onSuccess)
    }

    /**
     * Entfernt Listen-Termine (A+L-Paar).
     */
    fun removeListenTermine(liste: KundenListe, toRemove: List<KundenTermin>, onSuccess: (KundenListe) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val toRemoveSet = toRemove.toSet()
            val newTermine = liste.listenTermine.filter { t -> !toRemoveSet.any { it.datum == t.datum && it.typ == t.typ } }
            val updates = mapOf("listenTermine" to CustomerSnapshotParser.serializeKundenTermine(newTermine))
            when (val r = withContext(Dispatchers.IO) { listeRepository.updateListe(liste.id, updates) }) {
                is Result.Success -> {
                    Toast.makeText(activity, activity.getString(R.string.toast_liste_gespeichert), Toast.LENGTH_SHORT).show()
                    onSuccess(liste.copy(listenTermine = newTermine))
                    syncTermineVonListeToKunden(liste.id, newTermine)
                }
                is Result.Error -> Toast.makeText(activity, r.message, Toast.LENGTH_SHORT).show()
                is Result.Loading -> { }
            }
        }
    }

    /**
     * Löscht die Liste
     */
    fun deleteListe(listeId: String, onSuccess: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            when (val r = withContext(Dispatchers.IO) { listeRepository.deleteListe(listeId) }) {
                is Result.Success -> {
                    Toast.makeText(activity, activity.getString(R.string.toast_liste_geloescht), Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
                is Result.Error -> Toast.makeText(activity, r.message, Toast.LENGTH_SHORT).show()
                is Result.Loading -> { }
            }
        }
    }
}
