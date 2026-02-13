package com.example.we2026_5.util

import android.content.Context
import com.example.we2026_5.data.repository.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREF_MIGRATION = "tour_to_listenkunden_migration"
private const val KEY_DONE = "v1_done"

/**
 * Einmalige Migration (Option B): kundenArt "Tour" → "Listenkunden" in Firebase.
 * Läuft einmal beim App-Start; danach via SharedPreferences gesperrt.
 */
suspend fun runTourToListenkundenMigration(context: Context, repository: CustomerRepository) {
    val prefs = context.getSharedPreferences(PREF_MIGRATION, Context.MODE_PRIVATE)
    if (prefs.getBoolean(KEY_DONE, false)) return

    withContext(Dispatchers.IO) {
        try {
            val customers = repository.getAllCustomers()
            val toMigrate = customers.filter { it.kundenArt == "Tour" }
            toMigrate.forEach { customer ->
                repository.updateCustomer(customer.id, mapOf("kundenArt" to "Listenkunden"))
            }
            prefs.edit().putBoolean(KEY_DONE, true).apply()
        } catch (_: Exception) {
            // Bei Fehler: nächstes Mal erneut versuchen
        }
    }
}
