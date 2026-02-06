package com.example.we2026_5.util

import android.content.Context
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

private const val PREF_MIGRATION = "liste_intervalle_migration"
private const val KEY_DONE = "v1_done"

/**
 * Einmalige Migration: Kunden mit listeId aber ohne customer.intervalle
 * bekommen die Intervalle der zugehörigen Liste als eigene intervalle.
 * Zusätzlich: Kunden mit alter Struktur (abholungDatum/auslieferungDatum) aber ohne intervalle
 * bekommen ein Legacy-Intervall in intervalle. Danach kann die Alte-Struktur-Logik entfernt werden.
 */
suspend fun runListeIntervalleMigration(
    context: Context,
    customerRepository: CustomerRepository,
    listeRepository: KundenListeRepository
) {
    val prefs = context.getSharedPreferences(PREF_MIGRATION, Context.MODE_PRIVATE)
    if (prefs.getBoolean(KEY_DONE, false)) return

    withContext(Dispatchers.IO) {
        try {
            val customers = customerRepository.getAllCustomers()
            val listen = listeRepository.getAllListen()
            val listeById = listen.associateBy { it.id }

            customers.forEach { customer ->
                val updates = mutableMapOf<String, Any>()

                // 1) Kunde in Liste, aber keine eigenen Intervalle → von Liste übernehmen
                if (customer.listeId.isNotBlank() && customer.intervalle.isEmpty()) {
                    val liste = listeById[customer.listeId]
                    if (liste != null && liste.intervalle.isNotEmpty()) {
                        updates["intervalle"] = liste.intervalle.map {
                            mapOf<String, Any>(
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
                }

                // 2) Keine intervalle, aber alte Felder gesetzt → ein Legacy-Intervall anlegen
                if (updates.isEmpty() && customer.intervalle.isEmpty() &&
                    (customer.abholungDatum > 0 || customer.auslieferungDatum > 0)
                ) {
                    updates["intervalle"] = listOf(
                        mapOf<String, Any>(
                            "id" to "legacy-${UUID.randomUUID()}",
                            "abholungDatum" to customer.abholungDatum,
                            "auslieferungDatum" to customer.auslieferungDatum,
                            "wiederholen" to customer.wiederholen,
                            "intervallTage" to (customer.intervallTage.coerceIn(1, 365).takeIf { customer.wiederholen } ?: 7),
                            "intervallAnzahl" to 0,
                            "erstelltAm" to System.currentTimeMillis(),
                            "terminRegelId" to ""
                        )
                    )
                }

                if (updates.isNotEmpty()) {
                    customerRepository.updateCustomer(customer.id, updates)
                }
            }

            prefs.edit().putBoolean(KEY_DONE, true).apply()
        } catch (_: Exception) {
            // Bei Fehler: nächstes Mal erneut versuchen
        }
    }
}
