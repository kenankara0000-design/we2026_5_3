package com.example.we2026_5.util

import android.content.Context
import com.example.we2026_5.data.repository.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREF_MIGRATION = "liste_to_tour_migration"
private const val KEY_DONE = "v1_done"

/**
 * Einmalige Migration: kundenArt "Liste" → "Tour" in Firebase.
 * Läuft einmal beim App-Start; danach via SharedPreferences gesprerrt.
 */
suspend fun runListeToTourMigration(context: Context, repository: CustomerRepository) {
    val prefs = context.getSharedPreferences(PREF_MIGRATION, Context.MODE_PRIVATE)
    if (prefs.getBoolean(KEY_DONE, false)) return

    withContext(Dispatchers.IO) {
        try {
            val customers = repository.getAllCustomers()
            val toMigrate = customers.filter { it.kundenArt == "Liste" }
            toMigrate.forEach { customer ->
                repository.updateCustomer(customer.id, mapOf("kundenArt" to "Tour"))
            }
            prefs.edit().putBoolean(KEY_DONE, true).apply()
        } catch (_: Exception) {
            // Bei Fehler: nächstes Mal erneut versuchen
        }
    }
}
